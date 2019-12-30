package io.crumbl.encrypter

import io.crumbl.{BasicUnitSpecs, crypto}
import io.crumbl.models.core.Signer

/**
 * DispatcherSpecs test class
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 1.0
 */
class DispatcherSpecs extends BasicUnitSpecs {
  "Dispatcher.allocate()" should "dispatch signing trustees appropriately" in {
    val d = Dispatcher(4, Seq(
      Signer(crypto.ECIES_ALGORITHM, None, Some(Right(Seq[Byte](1.toByte)))),
      Signer(crypto.ECIES_ALGORITHM, None, Some(Right(Seq[Byte](2.toByte)))),
      Signer(crypto.ECIES_ALGORITHM, None, Some(Right(Seq[Byte](3.toByte))))
    ))
    val allocation = d.allocate()
    allocation(1) should have size 2
    allocation(1)(0).publicKey.get.right.get.diff(allocation(1)(1).publicKey.get.right.get) should not be empty
    allocation(2) should have size 2
    allocation(2)(0).publicKey.get.right.get.diff(allocation(2)(1).publicKey.get.right.get) should not be empty
    allocation(3) should have size 2
    allocation(3)(0).publicKey.get.right.get.diff(allocation(3)(1).publicKey.get.right.get) should not be empty
  }
}
