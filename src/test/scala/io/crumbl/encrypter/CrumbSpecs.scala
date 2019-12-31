package io.crumbl.encrypter

import io.crumbl.BasicUnitSpecs
import io.crumbl.models.core.Base64

/**
 * CrumbSpecs test class
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 1.0
 */
class CrumbSpecs extends BasicUnitSpecs {
  "Crumb.parse()" should "extract the index, the length and the encrypted text correctly" in {
    val crumbStr = "01000cRWRnZXdoZXJl"
    val (idx, ln, enc) = Crumb.parse(crumbStr)
    idx should equal (1)
    ln should equal (12)
    enc should equal ("RWRnZXdoZXJl")
    Base64("RWRnZXdoZXJl").decoded should equal ("Edgewhere")
  }

  "Crumb.toCrumb()" should "be deterministic" in {
    val ref = Crumb(Base64("RWRnZXdoZXJl"), 1, 12)
    val str = "01000cRWRnZXdoZXJl"
    val crumb = Crumb.toCrumb(str)
    crumb should equal (ref)
  }

  "Crumb.toString" should "format the crumb correctly" in {
    val ref = "01000cRWRnZXdoZXJl"
    val c = Crumb(Base64("RWRnZXdoZXJl"), 1, 12)
    c.toString should equal (ref)
  }
}
