package io.crumbl.utils

/**
 * Padder utility
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 1.0
 */
object Padder {
  val LEFT_PADDING_CHARACTER = '\u0002' // Unicode U+0002: start of text
  val RIGHT_PADDING_CHARACTER = '\u0003' // Unicode U+0003: end of text

  def leftPad(str: String, minLength: Int): String =
    str.reverse.padTo(minLength, LEFT_PADDING_CHARACTER).reverse

  def rightPad(str: String, minLength: Int): String =
    str.padTo(minLength, RIGHT_PADDING_CHARACTER)

  def unpad(str: String): String = {
    val regexLeft = "^\\" + LEFT_PADDING_CHARACTER + "+"
    val regexRight = "\\" + RIGHT_PADDING_CHARACTER + "+$"
    str.replaceAll(regexLeft, "").replaceAll(regexRight, "")
  }
}
