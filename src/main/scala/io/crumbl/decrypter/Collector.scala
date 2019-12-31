package io.crumbl.decrypter

import io.crumbl.crypto
import io.crumbl.utils.{Converter, Padder}

/**
 * Collector class
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 1.0
 *
 * @param mapping           A map of slice index -> uncrumb
 * @param numberOfSlices    The total number of slices
 * @param verificationHash  The computed verification hash
 * @param hashEngine        The used hash engine
 */
final case class Collector(
  mapping: Map[Int, Uncrumb],
  numberOfSlices: Int,
  verificationHash: String,
  hashEngine: String
) {
  /**
   * Verifies the passed data against the verification hash
   */
  def check(data: Seq[Byte]): Boolean = {
    val hashedData = crypto.hash(data, hashEngine)
    Converter.bytesToHex(hashedData) == verificationHash
  }

  /**
   * @return the obfuscated data
   */
  def toObfuscated: Seq[Byte] = {
    val o = new StringBuilder
    for (i <- 0 until numberOfSlices) {
      val uncrumb = if (mapping.keySet.contains(i)) mapping(i) else throw new Exception(s"missing slice with index: ${i}")
      o ++= Padder.unpad(uncrumb.toSlice)
    }
    o.toString.getBytes
  }
}
