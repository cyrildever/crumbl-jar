package io.crumbl.utils

import javax.xml.bind.DatatypeConverter

/**
 * Converter utility
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 1.0
 */
object Converter {
  /**
   * Convert a byte array to its hexadecimal string representation
   */
  def bytesToHex(bytes: Seq[Byte]): String = DatatypeConverter.printHexBinary(bytes.toArray).toLowerCase

  /**
   * Convert a byte array to a UTF-8 string
   */
  def bytesToString(bytes: Seq[Byte]): String = bytes.map(_.toChar).mkString

  /**
   * Convert an hexadecimal string representation to its underlying byte array
   */
  def hexToBytes(hex: String): Seq[Byte] = DatatypeConverter.parseHexBinary(hex)

  /**
   * Convert an hexadecimal string representation to its underlying integer
   */
  def hexToInt(hex: String): Int = Integer.parseInt(hex, 16)

  /**
   * Convert an integer to its hexadecimal representation
   */
  def intToHex(i: Int): String = i.toHexString
}
