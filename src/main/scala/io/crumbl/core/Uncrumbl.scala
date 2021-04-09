package io.crumbl.core

import fr.edgewhere.feistel.Feistel
import io.crumbl.crypto
import io.crumbl.decrypter.{Collector, Decrypter, Uncrumb}
import io.crumbl.encrypter.Crumb
import io.crumbl.hasher.Hasher
import io.crumbl.models.core.Signer
import io.crumbl.obfuscator.Obfuscator
import io.crumbl.utils.{Converter, Logging}
import java.io.{BufferedWriter, File, FileWriter}
import scala.collection.mutable.ArrayBuffer
import util.control.Breaks._

/**
 * Uncrumbl class
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 3.0
 *
 * @param crumbled          The crumbled string to use
 * @param slices            An optional list of partial uncrumbs
 * @param verificationHash  The optional verification hash to use
 * @param signer            The intended decrypter
 * @param isOwner           Pass `true` if the signer is a data owner (Default: `false`)
 */
final case class Uncrumbl(
  crumbled: String,
  slices: Option[Seq[Uncrumb]],
  verificationHash: Option[String],
  signer: Signer,
  isOwner: Boolean = false
) extends Logging {
  import Uncrumbl._

  private[Uncrumbl] val feistel = Feistel.FPECipher(Obfuscator.DEFAULT_HASH, Obfuscator.DEFAULT_KEY_STRING, Obfuscator.DEFAULT_ROUNDS)

  /**
   * @return the uncrumbled data from the passed crumbl and data
   */
  def process: Seq[Byte] = doUncrumbl()

  /**
   * Save the uncrumbl to file, eventually appending it to an already filled file
   */
  def toFile(filename: String): Seq[Byte] = {
    val file = new File(filename)
    val bw = new BufferedWriter(new FileWriter(file, true))
    try {
      val uncrumbled = process

      // Add newline
      bw.write(Converter.bytesToString(uncrumbled))
      bw.newLine()

      uncrumbled
    } catch {
      case e: Exception =>
        logger.throwing(getClass.getName, Thread.currentThread.getStackTrace()(2).getMethodName, e)
        throw e
    } finally {
      bw.close()
    }
  }

  /**
   * Writes the uncrumbl to stdout
   */
  def toStdOut: Seq[Byte] = {
    val uncrumbled = process
    System.out.println(Converter.bytesToString(uncrumbled))
    uncrumbled
  }

  /**
   * doUncrumbl is a multi-step process involving at least an owner or any necessary trustees to recover all crumbs depending on the number of signing parties.
   * Eventually, the owner will be the only one able to actually get the original source data in clear.<br />
   * <br />
   * On an operational stand-point, the process would be as follows:<br />
   * 1) The owner sends the crumbled string to all trustees;<br />
   * 2) Each trustee decrypts his crumbs and send them back to the owner as partial uncrumbs;<br />
   * 3) The owner decrypts his own crumb and add it to the other partial uncrumbs to get the original source fully deciphered.<br />
   * <br />
   * The uncrumbled result would either be:<br />
   * - the fully-deciphered data, ie. the original source normally;<br />
   * - partial uncrumbs to use as arguments in another call to the Uncrumbl (by the data owner).<br />
   * The latter will be in the following format: &lt;verificationHash&gt;&lt;uncrumbs ...&gt;.&lt;version&gt;, each uncrumb starting with the partial prefix,
   * the verification hash being prefixed for tracking purpose, and the version at the end after a dot.
   */
  private def doUncrumbl(): Seq[Byte] = {
    // 1- Parse
    val (vh, crumbs) = extractData(crumbled)
    if (verificationHash.getOrElse("") != vh) {
      logger.warning("incompatible input verification hash with crumbl")
    }

    // 2- Decrypt crumbs
    val uncrumbs = scala.collection.mutable.Map[Int, Uncrumb]()
    val indexSet = scala.collection.mutable.Map[Int, Boolean]()
    for (crumb <- crumbs) {
      breakable {
        val idx = crumb.index
        if (!indexSet.contains(idx) || !indexSet.getOrElse(idx, false)) {
          indexSet += (idx -> true)
        }
        if ((!isOwner && idx == 0) || (isOwner && idx != 0)) {
          break
        }
        try {
          val uncrumb = Decrypter.decrypt(crumb, signer)
          if (!uncrumbs.contains(uncrumb.index)) {
            uncrumbs += (idx -> uncrumb)
          }
        } catch {
          case _: Throwable => // NO-OP
        }
      }
    }

    // 3- Add passed uncrumbs
    slices match {
      case Some(sls) =>
        for (uncrumb <- sls) if (!uncrumbs.contains(uncrumb.index)) {
          uncrumbs += (uncrumb.index -> uncrumb)
        }
      case _ =>
    }

    // 4- Determine output
    val hasAllUncrumbs = indexSet.size == uncrumbs.size
    if (isOwner && !hasAllUncrumbs) {
      logger.warning("missing crumbs to fully uncrumbl as data owner: only partial uncrumbs to be returned")
    }
    if (hasAllUncrumbs) {
      // Owner may recover fully-deciphered data
      val collector = Collector(uncrumbs.toMap, indexSet.size, vh, crypto.DEFAULT_HASH_ENGINE)

      // 5a- Deobfuscate
      val obfuscated = collector.toObfuscated
      val obfuscator = Obfuscator(feistel)
      val deobfuscated = obfuscator.unapplyTo(obfuscated)

      // 6a- Check
      if (!collector.check(deobfuscated.getBytes)) {
        logger.warning("source has not checked verification hash") // TODO Change it as an Exception?
      }

      // 7a- Return uncrumbled data, ie. original source normally
      deobfuscated.getBytes
    } else {
      // Trustee only could return his own uncrumbs

      // 5b- Build partial uncrumbs
      val partialUncrumbs = uncrumbs.map { case (_, uncrumb) => uncrumb.toString }.mkString

      // 6b - Add verification hash prefix
      (vh + partialUncrumbs + "." + Crumbl.VERSION).getBytes
    }
  }
}
object Uncrumbl {
  /**
   * Extract verification and crumbs from the passed crumbled string
   */
  def extractData(crumbled: String): (String, Seq[Crumb]) = {
    val parts = crumbled.split("\\.")
    if (parts(1) != Crumbl.VERSION) throw new Exception(s"incompatible version: ${parts(1)}")

    val crumbs = new ArrayBuffer[Crumb]()
    var crumbsStr = parts(0).substring(crypto.DEFAULT_HASH_LENGTH)
    while (crumbsStr.nonEmpty) {
      val nextLen = Converter.hexToInt(crumbsStr.substring(2, 6))
      val nextCrumb = crumbsStr.substring(0, nextLen + 6)
      val crumb = Crumb.toCrumb(nextCrumb)
      crumbs += crumb
      crumbsStr = crumbsStr.substring(nextLen + 6)
    }

    val hashered = parts(0).substring(0, crypto.DEFAULT_HASH_LENGTH)
    val verificationHash = Hasher(crumbs.toSeq).unapplyTo(hashered)

    (verificationHash, crumbs.toSeq)
  }
}
