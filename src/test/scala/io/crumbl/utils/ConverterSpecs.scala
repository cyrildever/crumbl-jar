package io.crumbl.utils

import io.crumbl.BasicUnitSpecs

/**
 * ConverterSpecs test class
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 1.0
 */
class ConverterSpecs extends BasicUnitSpecs {
  "Converter.intToHex()" should "convert a byte array to its hexadecimal string representation" in {
    val ref1 = "ff"
    val i1 = 255
    val hex1 = Converter.intToHex(i1)
    hex1 should equal (ref1)

    val ref2 = "0"
    val i2 = 0
    val hex2 = Converter.intToHex(i2)
    hex2 should equal (ref2)
  }

  "Converter.hexToInt()" should "return the right integer from its hexadecimal string representation" in {
    val ref = 255
    val hex = "00ff"
    val i = Converter.hexToInt(hex)
    i should equal (ref)
  }

  "Converter.fromHex()" should "convert a byte array to its hexadecimal string representation" in {
    val ref = "Edgewhere".getBytes("utf-8")
    val bytes = Converter.hexToBytes("456467657768657265")
    bytes should equal (ref)
  }

  "Converter.bytestoHex()" should "convert a byte array to its hexadecimal string representation" in {
    val ref = "456467657768657265"
    val bytes = "Edgewhere".getBytes("utf-8")
    val hex = Converter.bytesToHex(bytes)
    hex should equal (ref)
  }

  "Converter.bytesToString" should "convert a byte array to a string" in {
    val ref = "Edgewhere"
    val bytes = Converter.hexToBytes("456467657768657265")
    val str = Converter.bytesToString(bytes)
    str should equal (ref)
  }
}
