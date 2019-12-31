package io.crumbl.models.core

/**
 * Signer class
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 1.0
 *
 * @param encryptionAlgorithm   The name of the encryption algorithm used
 * @param privateKey            The optional byte array of the private key
 * @param publicKey             The optional byte array of the public key
 */
final case class Signer(
  encryptionAlgorithm: String,
  privateKey: Option[Either[String, Seq[Byte]]],
  publicKey: Option[Either[String, Seq[Byte]]]
) { self =>
  /**
   * @return `true` if no field is filled, `false` otherwise
   */
  def isEmpty: Boolean = self == Signer.EMPTY
}
object Signer {
  val EMPTY: Signer = Signer("", None, None)
}
