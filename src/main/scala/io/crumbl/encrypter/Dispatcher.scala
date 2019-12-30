package io.crumbl.encrypter

import io.crumbl.models.core.Signer
import scala.collection.mutable.ArrayBuffer

/**
 * Dispatcher class
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 1.0
 */
case class Dispatcher(numberOfSlices: Int, trustees: Seq[Signer]) {
  /**
   * Returns a map of slice index -> trustees to sign.
   * It tries to uniformly distribute slices to trustees so that no trustee sign all slices and
   * all slices are at least signed twice if possible.
   * NB: the first slice (index 0) is reserved for data owners, so it should not be allocated.
   */
  def allocate(): Map[Int, Seq[Signer]] = {
    val allocation = scala.collection.mutable.Map[Int, ArrayBuffer[Signer]]()
    val numberOfTrustees = trustees.length
    numberOfTrustees match {
      case 1 =>
        // All slices must be signed by the single trustee
        for (i <- 1 until numberOfSlices) {
          allocation += (i -> ArrayBuffer(trustees(0)))
        }
      case 2 =>
        // Slices should all be in double but first and last
        for (i <- 1 until numberOfSlices) {
          if (i == 1) {
            allocation += (i -> ArrayBuffer(trustees(0)))
          } else if (i == numberOfSlices - 1) {
            allocation += (i -> ArrayBuffer(trustees(1)))
          } else {
            allocation += (i -> ArrayBuffer(trustees(0), trustees(1)))
          }
        }
      case 3 => {
        // Slices must be allocated to n-1 trustees at most, and no trustee can have it al
        val rng = new scala.util.Random(System.currentTimeMillis)
        // TODO Enhance: too naive!
        val combinations = Seq(
          Seq((1, 2), (1, 3), (2, 3)),
          Seq((1, 2), (2, 3), (1, 3)),
          Seq((1, 3), (2, 3), (1, 2)),
          Seq((1, 3), (1, 2), (2, 3)),
          Seq((2, 3), (1, 3), (1, 2)),
          Seq((2, 3), (1, 2), (1, 3))
        )
        val chosen = rng.nextInt(combinations.length)
        for (i <- 0 until 3) {
          val idx = combinations(chosen)(i)
          allocation += (i + 1 -> ArrayBuffer(trustees(idx._1 - 1), trustees(idx._2 - 1)))
        }
      }
      case _ => throw new Exception("wrong number of trustees")
    }
    allocation.map { case (k, v) => k -> v.toSeq }.toMap
  }
}
