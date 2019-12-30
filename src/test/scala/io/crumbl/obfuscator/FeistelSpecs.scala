package io.crumbl.obfuscator

import io.crumbl.BasicUnitSpecs

/**
 * FeistelSpecs test class
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 1.0
 */
class FeistelSpecs extends BasicUnitSpecs {
  "Feistel.add()" should "add two strings" in {
    val added1 = Feistel.add("a", "b")
    added1 should equal ("Ã")

    val added2 = Feistel.add("ab", "cd")
    added2 should equal ("ÄÆ")
  }

  "Feistel.extract()" should "extract the right string from another" in {
    val key = "abcd"

    val ext1 = Feistel.extract(key, 1, 2)
    ext1 should equal ("bc")

    val ext2 = Feistel.extract(key, 7, 6)
    ext2 should equal ("dabcda")
  }

  "Feistel.round()" should "apply the round method appropriately" in {
    val key = Obfuscator.DEFAULT_KEY_STRING
    val data1 = Array(1, 0, 0, 0, 0, 0, 0, 0).map(_.toByte)
    val round1 = Feistel.round(data1.toString, 5, key)
    val bytes1 = round1.getBytes
    val data2 = Array(1, 1, 0, 0, 0, 0, 0, 0).map(_.toByte)
    val round2 = Feistel.round(data2.toString, 5, key)
    val bytes2 = round2.getBytes
    round1 should not equal (round2)
  }

  "Feistel.split()" should "split a string in two parts of equal size" in {
    val str = "half1half2"
    val (part1, part2) = Feistel.split(str)
    part1 should equal ("half1")
    part2 should equal ("half2")
  }

  "Feistel.xor()" should "xor two strings appropriately" in {
    val xor1 = Feistel.xor("a", "b")
    xor1.charAt(0) should equal ('\u0003')

    val xor2 = Feistel.xor("ab", "cd")
    xor2.toCharArray should equal (Array('\u0002', '\u0006'))
  }
}
