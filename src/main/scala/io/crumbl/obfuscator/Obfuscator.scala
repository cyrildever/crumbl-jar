package io.crumbl.obfuscator

import fr.edgewhere.feistel.Feistel
import fr.edgewhere.feistel.common.utils.base256.Readable
import fr.edgewhere.feistel.common.utils.base256.Readable._
import fr.edgewhere.feistel.common.utils.hash.Engine._
import io.crumbl.padder.Padder
import io.crumbl.utils.Logging
import java.security.Security

/**
 * Obfuscator class
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 3.0
 * @constructor   Creates an instance of an `Obfuscator` with the cipher to use.
 *
 * @param cipher  The FPE cipher to use
 */
final case class Obfuscator(cipher: Feistel.FPECipher) extends Logging {
  self =>

  Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider())

  /**
   * Transforms the passed string to an obfuscated byte array through a Feistel cipher
   */
  def applyTo(data: String): Seq[Byte] = {
    val dataToUse = if (data.length % 2 == 1) {
      val (padded, _) = Padder.applyTo(data.getBytes, data.length + 1, buildEven = true)
      padded.map(_.toChar).mkString
    } else data
    // Apply the Feistel cipher
    self.cipher.encrypt(dataToUse).bytes
  }

  /**
   * Transforms the passed obfuscated byte array to a deobfuscated string through a Feistel cipher
   */
  def unapplyTo(obfuscated: Seq[Byte]): String = {
    if (obfuscated.length % 2 == 0) {
      // Apply Feistel cipher
      val b = self.cipher.decrypt(Readable(obfuscated))
      val (unpadded, _) = Padder.unapplyTo(b.getBytes)
      unpadded.map(_.toChar).mkString
    } else {
      throw new Exception("invalid obfuscated data")
    }
  }
}
object Obfuscator {
  val DEFAULT_HASH: Engine = SHA_256
  val DEFAULT_KEY_STRING = "8ed9dcc1701c064f0fd7ae235f15143f989920e0ee9658bb7882c8d7d5f05692" // SHA-256("crumbl by Edgewhere")
  val DEFAULT_ROUNDS = 10
}
