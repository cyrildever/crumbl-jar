package io.crumbl.utils

/**
 * Converter utility
 *
 * @author  Cyril Dever
 * @since   2.0
 * @version 1.1
 */
object Converter {
  /**
   * Convert a byte array to its hexadecimal string representation
   */
  def bytesToHex(bytes: Seq[Byte]): String = bytes.map(b => f"$b%02x").mkString.toLowerCase

  /**
   * Convert a byte array to a UTF-8 string
   */
  def bytesToString(bytes: Seq[Byte]): String = bytes.map(_.toChar).mkString

  /**
   * Convert an hexadecimal string representation to its underlying byte array
   */
  def hexToBytes(hex: String): Seq[Byte] = hex.sliding(2,2).toArray.map(Integer.parseInt(_, 16).toByte)

  /**
   * Convert an hexadecimal string representation to its underlying integer
   */
  def hexToInt(hex: String): Int = Integer.parseInt(hex, 16)

  /**
   * Convert an integer to its hexadecimal representation
   */
  def intToHex(i: Int): String = i.toHexString
}
