package io.crumbl.slicer

import io.crumbl.slicer.Seed.Seeder
import io.crumbl.utils.Padder
import scala.collection.mutable.ArrayBuffer

/**
 * Slicer class
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 1.0
 */
final case class Slicer(numberOfSlices: Int, deltaMax: Int) {
  import Slicer._

  /**
   * @return the slices from the passed data
   */
  def applyTo(data: String): Seq[Slice] = {
    val fixedLength = Math.floor(data.length.toDouble / numberOfSlices).toInt + deltaMax
    split(data).map(Padder.leftPad(_, fixedLength))
  }

  /**
   * Rebuild the original data from the passed slices
   */
  def unapplyTo(slices: Array[Slice]): String = {
    if (slices.length != 0) {
      slices.map(Padder.unpad(_)).mkString
    } else throw new Exception("impossible to use empty slices")
  }

  /**
   * Splits the passed data using a mask
   *
   * @param data  The data to split
   */
  def split(data: String): Seq[String] = {
    buildSplitMask(data.length, Seed.seederFor(data))
      .map{ mask => data.substring(mask.start, mask.end) }
  }

  private case class mask(start: Int, end: Int)
  private def buildSplitMask(dataLength: Int, seed: Seeder): Seq[mask] = {
    val masks = new ArrayBuffer[mask]()
    val dl = dataLength.toDouble
    val nos = numberOfSlices.toDouble
    val dm = deltaMax.toDouble
    val averageSliceLength = Math.floor(dl / nos)
    val minLen = Math.max(averageSliceLength - Math.floor(dm / 2), Math.floor(dl / (nos + 1) + 1))
    val maxLen = Math.min(averageSliceLength + Math.floor(dm / 2), Math.ceil(dl / (nos - 1) - 1))
    val delta = Math.min(dm, maxLen - minLen)
    var length = 0
    var usedDataLength = dataLength
    val rng = new scala.util.Random(seed)
    while (usedDataLength > 0) {
      val rnd = rng.nextDouble
      val randomNum = Math.floor(rnd * (Math.min(maxLen, dl) + 1 - minLen) + minLen)
      if (randomNum != 0) {
        val b = Math.floor((dl - randomNum) / minLen)
        val r = Math.floor((usedDataLength - randomNum.toInt) % minLen.toInt)
        if (r <= b * delta) {
          val m = mask(length, Math.min(dl, length + randomNum).toInt)
          masks += m
          length = (length + randomNum).toInt
          usedDataLength = (usedDataLength - randomNum).toInt
        }
      }
    }
    if (masks.length != 0) {
      masks.toSeq
    } else throw new Exception("unable to build split masks")
  }
}
object Slicer {
  val MAX_SLICES = 4 // The owner of the data + 3 trustees is optimal as of this version
  val MAX_DELTA = 5
  val MIN_INPUT_SIZE = 8 // Input below 8 characters must be left-padded
  val MIN_SLICE_SIZE = 2

  type Slice = String

  def getDeltaMax(dataLength: Int, numberOfSlices: Int): Int = {
    val sliceSize = Math.ceil(dataLength.toDouble / numberOfSlices.toDouble).toInt
    if (dataLength <= MIN_INPUT_SIZE || sliceSize <= MIN_SLICE_SIZE) {
      0
    } else {
      var deltaMax = 1
      while (deltaMax < 2 * (sliceSize - MIN_SLICE_SIZE) && deltaMax < MAX_DELTA) {
        deltaMax = deltaMax + 1
      }
      deltaMax
    }
  }
}
