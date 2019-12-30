package io.crumbl.core

import io.crumbl.crypto
import io.crumbl.encrypter.{Crumb, Dispatcher, Encrypter}
import io.crumbl.models.core.Signer
import io.crumbl.obfuscator.Obfuscator
import io.crumbl.slicer.Slicer
import io.crumbl.utils.{Converter, Logging, Padder}
import java.io._

/**
 * Crumbl class
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 1.0
 */
case class Crumbl(source: String, hashEngine: String, owners: Seq[Signer], trustees: Seq[Signer]) extends Logging {
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
  def toStdOut(): String = {
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
    val obfuscator = Obfuscator(Obfuscator.DEFAULT_KEY_STRING, Obfuscator.DEFAULT_ROUNDS)
    val obfuscated = obfuscator.applyTo(source)

    // 2- Slice
    val numberOfSlices = 1 + Math.min(trustees.length, Slicer.MAX_SLICES) // Owners only sign the first slice
    val deltaMax = Slicer.getDeltaMax(obfuscated.length, numberOfSlices)
    val slicer = Slicer(numberOfSlices, deltaMax)
    val slices = slicer.applyTo(Padder.leftPad(Converter.bytesToString(obfuscated), Slicer.MIN_INPUT_SIZE))

    // 3- Encrypt
    val crumbs = new scala.collection.mutable.ArrayBuffer[Crumb]()
    for (owner <- owners) {
      val crumb = Encrypter.encrypt(slices(0), 0, owner)
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

    // 4- Hash the source string
    val hSrc = crypto.hash(source.getBytes, crypto.DEFAULT_HASH_ENGINE)

    // 5- Finalize the output string
    val stringifiedCrumbs = new StringBuilder
    for (crumb <- crumbs.toSeq) {
      stringifiedCrumbs ++= crumb.toString
    }
    Converter.bytesToHex(hSrc) + stringifiedCrumbs.toString + "." + Crumbl.VERSION
  }
}
object Crumbl {
  val VERSION = "1" // TODO Change when necessary (change of hash algorithm, modification of string structure, etc.)
}
