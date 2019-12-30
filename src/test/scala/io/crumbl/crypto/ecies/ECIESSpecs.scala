package io.crumbl.crypto.ecies

import io.crumbl.BasicUnitSpecs
import io.crumbl.models.core.Base64
import io.crumbl.utils.Converter
import scala.io.Source

/**
 * ECIESSpecs test class
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 1.0
 */
class ECIESSpecs extends BasicUnitSpecs {
  "ECIES.encrypt()" should "not be deterministic" in {
    val pub = Source.fromResource("keys/owner1.pub").getLines.next
    val pk = Converter.hexToBytes(pub)
    val msg = "Edgewhere".getBytes

    val crypted1 = ECIES.encrypt(msg, pk)
    println(Base64.toBase64(crypted1).getString)
    crypted1 shouldNot have size 0

    val crypted2 = ECIES.encrypt(msg, pk)
    Base64.toBase64(crypted1).getString shouldNot equal (Base64.toBase64(crypted2).getString)
  }

  "ECIES.decrypt" should "decrypt an existing ciphered text" in {
    val ref = "Edgewhere".getBytes
    val priv = Source.fromResource("keys/owner1.sk").getLines.next
    val sk = Converter.hexToBytes(priv)

    val ciphered = Base64("BB76SDcT8FvJbVVs5J7jECfGGOk1T38wx1z8U9erOsyh8JOnYnVtHk7NXbB/FAj8nUpkUPSHBRIVx6+8ChdQ6L7mKSL099Odnomtl+0GMMd6mVOKj7r8Mt6klrSOiHUaq0wsATDThYTl8lGdPwsECQQT8waX+KOWjfo=")
    val decrypted = ECIES.decrypt(ciphered.getBytes, sk)
    decrypted should equal (ref)
  }
}
