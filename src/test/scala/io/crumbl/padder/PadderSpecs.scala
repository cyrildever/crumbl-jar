package io.crumbl.padder

import io.crumbl.BasicUnitSpecs

/**
 * PadderSpecs test class
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 2.0
 */
class PadderSpecs extends BasicUnitSpecs {
  private def toBytes(xs: Char*): Seq[Byte] = xs.map(_.toByte)

  "Padder.applyTo()" should "left pad a string appropriately" in {
    val maxSliceLength = 3
    val slice1 = "abc".getBytes
    val (padded1, padChar1) = Padder.applyTo(slice1, maxSliceLength, buildEven = false)
    padded1 should equal (toBytes('\u0002', '\u0002', 'a', 'b', 'c'))
    padChar1 should equal (Padder.LEFT_PADDING_CHARACTER)
    padded1 should have size (maxSliceLength + Padder.PREPEND_SIZE)

    val slice2 = "de".getBytes
    val (padded2, padChar2) = Padder.applyTo(slice2, maxSliceLength, buildEven = false)
    padded2 should equal (toBytes('\u0002', '\u0002', '\u0002', 'd', 'e'))
    padChar2 should equal (Padder.LEFT_PADDING_CHARACTER)
    padded2 should have size (maxSliceLength + Padder.PREPEND_SIZE)

    val slice3 = toBytes('\u0002', 'a')
    val (padded3, padChar3) = Padder.applyTo(slice3, maxSliceLength, buildEven = false)
    padded3 should equal (toBytes(padChar3, padChar3, padChar3, '\u0002', 'a'))
    padChar3.toByte should equal (padded3.head)
    padChar3 should equal (Padder.ALTERNATE_PADDING_CHARACTER_1)
    padded3 should have size (maxSliceLength + Padder.PREPEND_SIZE)

    val slice4 = toBytes('\u0002', '\u0004')
    val (padded4, padChar4) = Padder.applyTo(slice4, maxSliceLength, buildEven = false)
    padded4 should equal (toBytes(padChar4, padChar4, padChar4, '\u0002', '\u0004'))
    padChar4 should equal (Padder.ALTERNATE_PADDING_CHARACTER_2)

    val expected = toBytes(Padder.LEFT_PADDING_CHARACTER, 1, 1, 1)
    val slice5 = Seq[Byte](1, 1, 1)
    val (padded5, _) = Padder.applyTo(slice5, slice5.length, buildEven = true)
    padded5 should have size 4
    padded5 should equal (expected)

    val wrongSlice: Seq[Byte] = Seq.empty
    the [Exception] thrownBy {
      Padder.applyTo(wrongSlice, maxSliceLength, buildEven = false)
    } should have message "empty slice"

    val wrongLength = 0
    the [Exception] thrownBy {
      Padder.applyTo(slice1, wrongLength, buildEven = false)
    } should have message "max slice length too short"

    val alreadyEvenData = "22"
    val (padded, _) = Padder.applyTo(alreadyEvenData.getBytes, alreadyEvenData.length, buildEven = true)
    padded should equal (alreadyEvenData.getBytes)

    val alreadyEvenButTooShort = toBytes('\u0004', '\u0004')
    val wishedLength = 4
    val (paddedWished, _) = Padder.applyTo(alreadyEvenButTooShort, wishedLength, buildEven = true)
    paddedWished should equal (toBytes(Padder.LEFT_PADDING_CHARACTER, Padder.LEFT_PADDING_CHARACTER, '\u0004', '\u0004'))
    paddedWished should have size wishedLength

    the [Exception] thrownBy {
      Padder.applyTo(slice1, 5, buildEven = true)
    } should have message "wished length is not even"
  }

  "Padder.unapplyTo()" should "unpad the passed padded data" in {
    val padded1 = Seq[Byte](2, 2, 3, 4, 5)
    val (unpadded1, padChar1) = Padder.unapplyTo(padded1)
    unpadded1 should equal (Seq[Byte](3, 4, 5))
    padChar1 should equal (Padder.LEFT_PADDING_CHARACTER)

    val padded2 = Seq[Byte](2, 2, 2)
    the [Exception] thrownBy {
      Padder.unapplyTo(padded2)
    } should have message "invalid padded data: all pad chars"

    val padded3 = Seq[Byte](5, 5, 5, 2, 4)
    val (unpadded3, padChar3) = Padder.unapplyTo(padded3)
    unpadded3 should equal (Seq[Byte](2, 4))
    padChar3 should equal (Padder.ALTERNATE_PADDING_CHARACTER_2)

    val evenData = "12"
    val (unpadded4, padChar4) = Padder.unapplyTo(evenData.getBytes)
    unpadded4 should equal (evenData.getBytes)
    padChar4 should equal (Padder.NO_PADDING_CHARACTER)

    val evenZero = Seq[Byte](0, 0)
    val (unpadded5, padChar5) = Padder.unapplyTo(evenZero)
    unpadded5 should equal (evenZero)
    padChar5 should equal (Padder.NO_PADDING_CHARACTER) // doesn't mean that it has taken zero value as pad character

    val wrongPadded = Seq[Byte](127, 126, 125)
    the [Exception] thrownBy {
      Padder.unapplyTo(wrongPadded)
    } should have message "invalid padded data: wrong padding"
  }
}
