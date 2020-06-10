package io.crumbl.slicer

import io.crumbl.padder.Padder
import io.crumbl.slicer.Seed.Seeder

import scala.collection.mutable.ArrayBuffer

/**
 * Slicer class
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 2.0
 *
 * @param numberOfSlices  The number of slices to make
 * @param deltaMax        The maximum gap between the longest and the shortest slices
 */
final case class Slicer(
  numberOfSlices: Int,
  deltaMax: Int
) {
  import Slicer._

  /**
   * @return the slices from the passed data
   */
  def applyTo(data: String): Seq[Slice] = {
    val fixedLength = Math.floor(data.length.toDouble / numberOfSlices).toInt + deltaMax
    split(data).map(slice => {
      val (padded, _) = Padder.applyTo(slice.getBytes, fixedLength, buildEven = false)
      padded.map(_.toChar).mkString
    })
  }

  /**
   * Rebuild the original data from the passed slices
   */
  def unapplyTo(slices: Seq[Slice]): String = {
    if (slices.nonEmpty) {
      slices.map(slice => {
        val (unpadded, _) = Padder.unapplyTo(slice.getBytes)
        unpadded.map(_.toChar).mkString
      }).mkString
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

  private final case class mask(start: Int, end: Int)

  private def buildSplitMask(dataLength: Int, seed: Seeder): Seq[mask] = {
    val masks = new ArrayBuffer[mask]()
    val dl = dataLength.toDouble
    val nos = numberOfSlices.toDouble
    val dm = deltaMax.toDouble
    val averageSliceLength = Math.floor(dl / nos)
    var catchUp = dl - averageSliceLength * nos

    var length = 0.0
    var usedDataLength = dataLength
    var leftRound = nos
    val rng = new scala.util.Random(seed)
    while (usedDataLength > 0) {
      val randomNum = rng.nextDouble * dm / 2 + Math.floor(catchUp / leftRound)
      var addedNum = Math.min(usedDataLength, Math.ceil(randomNum) + averageSliceLength)
      // General rounding pb corrected at the end
      if (leftRound == 1 && length + addedNum < dl) {
        addedNum = dl - length
      }
      val m = mask(length.toInt, (length + addedNum).toInt)
      masks += m
      catchUp = dl - length - averageSliceLength * leftRound
      leftRound = leftRound - 1
      length = length + addedNum
      usedDataLength = usedDataLength - addedNum.toInt
    }
    masks.toSeq
  }
}
object Slicer {
  val MAX_SLICES = 4 // The owner of the data + 3 trustees is optimal as of this version
  val MAX_DELTA = 5
  val MIN_INPUT_SIZE = 8 // Input below 8 characters must be left-padded
  val MIN_SLICE_SIZE = 2

  type Slice = String

  /**
   * @return the maximum gap between the longest and the shortest slices given the passed parameters
   */
  def getDeltaMax(dataLength: Int, numberOfSlices: Int): Int = {
    val sliceSize = Math.floor(dataLength.toDouble / numberOfSlices.toDouble).toInt
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
