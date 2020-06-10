package io.crumbl.padder

import java.util.logging.Logger

/**
 * Padder object
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 2.0
 */
object Padder {
  val LEFT_PADDING_CHARACTER        = '\u0002' // Unicode U+0002: start of text
  val RIGHT_PADDING_CHARACTER       = '\u0003' // Unicode U+0003: end of text
  val ALTERNATE_PADDING_CHARACTER_1 = '\u0004' // Unicode U+0004: end-of-transmission
  val ALTERNATE_PADDING_CHARACTER_2 = '\u0005' // Unicode U+0005: enquiry
  val NO_PADDING_CHARACTER          = '\u0000' // Unicode U+0000: nul

  val PREPEND_SIZE = 2

  /**
   * Eventually left pad the passed data (generally a Slice)
   */
  def applyTo(slice: Seq[Byte], length: Int, buildEven: Boolean): (Seq[Byte], Char) = if (slice.isEmpty) {
    throw new Exception("empty slice")
  } else if (length < 1 ) {
    throw new Exception("max slice length too short")
  } else if (buildEven && length != slice.length && length % 2 != 0) {
    throw new Exception("wished length is not even")
  } else if (buildEven && slice.length % 2 == 0 && slice.length >= length) {
    (slice, NO_PADDING_CHARACTER)
  } else {
    // 1- Choose padding character
    val firstByte = slice.head
    val lastByte = slice.last
    val pc = if (firstByte == LEFT_PADDING_CHARACTER.toByte) {
      if (lastByte == ALTERNATE_PADDING_CHARACTER_1.toByte) {
        ALTERNATE_PADDING_CHARACTER_2
      } else {
        ALTERNATE_PADDING_CHARACTER_1
      }
    } else {
      if (lastByte == LEFT_PADDING_CHARACTER.toByte) {
        if (firstByte == ALTERNATE_PADDING_CHARACTER_1.toByte) {
          ALTERNATE_PADDING_CHARACTER_2
        } else {
          ALTERNATE_PADDING_CHARACTER_1
        }
      } else {
        LEFT_PADDING_CHARACTER
      }
    }

    // 2- Define filling delta
    var delta = math.max(0, length - slice.length)
    if (buildEven) {
      if ((slice.length + delta) % 2 != 0) {
        delta += 1
      }
    } else {
      delta += PREPEND_SIZE
    }

    // 3- Do pad
    ((0 until delta).map(_ => pc.toByte) ++ slice, pc)
  }

  /**
   * Removes the left padding from the passed padded data
   */
  def unapplyTo(padded: Seq[Byte]): (Seq[Byte], Char) = if (padded.length < 2) {
    throw new Exception("invalid padded data: too short")
  } else {
    // 1- Detect padding character
    val pc = padded.head
    if (pc.toChar != LEFT_PADDING_CHARACTER &&
      pc.toChar != ALTERNATE_PADDING_CHARACTER_1 &&
      pc.toChar != ALTERNATE_PADDING_CHARACTER_2) {
      if (padded.length % 2 == 0) {
        // It's probably a data that would have been padded only if it were of odd length,
        // hence probably padded with 'buildEven' set to `true`
        (padded, NO_PADDING_CHARACTER)
      } else {
        throw new Exception("invalid padded data: wrong padding")
      }
    } else {
      // 2- test prepend sequence
      if (padded.length < PREPEND_SIZE + 1) {
        throw new Exception("invalid data: padded data too short")
      }
      if (padded(PREPEND_SIZE - 1) != pc && padded.length % 2 == 1) {
        Logger.getLogger("io.crumbl.Padder").warning("possibly wrong padding: data is not of even length and prepend size wasn't respected") // TODO Change to exception?
      }

      // 3- Do unpad
      val unpadded = padded.dropWhile(_.equals(pc))
      if (unpadded.isEmpty) {
        throw new Exception("invalid padded data: all pad chars")
      }
      (unpadded, pc.toChar)
    }
  }

  /**
   * Pad with left padding character(s) the passed string until the `minLength` is reached
   *
   * @deprecated
   */
  def leftPad(str: String, minLength: Int): String =
    str.reverse.padTo(minLength, LEFT_PADDING_CHARACTER).reverse

  /**
   * Pad with right padding character(s) the passed string until the `minLength` is reached
   *
   * @deprecated
   */
  def rightPad(str: String, minLength: Int): String =
    str.padTo(minLength, RIGHT_PADDING_CHARACTER)

  /**
   * Get rid of any left and/or right padding character(s) to the passed string
   *
   * @deprecated
   */
  def unpad(str: String): String = {
    val regexLeft = "^\\" + LEFT_PADDING_CHARACTER + "+"
    val regexRight = "\\" + RIGHT_PADDING_CHARACTER + "+$"
    str.replaceAll(regexLeft, "").replaceAll(regexRight, "")
  }
}
