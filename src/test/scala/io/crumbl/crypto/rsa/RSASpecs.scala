package io.crumbl.crypto.rsa

import io.crumbl.BasicUnitSpecs
import io.crumbl.models.core.Base64
import scala.io.Source

/**
 * RSASpecs test class
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 1.0
 */
class RSASpecs extends BasicUnitSpecs {
  "RSA.encrypt()" should "not be deterministic" in {
    val pk = Source.fromResource("keys/trustee2.pub").getLines.mkString("\n")
    val msg = "Edgewhere".getBytes("utf-8")

    val crypted1 = RSA.encrypt(msg, pk)
    println(Base64.toBase64(crypted1).getString)
    crypted1 shouldNot have size 0

    val crypted2 = RSA.encrypt(msg, pk)
    Base64.toBase64(crypted1).getString shouldNot equal (Base64.toBase64(crypted2).getString)
  }
  it should "not accept too long input" in {
    val veryLongStr = "abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrs"
    val pk = Source.fromResource("keys/trustee2.pub").getLines.mkString("\n")

    // For RSA key of 2048 bits (256 bytes), using the SHA-512 hash of 64 bytes long with OAEP,
    // the RSA padding will be 2 * 64 + 2 = 130 bytes.
    // Hence, the max length of the data would be 256 - 130 = 126 bytes.
    veryLongStr.length should be > 126

    the [Exception] thrownBy {
      RSA.encrypt(veryLongStr.getBytes, pk)
    } should have message "too much data for RSA block"
  }

  "RSA.decrypt()" should "decrypt an existing ciphered text" in {
    val ref = "Edgewhere".getBytes("utf-8")
    val sk = Source.fromResource("keys/trustee2.sk").getLines.mkString("\n")
    val ciphered = Base64("QkordMcNgkQEV3NU5d2zcfmPfmUHnj/bXg7TpcgQqQzpuUhoExNpjNarNoZ+HMwRAzhtqzyIoaFERsTRi8lMiehX9+dvEZNqqNvCt5huRkgwW0g+FHYi2TTdgmCLuKJoBwtsun17o69HeoK9nmG6UXvocx/OPzUJEgHIVggW3ibk4j/uvCtCPiL44IV86JsOMaJewbKEXNMGGWuKsN25c93vr6tS+B4YhR5VFWc93ENdnK+3SIwcOGfNaJLunmRN96AsdDLU9J3Bsl93JH8xSnW1Q8paKqCliFxHXOAvsWbcGRMO2FfDXLCf+bBBZLxQrfSg7O+tn1WQfe0UVjY7Sw==")

    var decrypted = RSA.decrypt(ciphered.getBytes, sk)
    decrypted should equal (ref)

    // see 'crumb-js/test/src/typescript/crypto/rsa/index.spec.ts' test
    val fromTypescript = Base64("Y7MqItzPFiWIgyhzXNqllmZnaIT1N82kMBfExUv0XrJMrXLRfp/60zSZJbcSIWxXxqqCpW99bcjFtadzveMUaf/T8DvHyXmJXVtOb28ep9mzSoIkyveGxIKZ1347A9kQ2FIzbNlC4UH3ooROu+BXHw/VpaYZCOcupO2RqXC/6OLYi8g02uZQZiIbnkrx/jOXDK/HyQabhb24y+7i53QTROonJUXQE2cE+Q7AIFN7mOZR718dqWu2jGllGFeE5nABreTG6ySqzvVOisPrTqlojXHHe/StCwp8R/oP+cmQN2M1lvzMxFOE26pTNEU1oiJCWBV07aoXZofz/g8hKDL1xg==")
    decrypted = RSA.decrypt(fromTypescript.getBytes, sk)
    decrypted should equal (ref)
  }
}
