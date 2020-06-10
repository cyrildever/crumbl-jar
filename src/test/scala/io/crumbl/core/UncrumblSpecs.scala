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
 * @version 1.0
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
