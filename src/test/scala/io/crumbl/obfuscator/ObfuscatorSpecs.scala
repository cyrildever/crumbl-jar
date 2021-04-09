package io.crumbl.obfuscator

import fr.edgewhere.feistel.Feistel
import io.crumbl.BasicUnitSpecs
import io.crumbl.utils.Converter

/**
 * ObfuscatorSpecs test class
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 2.0
 */
class ObfuscatorSpecs extends BasicUnitSpecs {
  private val cipher = Feistel.FPECipher(Obfuscator.DEFAULT_HASH, Obfuscator.DEFAULT_KEY_STRING, Obfuscator.DEFAULT_ROUNDS)

  "Obfuscator.applyTo()" should "correctly obfuscate an input" in {
    val obfuscator = Obfuscator(cipher)
    val obfuscated = obfuscator.applyTo("Edgewhere")
    Converter.bytesToHex(obfuscated) should equal ("3d7c0a0f51415a521054")
  }

  "Obfuscator.unapplyTo()" should "return the appropriate deobfuscated string" in {
    val obfuscated = Converter.hexToBytes("3d7c0a0f51415a521054")
    val obfuscator = Obfuscator(cipher)
    val deobfuscated = obfuscator.unapplyTo(obfuscated)
    deobfuscated should equal ("Edgewhere")
  }
}
