package io.crumbl.decrypter

import io.crumbl.crypto
import io.crumbl.crypto.ecies.ECIES
import io.crumbl.crypto.rsa.RSA
import io.crumbl.encrypter.Crumb
import io.crumbl.models.core.{Base64, Signer}

/**
 * Decrypter class
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 1.0
 */
object Decrypter {
  /**
   * Returns the base64-encoded result of decryption, or throws exceptions on error.
   */
  def decrypt(enc: Crumb, s: Signer): Uncrumb = s.privateKey match {
    case Some(sk) => {
      val dec = s.encryptionAlgorithm match {
        case crypto.ECIES_ALGORITHM => sk match {
          case Right(privkey) => ECIES.decrypt(enc.encrypted.getBytes, privkey)
          case _ => throw new Exception("missing private key bytes")
        }
        case crypto.RSA_ALGORITHM => sk match {
          case Left(privkey) => RSA.decrypt(enc.encrypted.getBytes, privkey)
          case _ => throw new Exception("missing private key string")
        }
        case _ => throw new Exception(s"unknown encryption algorithm: ${s.encryptionAlgorithm}")
      }
      Uncrumb(Base64.toBase64(dec), enc.index)
    }
    case None => throw new Exception("invalid empty private key")
  }
}
