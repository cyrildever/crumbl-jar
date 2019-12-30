package io.crumbl.slicer

import io.crumbl.BasicUnitSpecs
import io.crumbl.slicer.Seed.Seeder

/**
 * SeedSpecs test class
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 1.0
 */
class SeedSpecs extends BasicUnitSpecs {
  "Seed.seederFor()" should "be deterministic" in {
    val ref1: Seeder = 195
    val data1 = "ab" // 97 + 98 = 195
    val seed1 = Seed.seederFor(data1)
    seed1 should equal (ref1)
  }
}
