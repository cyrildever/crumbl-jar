package io.crumbl.slicer

/**
 * Seed utility
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 1.0
 */
object Seed {
  type Seeder = Int

  /**
   * Generate a deterministic seed from the passed data
   */
  def seederFor(data: String): Seeder = {
    var s: Seeder = 0
    for (i <- 0 until data.length) {
      s += data.codePointAt(i)
    }
    s
  }
}
