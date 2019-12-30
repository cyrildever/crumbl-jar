package io.crumbl.obfuscator

import io.crumbl.utils.Converter
import java.security.MessageDigest

/**
 * Feistel implementation
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 1.0
 */
object Feistel {
  /**
   * Round is the main function of the Feistel algoritmh.
   * It's applied at each round of the obfuscation process to the right side of the Feistel cipher.
   *
   * @param item    The right side to apply the function to
   * @param number  The number of rounds
   * @param key     The key to use
   * @return the processed string to xor with the left side
   */
  def round(item: String, number: Int, key: String): String = {
    // First, add passed item to key extraction
    val addition = Feistel.add(item, Feistel.extract(key, number, item.length))
    // Then, hash the addition
    val digest = MessageDigest.getInstance("sha-256")
    val hash = digest.digest(addition.getBytes("utf-8"))
    // Finally, keep what's needed
    Feistel.extract(Converter.bytesToHex(hash), number, item.length)
  }

  /**
   * Adds two strings in the sense that each codePoint are added
   */
  def add(str1: String, str2: String): String = {
    if (str1.length == str2.length) {
      val added = new StringBuilder
      for (i <- 0 until str1.length) {
        added ++= Character.toChars(str1.codePointAt(i) + str2.codePointAt(i))
      }
      added.toString
    } else throw new Exception("to be added, strings must be of the same length")
  }

  /**
   * @return an extraction of the passed string of the desired length from the passed start index.
   * If the desired length is too long, the key string is repeated.
   */
  def extract(from: String, startIndex: Int, desiredLength: Int): String = {
    val start = startIndex % from.length
    val lengthNeeded = start + desiredLength
    (from * Math.ceil(lengthNeeded.toDouble / from.length.toDouble).toInt).substring(start, lengthNeeded)
  }

  /**
   * Splits a string in two equal parts
   */
  def split(data: String): (String, String) = {
    if (data.length % 2 == 0) {
      val half = data.length / 2
      (data.substring(0, half), data.substring(half))
    } else throw new Exception("invalid string length: cannot be split")
  }

  /**
   * Xor two strings in the sense that each codePoints are xored
   */
  def xor(item1: String, item2: String): String = {
    val b = new StringBuilder
    for (i <- 0 until item1.length) {
      b ++= Character.toChars(item1.codePointAt(i) ^ item2.codePointAt(i))
    }
    b.toString
  }
}
