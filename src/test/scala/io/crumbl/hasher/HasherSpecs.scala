package io.crumbl.hasher

import io.crumbl.{BasicUnitSpecs, crypto}
import io.crumbl.encrypter.Crumb
import io.crumbl.models.core.Base64
import io.crumbl.utils.Converter

/**
 * SlicerSpecs test class
 *
 * @author  Cyril Dever
 * @since   2.0
 * @version 1.0
 */
class HasherSpecs extends BasicUnitSpecs {
  val crumbs = Seq(Crumb(index = 0, length = 12, encrypted = Base64("RWRnZXdoZXJl")))

  "Hasher.applyTo()" should "build the appropriate hashered prefix" in {
    val source = "data to hash"
    val hashered = Hasher(crumbs).applyTo(source)
    hashered should equal ("c5066fffa7ee8e9a2013c62b465c993d51f9ec5191435c3e078d8801859c74d6")

    val hash = crypto.hash(source.getBytes, crypto.DEFAULT_HASH_ENGINE)
    val firstChars = Converter.bytesToHex(hash).substring(0, 32)
    firstChars should equal (hashered.substring(0, 32))

    the [Exception] thrownBy {
      val notOwnerCrumbs = Seq(Crumb(
        index = 1, // Wrong index
        length = 12,
        encrypted = Base64("RWRnZXdoZXJl")
      ))
      Hasher(notOwnerCrumbs).applyTo(source)
    } should have message "owner's crumbs not present"
  }

  "Hasher.unapplyTo()" should "fined the adequate hash" in {
    val hashered = "c5066fffa7ee8e9a2013c62b465c993d51f9ec5191435c3e078d8801859c74d6"
    val hash = Hasher(crumbs).unapplyTo(hashered)
    hash should equal ("c5066fffa7ee8e9a2013c62b465c993d149d8b34e62b394c62c8ec66e0eb1cb3")

    // Like in TypeScript and Golang
    val crumbs2 = Seq(
      Crumb(index = 0, length = 164, encrypted = Base64("BHbIv7jmXdjb3n7+/Ewg1Br4EXYksmb4RmavTQsDVJAFY/he1rs7gpRY9+waPCww3YMzr5IIWFYN8LDVDDxV2bdTGBNjHWbsb4WRM2ItLFVYubxBto/6pfGyXJ0ZriYjL2RCVtPw8cdJrkeXqSqOEz1ICBTN9UOBZw==")),
      Crumb(index = 0, length = 164, encrypted = Base64("BLaggeNjl7tsFhAymEipkuIco8xG8trm6E+4WzrTX7spC0DmdyTFhU8enJV2WERxOaF7iy6E64/2+2wIL/MqRMnyRnJJJDab6d7pTWMSEVOUAvEFugTP95XpnnZcDyzLdZ2J+YBbMYtRcnxH4TJA9AIwksl0oJRA/w=="))
    )
    val expected = "cc8b00be1cc7592806ba4f8fe4411d3744121dc12e6201dc36e873f7fd9a6bae"
    val hashered2 = "cc8b00be1cc7592806ba4f8fe4411d374064d57e96845c04ed360d0901d64b7a"
    val found = Hasher(crumbs2).unapplyTo(hashered2)
    found should equal (expected)
  }
}
