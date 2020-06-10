package io.crumbl.hasher

import io.crumbl.crypto
import io.crumbl.encrypter.Crumb
import io.crumbl.utils.{Converter, Logging, XOR}
import scala.math.Integral.Implicits._

/**
 * Hasher class
 *
 * @author  Cyril Dever
 * @since   2.0
 * @version 1.0
 */
final case class Hasher(crumbs: Seq[Crumb]) extends Logging {
  import Hasher._

  /**
   * Generates the hashered prefix for the Crumbl
   */
  def applyTo(source: String): String = {
    val hSrc = crypto.hash(source.getBytes, crypto.DEFAULT_HASH_ENGINE)
    val stringifiedHash = Converter.bytesToHex(hSrc)
    val length = stringifiedHash.length
    if (length < NUMBER_OF_CHARACTERS) {
      throw new Exception("wrong hash algorithm")
    }
    val lastBytes = Converter.hexToBytes(stringifiedHash.substring(length - NUMBER_OF_CHARACTERS))
    val sortedOwnerCrumbs = crumbs.filter(_.index == 0).sortBy(_.encrypted.getString)
    if (sortedOwnerCrumbs.isEmpty) {
      throw new Exception("owner's crumbs not present")
    }
    val concat = sortedOwnerCrumbs.flatMap(_.encrypted.getBytes)
    val mask = buildMask(concat, NUMBER_OF_CHARACTERS / 2)
    val xored = XOR.bytes(lastBytes, mask)
    stringifiedHash.substring(0, NUMBER_OF_CHARACTERS) + Converter.bytesToHex(xored)
  }

  /**
   * Recovers the original hash of the source from the passed hashered prefix
   */
  def unapplyTo(hashered: String): String = {
    if (hashered.length < NUMBER_OF_CHARACTERS) {
      throw new Exception("wrong hashered value")
    }
    val xored = Converter.hexToBytes(hashered.substring(hashered.length - NUMBER_OF_CHARACTERS))
    val sortedOwnerCrumbs = crumbs.filter(_.index == 0).sortBy(_.encrypted.getString)
    if (sortedOwnerCrumbs.isEmpty) {
      throw new Exception("owner's crumbs not present")
    }
    val concat = sortedOwnerCrumbs.flatMap(_.encrypted.getBytes)
    val mask = buildMask(concat, NUMBER_OF_CHARACTERS / 2)
    val lastChars = Converter.bytesToHex(XOR.bytes(xored, mask))
    hashered.substring(0, hashered.length - NUMBER_OF_CHARACTERS) + lastChars
  }

  private def buildMask(key: Seq[Byte], length: Int): Seq[Byte] = if (key.isEmpty) {
      Array.ofDim[Byte](length)
    } else if (key.length >= length) {
      key.take(length)
    } else {
    val (quotient, remainder) = length /% key.length
    Array.fill(quotient)(key).flatten ++ key.take(remainder).toArray
  }
}
object Hasher {
  val NUMBER_OF_CHARACTERS = 32
}
