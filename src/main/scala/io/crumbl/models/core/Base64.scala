package io.crumbl.models.core

import java.util.{Base64 => JBase64}

/**
 * Base64 is the base64 string representation of a byte array.
 *
 * NB: if the passed string is not a valid base64 representation,
 * it will not throw an error but rather returns empty or nil items when methods are called.
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 1.0
 */
case class Base64(str: String) {
  def decoded: String = JBase64.getDecoder.decode(getString).map(_.toChar).mkString

  def getBytes: Seq[Byte] = if (! toString.isEmpty) {
    JBase64.getDecoder.decode(str)
  } else Seq[Byte] ()

  def getString: String = if (Base64.isBase64String(str)) str else ""

  lazy val length: Int = getString.length
}
object Base64 {
  /**
   * @return `true` if the passed string is a base64-encode string representation, `false` otherwise
   */
  def isBase64String(str: String): Boolean = try {
    JBase64.getDecoder.decode(str)
    true
  } catch {
    case _ => false
  }

  /**
   * Convert a byte array to a Base64 instance
   */
  def toBase64(bytes: Seq[Byte]): Base64 = Base64(JBase64.getEncoder.encodeToString(bytes.toArray))
}
