package io.crumbl.crypto

import io.crumbl.BasicUnitSpecs
import io.crumbl.utils.Converter

/**
 * packageSpecs test class
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 1.0
 */
class packageSpecs extends BasicUnitSpecs {
  "crypto.hash()" should "be deterministic" in {
    val ref = "c0c77f225dd222144bc4ef79dca00ab7d955f26da2b1e0f25df81f8a7e86917c"
    val h = hash("Edgewhere".getBytes("utf-8"), DEFAULT_HASH_ENGINE)
    Converter.bytesToHex(h) should equal (ref)

    the [Exception] thrownBy {
      hash("Edgewhere".getBytes("utf-8"), "wrong-hash-engine")
    } should have message "invalid hash engine"
  }
}
