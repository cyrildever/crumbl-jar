package io.crumbl.slicer

import io.crumbl.BasicUnitSpecs

/**
 * SlicerSpecs test class
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 1.1
 */
class SlicerSpecs extends BasicUnitSpecs {
  "Slicer.applyTo()" should "split data in appropriate slices" in {
    val s1 = Slicer(4, 0)
    val slices1 = s1.applyTo("11111222223333344444")
    slices1 should have size s1.numberOfSlices
    slices1(0) should equal("11111")
    slices1(1) should equal("22222")
    slices1(2) should equal("33333")
    slices1(3) should equal("44444")

    val s2 = Slicer(4, 2)
    val slices2 = s2.applyTo("111111111222222222333333333444444444")
    for (slice <- slices2) {
      slice.length should equal (11)
    }
    slices2(3) should equal ("\u0002\u0002\u0002\u00024444444") // It's predictive thanks to the seed
  }

  "Slicer.unapplyTo()" should "return appropriate results" in {
    import Slicer._

    val s = Slicer(4, 0)
    val s1: Slice = "11111"
    val s2: Slice = "22222"
    val s3: Slice = "33333"
    val s4: Slice = "44444"
    val slices = Array(s1, s2, s3, s4)
    val data = s.unapplyTo(slices)
    data should equal ("11111222223333344444")

    the [Exception] thrownBy {
      val emptySlices = new Array[Slice](0)
      s.unapplyTo(emptySlices)
    } should have message "impossible to use empty slices"
  }

  "Slicer.getDeltaMax" should "return the right delta max" in {
    var dMax = Slicer.getDeltaMax(8, 4)
    dMax should equal (0)
    dMax = Slicer.getDeltaMax(12, 4)
    dMax should equal (2)
    dMax = Slicer.getDeltaMax(16, 4)
    dMax should equal (4)
    dMax = Slicer.getDeltaMax(20, 4)
    dMax should equal (5)
    dMax = Slicer.getDeltaMax(50, 4)
    dMax should equal (Slicer.MAX_DELTA)
  }

  "Slicer" should "work under heavy load" in {
    (1 until 10000).foreach(x => {
      val data = (Math.random() + x).toString + "abcdefghij"
      val s = Slicer(10, Slicer.getDeltaMax(data.length, 10))
      val found = s.unapplyTo(s.applyTo(data))
      found should equal (data)
    })
  }
}
