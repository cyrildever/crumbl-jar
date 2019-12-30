package io.crumbl.models.core

/**
 * Signer class
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 1.0
 */
case class Signer(
  encryptionAlgorithm: String,
  privateKey: Option[Either[String, Seq[Byte]]],
  publicKey: Option[Either[String, Seq[Byte]]]
) {
  def isEmpty: Boolean = this == Signer.EMPTY
}
object Signer {
  val EMPTY = Signer("", None, None)
}
