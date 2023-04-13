package io.crumbl.core

import com.cyrildever.feistel.Feistel
import io.crumbl.encrypter.{Crumb, Dispatcher, Encrypter}
import io.crumbl.hasher.Hasher
import io.crumbl.models.core.Signer
import io.crumbl.obfuscator.Obfuscator
import io.crumbl.padder.Padder
import io.crumbl.slicer.Slicer
import io.crumbl.utils.Logging
import java.io._
import scala.collection.mutable.ArrayBuffer

/**
 * Crumbl class
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 3.1
 *
 * @param source      The data to use
 * @param hashEngine  The name of the hash engine
 * @param owners      The list of signers that "own" the data
 * @param trustees    The list of trusted signing third-parties
 */
final case class Crumbl(
  source: String,
  hashEngine: String,
  owners: Seq[Signer],
  trustees: Seq[Signer]
) extends Logging {
  private[Crumbl] val feistel = Feistel.FPECipher(Obfuscator.DEFAULT_HASH, Obfuscator.DEFAULT_KEY_STRING, Obfuscator.DEFAULT_ROUNDS)

  /**
   * @return the crumbled string for the passed source
   */
  def process: String = doCrumbl()

  /**
   * Save the crumbl to file, eventually appending it to an already filled file
   */
  def toFile(filename: String): String = {
    val file = new File(filename)
    val bw = new BufferedWriter(new FileWriter(file, true))
    try {
      val crumbled = process

      // Add newline
      bw.write(crumbled)
      bw.newLine()

      crumbled
    } catch {
      case e: Exception =>
        logger.throwing(getClass.getName, Thread.currentThread.getStackTrace()(2).getMethodName, e)
        throw e
    } finally {
      bw.close()
    }
  }

  /**
   * Writes the crumbl to stdout
   */
  def toStdOut: String = {
    val crumbled = process
    System.out.println(crumbled)
    crumbled
  }

  /**
   * doCrumbl build the actual crumbled string which would be composed of:<br />
   * - the hash of the source (in hexadecimal);<br />
   * - the concatenation of the stringified encrypted crumbs;<br />
   * - a dot followed by the version number of the Crumb&trade; engine used.
   */
  private def doCrumbl(): String = {
    // 1- Obfuscate
    val obfuscator = Obfuscator(feistel)
    val obfuscated = obfuscator.applyTo(source)

    // 2- Pad
    val (padded, _) = Padder.applyTo(obfuscated, obfuscated.length, buildEven = true)

    // 3- Slice
    val numberOfSlices = 1 + Math.min(trustees.length, Slicer.MAX_SLICES) // Owners only sign the first slice
    val deltaMax = Slicer.getDeltaMax(padded.length, numberOfSlices)
    val slicer = Slicer(numberOfSlices, deltaMax)
    val slices = slicer.applyTo(padded.map(_.toChar).mkString)

    // 4- Encrypt
    val crumbs = new ArrayBuffer[Crumb]()
    for (owner <- owners) {
      val crumb = Encrypter.encrypt(slices.head, 0, owner)
      crumbs += crumb
    }
    val dispatcher = Dispatcher(numberOfSlices, trustees)
    val allocation = dispatcher.allocate()
    for ((i, ttees) <- allocation) {
      for (trustee <- ttees) {
        val crumb = Encrypter.encrypt(slices(i), i, trustee)
        crumbs += crumb
      }
    }

    // 5- Hash the source string
    val hashered = Hasher(crumbs.toSeq).applyTo(source)

    // 6- Finalize the output string
    val stringifiedCrumbs = new StringBuilder
    for (crumb <- crumbs.toSeq) {
      stringifiedCrumbs ++= crumb.toString
    }
    hashered + stringifiedCrumbs.toString + "." + Crumbl.VERSION
  }
}
object Crumbl {
  val VERSION = "1" // TODO Change when necessary (change of hash algorithm, modification of string structure, etc.)
}
