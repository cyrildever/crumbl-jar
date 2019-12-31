package io.crumbl.core

import io.crumbl.decrypter.Uncrumb
import io.crumbl.models.core.Signer
import io.crumbl.utils.Converter
import io.crumbl.{BasicUnitSpecs, crypto}
import scala.collection.mutable.ArrayBuffer
import scala.io.Source

/**
 * UncrumblSpecs test class
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 1.0
 */
class UncrumblSpecs extends BasicUnitSpecs {
  "process" should "return the appropriate uncrumbled data" in {
    val owner1_privkey = Converter.hexToBytes("b9fc3b425d6c1745b9c963c97e6e1d4c1db7a093a36e0cf7c0bf85dc1130b8a0") // see '../../../resources/keys/owner1.sk'
    val trustee1_privkey = Converter.hexToBytes("80219e4d24caf16cb4755c1ae85bad02b6a3efb1e3233379af6f2cc1a18442c4") // see '../../../resources/keys/trustee1.sk'
    val trustee2_privkey = Source.fromResource("keys/trustee2.sk").getLines.mkString("\n")

    val ref = "cdever@edgewhere.fr"
    val crumbled = "580fb8a91f05833200dea7d33536aaec9d7ceb256a9858ee68e330e126ba409d0000a8BJM2I8mS/bkFNdZOATg8jHsQbzYp4o5rTqYWkf/pqgvkH7a4OijBxy86W1y2J+pB525jYO4iBuig2JswBdNv++8dkb0GcSXT873M0I5Xma9oM83eHXihOF2rqnqWN/RNZPwJSM23DcCj/xyVs1FK5jWVMGxtMLttIN7vqg==010158KbcQ6boXhkGdXR97+UwSHvt12wEwkVa57e+2m+66sTu32luP00cWET2gb01tgNZYjU621U7u4RI6fmz5kkyTSZtjPJ5wXISTf2wOBv5cY94LvgYoyMFKP9J3mGbPgAKGGsIdY4GCQBx6+Gi7VzfuNxdP1YHAPqcpKXPWiY+nmqYhT7eZVZlmNF1UmkMbgrneYglenmKxWSyUA6P7yMj3LrhlKekWAPdWpMLzRftLh1oH5e2KHkz7Wyh9eYOCKXlQ4sUUm8o3i0Inann41wL0KGaNajPU1RP0M9n3/Zil1/T+ZZcNJgSlQh1mxVKX1ztBRqYNUy+pqDat1qq6ED5r5A==0200a8BIIMyYgouCq7ZVy7S1kRJUl1Lg+aQMHoNeo7SauKwsy//XZ5rJOF4FrYMXmPpu0pf7nwCgAgk6Iv9IQK+WXsKpDE+QazdPpYFtxm4/1qi8qnzG1Wp/9Lf5nFTozacHqghz2e7XkaO1qyLNfmzimpsm6aw/lhEsd+djJ8KA==.1"

    val verificationHash = crypto.hash(ref.getBytes("utf-8"), crypto.DEFAULT_HASH_ENGINE)

    // 1- As trustees
    val uncrumbs = new ArrayBuffer[Seq[Byte]]()
    val uTrustee1 = Uncrumbl(
      crumbled,
      None,
      Some("580fb8a91f05833200dea7d33536aaec9d7ceb256a9858ee68e330e126ba409d"),
      Signer(crypto.ECIES_ALGORITHM, Some(Right(trustee1_privkey)), None)
    )
    val uncrumb1 = uTrustee1.process
    Converter.bytesToString(uncrumb1) should equal ("580fb8a91f05833200dea7d33536aaec9d7ceb256a9858ee68e330e126ba409d%02AgICAgICAgkYUkI=.1") // This shall be returned by the trustee
    uncrumbs += uncrumb1

    val uTrustee2 = Uncrumbl(
      crumbled,
      None,
      Some("580fb8a91f05833200dea7d33536aaec9d7ceb256a9858ee68e330e126ba409d"),
      Signer(crypto.RSA_ALGORITHM, Some(Left(trustee2_privkey)), None)
    )
    val uncrumb2 = uTrustee2.process
    Converter.bytesToString(uncrumb2) should equal ("580fb8a91f05833200dea7d33536aaec9d7ceb256a9858ee68e330e126ba409d%01AgICBFUDDk4PXEc=.1") // This shall be returned by the trustee
    uncrumbs += uncrumb2

    // 2- As an owner
    val fromTrustees = uncrumbs.flatMap(uncrumb => {
      val parts = Converter.bytesToString(uncrumb).split("\\.")
      if (parts(1) != Crumbl.VERSION) throw new Exception(s"invalid version: ${parts(1)}")
      val us = parts(0).substring(crypto.DEFAULT_HASH_LENGTH)
      val uncs = us.split("\\" + Uncrumb.PARTIAL_PREFIX)
      uncs.map(unc => if (!unc.isEmpty) {
        Uncrumb.toUncrumb(unc)
      } else Uncrumb.EMPTY)
    }).filterNot(_.isEmpty)

    val uOwner = Uncrumbl(
      crumbled,
      Some(fromTrustees.toSeq),
      Some(Converter.bytesToHex(verificationHash)),
      Signer(crypto.ECIES_ALGORITHM, Some(Right(owner1_privkey)), None),
      isOwner = true
    )
    val uncrumbled = uOwner.process
    Converter.bytesToString(uncrumbled) should equal (ref)
  }
}
