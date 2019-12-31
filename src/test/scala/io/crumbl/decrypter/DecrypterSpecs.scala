package io.crumbl.decrypter

import io.crumbl.encrypter.Crumb
import io.crumbl.{BasicUnitSpecs, crypto}
import io.crumbl.models.core.{Base64, Signer}
import io.crumbl.utils.Converter

/**
 * DecrypterSpecs test class
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 1.0
 */
class DecrypterSpecs extends BasicUnitSpecs {
  "Decrypter.decrypt()" should "not be able to work with another keypair" in {
    // Equivalent to 'crumbl-exe/decrypter/decrypter_test.go' tests
    val ref = "AgICAmoMD1lNU0g="
    val ciphered = "BFimUWhXgnYhTPo7CAQfxBcctdESBrpB/0ECaTPArpxNFr9hLUIJ2nLEwxm2F6xFu8d5sgA9QJqI/Y/PVDem9IxuiFJsAU+4CeUVKYw/nSwwt4Nco8EGBgPY03ekxLD2T3Zp0Z+jowTPtCGHwtuwYE+INwjQgti0Io6E1Q=="

    val owner1_sk = Converter.hexToBytes("b9fc3b425d6c1745b9c963c97e6e1d4c1db7a093a36e0cf7c0bf85dc1130b8a0")
    val owner1_pk = Converter.hexToBytes("04e315a987bd79b9f49d3a1c8bd1ef5a401a242820d52a3f22505da81dfcd992cc5c6e2ae9bc0754856ca68652516551d46121daa37afc609036ab5754fe7a82a3")
    val owner1 = Signer(crypto.ECIES_ALGORITHM, Some(Right(owner1_sk)), Some(Right(owner1_pk)))
    val crumb = Crumb(Base64(ciphered), 2, ciphered.length)
    val found = Decrypter.decrypt(crumb, owner1)
    found.deciphered.getString should equal (ref)

    val trustee1_sk = Converter.hexToBytes("80219e4d24caf16cb4755c1ae85bad02b6a3efb1e3233379af6f2cc1a18442c4")
    val trustee1_pk = Converter.hexToBytes("040c96f971c0edf58fe4afbf8735581be05554a8a725eae2b7ad2b1c6fcb7b39ef4e7252ed5b17940a9201c089bf75cb11f97e5c53333a424e4ebcca36065e0bc0")
    val trustee1 = Signer(crypto.ECIES_ALGORITHM, Some(Right(trustee1_sk)), Some(Right(trustee1_pk)))
    the [Exception] thrownBy {
      Decrypter.decrypt(crumb, trustee1)
    } should have message "Incorrect MAC"
  }
}
