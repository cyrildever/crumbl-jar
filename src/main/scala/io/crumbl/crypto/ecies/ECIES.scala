package io.crumbl.crypto.ecies

import java.security.interfaces.{ECPrivateKey, ECPublicKey}
import java.security.spec._
import java.security.{KeyPairGenerator, SecureRandom, _}
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}
import javax.crypto.{Cipher, KeyAgreement, Mac}
import org.bouncycastle.asn1.sec.SECNamedCurves
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.math.ec.FixedPointCombMultiplier
import org.bouncycastle.util.Arrays

/**
 * ECIES class as implemented in Go Ethereum `ecies` module (Geth)
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 1.0
 *
 * @see [[https://github.com/edgewhere/ecies-geth/]](Edgewhere's ecies-geth library)
 *      [[https://https://github.com/ethereum/go-ethereum/blob/master/crypto/ecies/ecies.go]](Go Ethereum ecies module)
 *      [[https://neilmadden.blog/2016/05/20/ephemeral-elliptic-curve-diffie-hellman-key-agreement-in-java/]]
 *      [[https://github.com/ConsenSys/cava/blob/master/crypto/src/main/java/net/consensys/cava/crypto/SECP256K1.java]]
 *      [[http://www.java2s.com/Code/Java/Security/BasicIOexamplewithCTRusingAES.htm]]
 *      [[https://github.com/swaldman/consuela/blob/master/src/main/scala/com/mchange/sc/v1/consuela/crypto/package.scala]]
 */
object ECIES {
  Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider())

  val HeaderByte = 0x04
  val InitializationVectorLength = 16
  val MetaLength: Int = 65 + 16 + 32
  val MacKeyLength = 32
  val PublicKeyLength = 65

  val algorithmName = "EC"
  val providerName = "BC"
  val curveName = "secp256k1"
  val hashAlgorithm = "sha-256"
  val symmetricEncryptionName = "AES/CTR/NoPadding"
  val hmacAlgorithm = "HmacSHA256"

  private def init(): (KeyFactory, ECGenParameterSpec, ECParameterSpec) = {
    val kf = KeyFactory.getInstance(algorithmName, providerName)
    val ecGenSpec = new ECGenParameterSpec(curveName)
    val ecParamSpec = {
      val algorithmParameters = AlgorithmParameters.getInstance(algorithmName, providerName)
      algorithmParameters.init(ecGenSpec)
      algorithmParameters.getParameterSpec(classOf[ECParameterSpec])
    }
    (kf, ecGenSpec, ecParamSpec)
  }

  /**
   * Encrypts with passed message with the public key
   */
  def encrypt(msg: Seq[Byte], publicKey: Seq[Byte]): Seq[Byte] = {
    val (_, ecGenSpec, _) = init()
    val keygen = KeyPairGenerator.getInstance(algorithmName, providerName)
    keygen.initialize(ecGenSpec, new SecureRandom())
    val ephemKeypair = keygen.generateKeyPair
    val sk = ephemKeypair.getPrivate.asInstanceOf[ECPrivateKey].getS.toByteArray
    val ephemPrivateKey = {
      if (sk.length < 32) sk.reverse.padTo(32, 0.toByte).reverse
      else if (sk.length > 32) {
        val diff = sk.length - 32
        val shorter = new Array[Byte](32)
        System.arraycopy(sk, diff, shorter, 0, 32)
        shorter
      } else sk
    }
    val ephemPublicKey = getPublic(ephemPrivateKey)
    val sharedPx = derive(ephemPrivateKey, publicKey)
    val hash = kdf(sharedPx, 32)
    val encryptionKey = hash.slice(0, 16)
    val rng = new SecureRandom()
    val IV = Array.ofDim[Byte](InitializationVectorLength)
    rng.nextBytes(IV)
    val macKey = MessageDigest.getInstance(hashAlgorithm).digest(hash.slice(16, hash.length).toArray)
    val cipherText = aes128CtrEncrypt(IV, encryptionKey, msg)
    val HMAC = hmacSha256(macKey, cipherText)
    ephemPublicKey ++ cipherText ++ HMAC
  }

  /**
   * Decrypts message using given private key
   */
  def decrypt(encrypted: Seq[Byte], privateKey: Seq[Byte]): Seq[Byte] =
    if (encrypted.length < MetaLength) throw new Exception("invalid ciphertext: data is too small")
    else if (encrypted.head != HeaderByte) throw new Exception("invalid ciphertext: wrong header")
    else {
      // Deserialize
      val ephemPublicKey = encrypted.slice(0, PublicKeyLength)
      val cipherTextLength = encrypted.length - MetaLength
      val IV = encrypted.slice(PublicKeyLength, PublicKeyLength + InitializationVectorLength)
      val cipherAndIV = encrypted.slice(PublicKeyLength, PublicKeyLength + InitializationVectorLength + cipherTextLength)
      val cipherText = cipherAndIV.slice(InitializationVectorLength, InitializationVectorLength + cipherTextLength)
      val msgMac = encrypted.slice(PublicKeyLength + InitializationVectorLength + cipherTextLength, encrypted.length)

      // Chech HMAC
      val sharedPx = derive(privateKey, ephemPublicKey)
      val hash = kdf(sharedPx, 32)
      val encryptionKey = hash.slice(0, 16)
      val macKey = MessageDigest.getInstance(hashAlgorithm).digest(hash.slice(16, hash.length).toArray)
      val currentHMAC = hmacSha256(macKey, cipherAndIV)
      if (!equalConstTime(currentHMAC, msgMac)) throw new Exception("Incorrect MAC")
      else {
        // Decrypt message
        aes128CtrDecrypt(IV, encryptionKey, cipherText)
      }
    }


  /**
   * Process AES-128-CTR encryption
   */
  private def aes128CtrEncrypt(iv: Seq[Byte], key: Seq[Byte], plainText: Seq[Byte]): Seq[Byte] = {
    val keySpec = new SecretKeySpec(key.toArray, "AES")
    val ivSpec = new IvParameterSpec(iv.toArray)
    val cipher = Cipher.getInstance(symmetricEncryptionName, providerName)
    cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
    cipher.update(plainText.toArray)
    iv ++ cipher.doFinal()
  }

  /**
   * Process AES-128-CTR decryption
   */
  private def aes128CtrDecrypt(iv: Seq[Byte], key: Seq[Byte], cipherText: Seq[Byte]): Seq[Byte] = {
    val keySpec = new SecretKeySpec(key.toArray, "AES")
    val ivSpec = new IvParameterSpec(iv.toArray)
    val cipher = Cipher.getInstance(symmetricEncryptionName, providerName)
    cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
    cipher.update(cipherText.toArray)
    cipher.doFinal()
  }

  /**
   * Compare two byte arrays in constant time to prevent timing attacks
   */
  private def equalConstTime(b1: Seq[Byte], b2: Seq[Byte]): Boolean =
    if (b1.length != b2.length) false
    else {
      var res = 0
      for (i <- 0 until b1.length) {
        res |= b1(i) ^ b2(i)
      }
      res == 0
    }

  /**
   * @return  the secp256k1 public key (65 bytes long) from the passed private key
   */
  private def getPublic(privateKey: Seq[Byte]): Seq[Byte] =
    if (privateKey.length != 32) throw new Exception("private key should be 32 bytes long")
    else {
      var privkey = new java.math.BigInteger(1, privateKey.toArray)
      val params = SECNamedCurves.getByName(curveName)
      val curve = new ECDomainParameters(params.getCurve, params.getG, params.getN, params.getH)
      val curveOrder = curve.getN
      if (privkey.bitLength > curveOrder.bitLength) {
        privkey = privkey.mod(curveOrder)
      }
      val point = new FixedPointCombMultiplier().multiply(curve.getG, privkey)
      Arrays.copyOfRange(point.getEncoded(false), 0, 65)
    }

  /**
   * Process HMAC-SHA256 message authentication
   */
  private def hmacSha256(key: Seq[Byte], msg: Seq[Byte]): Seq[Byte] = {
    val keySpec = new SecretKeySpec(key.toArray, hmacAlgorithm)
    val mac = Mac.getInstance(hmacAlgorithm)
    mac.init(keySpec)
    mac.doFinal(msg.toArray)
  }

  /**
   * Derive shared secret for given private and public keys.
   *
   * @param privateKey the sender's private key (32 bytes)
   * @param publicKey  the recipient's public key (65 bytes)
   * @return the derived shared secret (Px, 32 bytes)
   */
  private def derive(privateKey: Seq[Byte], publicKey: Seq[Byte]): Seq[Byte] =
    if (privateKey.length != 32) throw new Exception(s"bad private key length: it should be 32 bytes but it's actually ${privateKey.length} bytes long")
    else if (publicKey.length != 65) throw new Exception(s"bad public key length: it should be 65 bytes but it's actually ${publicKey.length} bytes long")
    else if (publicKey.head != HeaderByte) throw new Exception("bad public key header: a valid public key would begin with 0x04")
    else {
      val sk = privateKeyFrom(privateKey)
      val pk = publicKeyFrom(publicKey)
      val ka = KeyAgreement.getInstance("ECDH", providerName)
      ka.init(sk)
      ka.doPhase(pk, true)
      ka.generateSecret()
    }

  /**
   * Mimic Geth's implementation of KDF
   */
  private def kdf(secret: Seq[Byte], outputLength: Int): Seq[Byte] = {
    var ctr = 1
    var written = 0
    var result = new scala.collection.mutable.ArrayBuffer[Seq[Byte]]()
    val digest = MessageDigest.getInstance(hashAlgorithm)
    while (written < outputLength) {
      val ctrs = Array((ctr >> 24).toByte, (ctr >> 16).toByte, (ctr >> 8).toByte, ctr.toByte)
      digest.update(ctrs)
      digest.update(secret.toArray)
      val h = digest.digest
      result += h
      written += 32
      ctr += 1
      digest.reset()
    }
    result.flatten.toSeq
  }

  private def publicKeyFrom(bytes: Seq[Byte]): ECPublicKey = {
    val (kf, _, ecParamSpec) = init()
    val pk = PublicKey(bytes)
    val pubKeyAsPointW = new ECPoint(pk.x.bigInteger, pk.y.bigInteger)
    val pubSpec = new ECPublicKeySpec(pubKeyAsPointW, ecParamSpec)
    kf.generatePublic(pubSpec).asInstanceOf[ECPublicKey]
  }

  private final case class PublicKey(bytes: Seq[Byte]) {
    private lazy val _arr : Array[Byte] = {
      // Get rid of uncompressed header if need be
      if (bytes.length == PublicKeyLength && bytes.head == HeaderByte) {
        bytes.toArray.drop(1)
      } else bytes.toArray
    }
    private lazy val (_xBytes, _yBytes) = _arr.splitAt(32)

    lazy val x = BigInt(new java.math.BigInteger(1, _xBytes))
    lazy val y = BigInt(new java.math.BigInteger(1, _yBytes))
  }

  private def privateKeyFrom(bytes: Seq[Byte]): ECPrivateKey = {
    val (kf, _, ecParamSpec) = init()
    val sk = PrivateKey(bytes)
    val privSpec = new ECPrivateKeySpec(sk.s, ecParamSpec)
    kf.generatePrivate(privSpec).asInstanceOf[ECPrivateKey]
  }

  private final case class PrivateKey(bytes: Seq[Byte]) {
    lazy val s = new java.math.BigInteger(1, bytes.toArray)
  }
}
