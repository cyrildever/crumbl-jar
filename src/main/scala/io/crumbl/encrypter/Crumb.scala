package io.crumbl.encrypter

import io.crumbl.models.core.Base64
import io.crumbl.utils.Converter

/**
 * Crumbl holds the encrypted slice, its index and length.
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 1.0
 */
case class Crumb(encrypted: Base64, index: Int, length: Int) {
  /**
   * toString transforms the Crumb into its stringified representation.
   *
   * The construct is as follows:
   * - the first two characters are the hexadecimal representation of the index;
   * - the following four characters are the hexadecimal representation of the length of the encrypted data to follow;
   * - the base64-encoded string.
   * NB: the condition to only use four characters for the length of the encrypted data implies that
   * this encrypted crumb shouldn't be longer than 65 535 characters, ie. 64 ko.
   */
  override def toString: String = "%02x%04x%s".format(index, length, encrypted.getString)
}
object Crumb {
  /**
   * Extracts the index, the encrypted length and text from the passed string
   */
  def parse(unparsed: String): (Int, Int, String) = {
    val idxHex = unparsed.substring(0, 2)
    val idx = Converter.hexToInt(idxHex)
    val lnHex = unparsed.substring(2, 6)
    val ln = Converter.hexToInt(lnHex)
    val enc = unparsed.substring(6)
    if (!Base64.isBase64String(enc)) {
      throw new Exception("not a base64-encoded string")
    } else if (ln != enc.length) {
      throw new Exception("incompatible lengths")
    } else {
      (idx, ln, enc)
    }
  }

  /**
   * @return a Crumb from the passed unparsed string
   */
  def toCrumb(unparsed: String): Crumb = {
    val (idx, ln, enc) = Crumb.parse(unparsed)
    Crumb(Base64(enc), idx, ln)
  }
}
