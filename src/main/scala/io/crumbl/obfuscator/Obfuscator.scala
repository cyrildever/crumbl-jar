package io.crumbl.obfuscator

import io.crumbl.utils.{Converter, Logging, Padder}

/**
 * Obfuscator class
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 1.0
 * @constructor   Creates an instance of an `Obfuscator` with the key to use and the number of rounds to apply.
 * @param key     The key to use
 * @param rounds  The number of rounds to apply
 */
final case class Obfuscator(key: String, rounds: Int) extends Logging {
  /**
   * Transforms the passed string to an obfuscated byte array through a Feistel cipher
   */
  def applyTo(data: String): Seq[Byte] = {
    val dataToUse = if (data.length % 2 == 1) Padder.leftPad(data, data.length + 1) else data
    // Apply the Feistel cipher
    var parts = Feistel.split(dataToUse)
    for (i <- 0 until rounds) {
      val rnd = Feistel.round(parts._2, i, key)
      val tmp = Feistel.xor(parts._1, rnd)
      parts = (parts._2, tmp)
    }
    (parts._1 + parts._2).getBytes
  }

  /**
   * Transforms the passed obfuscated byte array to a deobfuscated string through a Feistel cipher
   */
  def unapplyTo(obfuscated: Seq[Byte]): String = {
    if (obfuscated.length % 2 == 0) {
      // Apply Feistel cipher
      val parts = Feistel.split(Converter.bytesToString(obfuscated))
      var a = parts._2
      var b = parts._1
      for (i <- 0 until rounds) {
        val rnd = Feistel.round(b, rounds - i - 1, key)
        val tmp = Feistel.xor(a, rnd)
        a = b
        b = tmp
      }
      Padder.unpad(b + a)
    } else {
      throw new Exception("invalid obfuscated data")
    }
  }
}
object Obfuscator {
  val DEFAULT_KEY_STRING = "8ed9dcc1701c064f0fd7ae235f15143f989920e0ee9658bb7882c8d7d5f05692" // SHA-256("crumbl by Edgewhere")
  val DEFAULT_ROUNDS = 10
}
