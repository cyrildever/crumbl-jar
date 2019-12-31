package io.crumbl.decrypter

import io.crumbl.models.core.Base64
import io.crumbl.slicer.Slicer.Slice
import io.crumbl.utils.Converter

/**
 * Uncrumb class
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 1.0
 *
 * @param deciphered  The base64-encoded data after being run through the decryption process
 * @param index       The slice index
 */
final case class Uncrumb(
  deciphered: Base64,
  index: Int
) {
  /**
   * @return `true` if the deciphered data is empty or the index is incoherent, `false` otherwise
   */
  def isEmpty: Boolean = deciphered.length == 0 || index < 0

  /**
   * @return the slice off the deciphered input
   */
  def toSlice: Slice = deciphered.decoded

  /**
   * toString transforms the Uncrumb into its stringified representation.
   *
   * The construct is as follows:
   * - the uncrumb's partial prefix;
   * - the following two characters are the hexadecimal representation of the index, ie. Converter.IntToHex(Index);
   * - the base64-encoded deciphered slice.
   */
  override def toString: String = "%s%02x%s".format(Uncrumb.PARTIAL_PREFIX, index, deciphered.getString)
}
object Uncrumb {
  val PARTIAL_PREFIX = "%"

  val EMPTY: Uncrumb = Uncrumb(Base64(""), -1)

  /**
   * Extracts the index, the deciphered text from the passed string
   */
  def parse(unparsed: String): (Int, String) = {
    val toUnparse = if (unparsed.startsWith(PARTIAL_PREFIX)) unparsed.substring(1) else unparsed
    val idxHex = toUnparse.substring(0, 2)
    val idx = Converter.hexToInt(idxHex)
    val dec = toUnparse.substring(2)
    (idx, dec)
  }

  /**
   * @return an Uncrumb from the passed string
   */
  def toUncrumb(unparsed: String): Uncrumb = {
    val (idx, dec) = parse(unparsed)
    Uncrumb(Base64(dec), idx)
  }
}
