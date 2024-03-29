package io.crumbl.core

import io.crumbl.decrypter.Uncrumb
import io.crumbl.encrypter.Crumbs
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
 * @version 2.0
 */
class UncrumblSpecs extends BasicUnitSpecs {
  "process" should "return the appropriate uncrumbled data" in {
    val owner1_privkey = Converter.hexToBytes("b9fc3b425d6c1745b9c963c97e6e1d4c1db7a093a36e0cf7c0bf85dc1130b8a0") // see '../../../resources/keys/owner1.sk'
    val trustee1_privkey = Converter.hexToBytes("80219e4d24caf16cb4755c1ae85bad02b6a3efb1e3233379af6f2cc1a18442c4") // see '../../../resources/keys/trustee1.sk'
    val trustee2_privkey = Source.fromResource("keys/trustee2.sk").getLines.mkString("\n")

    val ref = "cdever@edgewhere.fr"
    val crumbled = "580fb8a91f05833200dea7d33536aaec995cb2ed83f99c68d99a3f114d5b93e20000a8BCBZyOlhxIaxeQ/wa+HTf8o6EV/pvnNfS+Bc3zcYsbSvU0nK8asl058RYeSg+ierk8siW1os/GGVI9S9jG+j3S9iPrVAImWQBa8TmK7FKJZZCGadmvgXwOwYk13tnhzRztn8XgRBuD3Sz1pl/2NwLrnf6Gzd65S6R3atXg==0100a8BG5EjHp5w+jIsOOS+ioCU6kZVx1AzLQx/IxmuaBsVLuPd2bPvH5cZBB92MDS1YnapeSsHLQ4sQT1oy7jT9Mj50Ncjqy0Tqo87H6l9OTPrqj/elN/v9fxpFd7r1zxiljAM31tLyvYCqfGmAhqnyscWOOPA83MmY1jZNAy+A==020158SWu9PrmVHwZqNfBvxAeXa85Q11/l3jUevcfqujYIR1Xw+PzaHjqSAca1zkSLtSvgwnIQKiKt/ug6ox/bpF3QU4wIDg/VFzG0pzE6IgWlWzWYbl4gRlBiGSXQT5MCu4zJvlmusH7UqANYZlGhNdapkJqalMV3ir06RXIIS3ffWvUxdprU6mP5MQqfYj6creGfBxXc7SSyLgL3znPFSb+ddz4TVbc7+sbAjS0LCPrYXm6bBxx7KVuJMjIQmWNqObD5mtiLLTyhmhLvNJ21zgz+pB6sRVq61hT7fKJ5TFsUNkkKQk/HmOld8N38usv1xdZQKRrIoQ5m+C3pMKbhyaS8TA==.1"

    val verificationHash = crypto.hash(ref.getBytes, crypto.DEFAULT_HASH_ENGINE)

    // 1- As trustees
    val uncrumbs = new ArrayBuffer[Seq[Byte]]()
    val uTrustee1 = Uncrumbl(
      crumbled,
      None,
      Some("580fb8a91f05833200dea7d33536aaec9d7ceb256a9858ee68e330e126ba409d"),
      Signer(crypto.ECIES_ALGORITHM, Some(Right(trustee1_privkey)), None)
    )
    val uncrumb1 = uTrustee1.process
    Converter.bytesToString(uncrumb1) should equal ("580fb8a91f05833200dea7d33536aaec9d7ceb256a9858ee68e330e126ba409d%01AgIEVQMOTg9cRwk=.1") // This shall be returned by the trustee
    uncrumbs += uncrumb1

    val uTrustee2 = Uncrumbl(
      crumbled,
      None,
      Some("580fb8a91f05833200dea7d33536aaec9d7ceb256a9858ee68e330e126ba409d"),
      Signer(crypto.RSA_ALGORITHM, Some(Left(trustee2_privkey)), None)
    )
    val uncrumb2 = uTrustee2.process
    Converter.bytesToString(uncrumb2) should equal ("580fb8a91f05833200dea7d33536aaec9d7ceb256a9858ee68e330e126ba409d%02AgICAgICAgIYUkI=.1") // This shall be returned by the trustee
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
  it should "work as the owner" in {
    val ref = Seq[Byte](99, 121, 114, 105, 108, 64, 100, 101, 118, 101, 114, 46, 99, 111, 109)
    val verificationHash = "cc8b00be1cc7592806ba4f8fe4411d3744121dc12e6201dc36e873f7fd9a6bae"
    val crumbled = "cc8b00be1cc7592806ba4f8fe4411d3740260115ca8498eec69facc7ca3a4c4a0000a4BPJlFcqvSkE5WJbaV2urVqK+1ui5Ig1YGzKFL7CVhqYi1VK7benFrsOLsXQez2lxzCujoTsL2tzkNsvc0gy5EoOSSlWuuwNthUZInXS9qVG1sJR0E8Jy25xidKNWpRhXoSjtZI93IRfO954Hh0oHDkhpmYf85MwdDA==0000a4BDQc1OTmmTLwd98wN6An5B/NdobkO8Z9uNjvtbGi3q9ei3w4YeUcNPrnDKM9ErJsmJdlRdWVeTiKF1qsEfBPkKtuQQnH6EIamN0sxX/8zg9NDKGtsiS12x3DA3+TcI/6oMqYqk7DgGqFkZPBksk09GJnG/r3frkiqA==0100a4BGnrJEMP0eDfuJHxcv+OxxK3sDqReN6mRru5Kv3MYBv6SYIxq5+B9TQLAV1O6G/c5tRQt035JaFAO94shyGPZqZv4sV8105XH3wbQmN+KhKWalNj3+Cu8qCUbxI+U97XoM1pQi3oZ1j3uul7RO6aFR26pgMcZYm61w==0100a4BGOPcjP5wzwoSld3+X1yvnK5OfusNRkXWkZQzrotJlwJ1utC2554wkwViyLBCGyvIzQKd3L8JOy8smAUg8S67EYTO8FhZRBzz+KnN4k4Asu6uAQRs6iwqFyta9p8sE+rZ0QuAsKzxnNI6HRkFAK1/h3+5JGJp7nXDg==0200a4BPsT4fQJJ4g/wAVJWgBhCLVTWZgliRcSdc8A7J+hiyOG2mEUf/lNayj5z9lakcHK+DELYqbl+GXr3LPve9njifIO9f0rNRSVRErS72kic+CEuFVDjs6N5cKTRGYe2nEBiZXQ6tRTbMDFclInoAMGj+Yh7eWEiYXNpw==0200a4BGCdVlb8xc1w4yuTaHP86YeO8F1SgDzkrn0UgWV0D+8ELWXb4mc4qX1RKSxCMooeWwMiEQFyiHK4q3hTUlNXIkR2G6o6JQE9JwMd8Trw0AjV6aowZ9/e3e1FLWHr8k/0jFMTYa0/DG63uw3yYd/WYLIFht7F0leMUw==0300a4BE5WGLZX+jLrcxCz381u9Vx1yECpLIVg/SXv5/lx4lHZpxXywQMuRpEUQKF5QwDXrF8x6HrOHjLl2/9UmL7p7c7H295bXwt+Ok3Pk8+x+2ulXRnlrghvfvBiaclYcuitR58+g5y7cJWKCf1T3Z5+5Ufh01wEnBK+jg==0300a4BBL5L3jo8KNHN5UErzQ3j+9c2OndPWf6hqs43fek6xfU5tWTpcf86yb/ZSxkcj1fe7ZF+v5x788mjPUJfOC+WyCUZ/+du8atZVkbT4pliSj85vBc2O1brupGKIvhfRPRNzoqpocPSyPAnu/rkS7FOgj+hYjEBiX3LQ==.1"
    val owner = Signer(crypto.ECIES_ALGORITHM, Some(Right(Converter.hexToBytes("325ab5aace32d823f990a2057119bedddf0d7a8ddc8b19e61d89e73d4531c587"))), None)
    val uncrumb1 = Uncrumb.toUncrumb("01BAQEBAYkUgI=")
    val uncrumb2 = Uncrumb.toUncrumb("02AgICAk5WGh0=")
    val uncrumb3 = Uncrumb.toUncrumb("03BAQEBARRVgI=")
    val uncrumbler = new Uncrumbl(crumbled, Some(List(uncrumb1, uncrumb2, uncrumb3)), Some(verificationHash), owner, true)
    val uncrumbled = uncrumbler.process
    uncrumbled should equal (ref)
  }

  "Uncrumbl.extractData()" should "enforce verification hash checking" in {
    val verificationHash = "580fb8a91f05833200dea7d33536aaec9d7ceb256a9858ee68e330e126ba409d"
    val crumbled = "580fb8a91f05833200dea7d33536aaec99df7af5098d296c1bf90c28cebf76d70000a8BKOR0GMVcYJzGjzJ6AU2Sq15QrxbTql/gkWpW5mddRjqpnmDym4HdkBl9Pt9bSPYEcXiBqJqUcTm0oZIevcqWlZPjM4GbBdL6qgR/wBAXZ1I1zah4czAJBMKMdbmOyuAo06NsCu0bqX0DkcZt2UuOax9ATVQymOiDwHzSg==0100a8BHYwWaREFtdRInDh5c/mweXM0SitlvO7hDzlAC8F3TvpYewDaBn4/FiFonKbINTwPQeoibIkXMFw+HKkXn5FEJ8pNtWalbgAFoO2Iy++FguMOt0aH00PVyV4LNBrEy9RUx5C1tJ5kxM6wTua/3mXsOMtXcjoVPIldF+zLg==020158ciYehulbTIg2p8EueDXRWl7EmnSsLo+sbOPsogMamWxs0MkzXhqPhpTfgKsBnnQ4g32rnwvQs5JyVfOR33LKz+XIgiwSSszOE96TTSp6EMDlEuijp/PK1qmCha/TAJ+o8odJy6tasON/OwWCRUq6hX+ZwKaw1s+xYWpsK7u1ugAtitjpo66xt0LZTtZpmAqT9jZa+1CXQLUsT/TGHovsq45GZJe9zLnuPgxVKjbHrS9xHKSU1P7aQ7zcaZwh+PAmxu+egFXC5U5YYeFmoCBXbN9dk8/sG+qb/T5Z1wVhcogVic6aYNmigxY/DpoErUCooyEGwJVunu+7vpgFeMnz+w==.1"
    val (vh, crumbs) = Uncrumbl.extractData(crumbled)
    verificationHash should equal (vh)
    Crumbs.getAt(0, crumbs) should have size 1
    Crumbs.getAt(0, crumbs)(0).toString should equal ("0000a8BKOR0GMVcYJzGjzJ6AU2Sq15QrxbTql/gkWpW5mddRjqpnmDym4HdkBl9Pt9bSPYEcXiBqJqUcTm0oZIevcqWlZPjM4GbBdL6qgR/wBAXZ1I1zah4czAJBMKMdbmOyuAo06NsCu0bqX0DkcZt2UuOax9ATVQymOiDwHzSg==")

    val crumbled2 = "cc8b00be1cc7592806ba4f8fe4411d374064d57e96845c04ed360d0901d64b7a0000a4BHbIv7jmXdjb3n7+/Ewg1Br4EXYksmb4RmavTQsDVJAFY/he1rs7gpRY9+waPCww3YMzr5IIWFYN8LDVDDxV2bdTGBNjHWbsb4WRM2ItLFVYubxBto/6pfGyXJ0ZriYjL2RCVtPw8cdJrkeXqSqOEz1ICBTN9UOBZw==0000a4BLaggeNjl7tsFhAymEipkuIco8xG8trm6E+4WzrTX7spC0DmdyTFhU8enJV2WERxOaF7iy6E64/2+2wIL/MqRMnyRnJJJDab6d7pTWMSEVOUAvEFugTP95XpnnZcDyzLdZ2J+YBbMYtRcnxH4TJA9AIwksl0oJRA/w==0100a4BJOU/0Jo239EUuDd6Lxu8gRICsND71bSg+qY3n1n7o/EA+d5g6orQnpw4Zr5QnfNQIUxgMCmNqI/rY5EpsR+emtHVIF6OS+1cgKhkwhbBHrNRZDsnOvH3XBVrIOk6CpEm1+hPRNt1E6h6zFr7EfDenRhE0RxWth42g==0100a4BPKEPk14AeNPSiPos2Ob5wML/1Oyn3zJDXdKr9nCEct7HhSGu348ESZrhbp14nsNEihbrh6tzOOj11KqfJWeq7PEROAnynm9K2fLp6rl7o1vwAgW5f+4a/7tzn3rH8XkoHbamHfAU1GGYwop1QPFvXhr+FC8SKwuuQ==0200a4BFq+ZlkogXqxBDlTZvMZbCEqi6F480LSEZioZLcj8aGBmT2c+yzY2DR66+KinG1qaYQ+rGRRenMut+dcuRIKbVA5tNZYXJAWBtL3mStYpaDWqYBTIG06Ml3tjpD4+Jr3xgI0lmczL53CUm0SzL7eXaBbxMtQmcfg6Q==0200a4BIq9Hdb9zuSd+YwNwD9kUXuY0RogGe6NNWfpXhhbmbCGM/mhuX9qegKotLit7Ws0osggpVLSQ9b2yV8krH5DUToJNuxdCiRTvKMtgMJpWtDbYuV3yxoITb2azq7u9G2SVkWGaqXUsAupe9si7nIc4maeufh1vXkgWA==0300a4BLHdrR+dhbUfm9O5LByNZfXmJIpEGr2yy6+nyQG1GtLpdMLbUrzStYExEoAfQPuPtpKZmyWCBHleVQVrF71mPBjeiTCLyyuLm6qMXJ6lxQ0D/7jUcKQX6vUymtnEevw0326nD/lobnxp65lhg30cnDsYngT5+AeKSw==0300a4BAhTnL9o/IxxAyVqoal6hn9JlwZCn+SDnuBkhH4fCBrOC+btqM/D3NIDcR1+Zdiy55k9doYtiaeYcdvKGlXbwE3rwK94heEHgEzTysX18ciduFMpgoc1x0aY7MfUipyEw5y5165NFhX6U3lZJ7501AU9kAZ3iHAePA==.1"
    val (vh2, crumbs2) = Uncrumbl.extractData(crumbled2)
    vh2 should equal ("cc8b00be1cc7592806ba4f8fe4411d3744121dc12e6201dc36e873f7fd9a6bae")
    crumbs2.zipWithIndex.foreach { case (crumb, idx) =>
      if (idx == 0) {
        crumb.index should equal (0)
        crumb.encrypted.getString should equal ("BHbIv7jmXdjb3n7+/Ewg1Br4EXYksmb4RmavTQsDVJAFY/he1rs7gpRY9+waPCww3YMzr5IIWFYN8LDVDDxV2bdTGBNjHWbsb4WRM2ItLFVYubxBto/6pfGyXJ0ZriYjL2RCVtPw8cdJrkeXqSqOEz1ICBTN9UOBZw==")
      } else if (idx == 1) {
        crumb.index should equal (0)
        crumb.encrypted.getString should equal ("BLaggeNjl7tsFhAymEipkuIco8xG8trm6E+4WzrTX7spC0DmdyTFhU8enJV2WERxOaF7iy6E64/2+2wIL/MqRMnyRnJJJDab6d7pTWMSEVOUAvEFugTP95XpnnZcDyzLdZ2J+YBbMYtRcnxH4TJA9AIwksl0oJRA/w==")
      }
    }
  }
}
