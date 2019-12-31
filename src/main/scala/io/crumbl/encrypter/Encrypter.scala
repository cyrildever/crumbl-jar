package io.crumbl.encrypter

import io.crumbl.crypto
import io.crumbl.crypto.ecies.ECIES
import io.crumbl.crypto.rsa.RSA
import io.crumbl.models.core.{Base64, Signer}
import io.crumbl.slicer.Slicer.Slice

/**
 * Encrypter class
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 1.0
 */
object Encrypter {
  /**
   * Returns the base64-encoded encrypted slice as crumb:
   * it takes the slice data and index as well as the signer as arguments,
   * and returns the corresponding `Crumb` object.
   */
  def encrypt(data: Slice, index: Int, s: Signer): Crumb = s.publicKey match {
    case Some(pk) => {
      val enc = s.encryptionAlgorithm match {
        case crypto.ECIES_ALGORITHM => pk match {
          case Right(pubkey) => ECIES.encrypt(data.getBytes, pubkey)
          case _ => throw new Exception("missing public key bytes")
        }
        case crypto.RSA_ALGORITHM => pk match {
          case Left(pubkey) => RSA.encrypt(data.getBytes, pubkey)
          case _ => throw new Exception("missing public key string")
        }
        case _ => throw new Exception(s"unknown encryption algorithm: ${s.encryptionAlgorithm}")
      }
      val b64 = Base64.toBase64(enc)
      Crumb(b64, index, b64.length)
    }
    case _ => throw new Exception("invalid empty public key")
  }
}
