package io.crumbl.decrypter

import io.crumbl.BasicUnitSpecs
import io.crumbl.models.core.Base64
import io.crumbl.slicer.Slicer.Slice

/**
 * UncrumbSpecs test class
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 1.0
 */
class UncrumbSpecs extends BasicUnitSpecs {
  "Uncrumb.parse()" should "be deterministc" in {
    val uncrumbStr = "%01RWRnZXdoZXJl"
    val (idx, dec) = Uncrumb.parse(uncrumbStr)
    idx should equal (1)
    dec should equal ("RWRnZXdoZXJl")
    Base64(dec).decoded should equal ("Edgewhere")
  }

  "Uncrumb.toUncrumb()" should "extract the right uncrumb from a string" in {
    val ref = Uncrumb(Base64("RWRnZXdoZXJl"), 1)
    val str = "01RWRnZXdoZXJl" // PARTIAL_PREFIX is ignored anyway
    val uncrumb = Uncrumb.toUncrumb(str)
    uncrumb should equal (ref)
  }

  "toSlice" should "return the appropriate slice" in {
    val ref: Slice = "Edgewhere"
    val uncrumb = Uncrumb(Base64("RWRnZXdoZXJl"), 1)
    val slice = uncrumb.toSlice
    slice should equal (ref)
  }
}
