package io.crumbl.utils

import io.crumbl.BasicUnitSpecs

/**
 * PadderSpecs test class
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 1.0
 */
class PadderSpecs extends BasicUnitSpecs {
  "Padder" should "left pad a string" in {
    val length = 12

    val str = "test"
    val padded = Padder.leftPad(str, length)
    padded should have size length

    val padding = Padder.LEFT_PADDING_CHARACTER.toString * 8
    padded should equal (padding + str)

    val unpadded = Padder.unpad(padded)
    unpadded should equal (str)

    val empty = ""
    val emptyPadded = Padder.leftPad(empty, length)
    emptyPadded should have size length
    val emptyUnpadded = Padder.unpad(emptyPadded)
    emptyUnpadded should equal (empty)
  }

  it should "right pad a string" in {
    val length = 12

    val str = "test"
    val padded = Padder.rightPad(str, length)
    padded should have size length

    val unpadded = Padder.unpad(padded)
    unpadded should equal (str)
  }

  it should "unpad a string correctly" in {
    val leftLength = 12
    val rightLength = 16

    val str = "test"
    var padded = Padder.leftPad(str, leftLength)
    padded = Padder.rightPad(padded, rightLength)
    padded should have size rightLength

    val unpadded = Padder.unpad(padded)
    unpadded should equal (str)

    // A padding character in the middle of the string shouldn't change the unpadding behaviour
    val manInTheMiddle = "test" + Padder.LEFT_PADDING_CHARACTER + "test"
    padded = Padder.leftPad(manInTheMiddle, leftLength)
    padded should have size leftLength
    val unpadManInTheMiddle = Padder.unpad(padded)
    unpadManInTheMiddle should equal (manInTheMiddle)
  }
}
