package io.crumbl.utils

object XOR {
  /**
   * Returns the XOR of the two passed byte arrays
   */
  def bytes(arr1: Seq[Byte], arr2: Seq[Byte]): Seq[Byte] = if (arr1.length != arr2.length) {
    throw new Exception("items are not of the same length")
  } else {
    arr1.zipWithIndex.map { case (b, i) => b ^ arr2(i) }.map(_.toByte)
  }

  /**
   * Returns the XOR of the two passed strings
   */
  def strings(str1: String, str2: String): String =
    bytes(str1.getBytes, str2.getBytes).map(_.toChar).mkString

}
