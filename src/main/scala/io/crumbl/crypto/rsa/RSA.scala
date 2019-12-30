package io.crumbl.crypto.rsa

import io.crumbl.models.core.Base64
import java.security.spec.{MGF1ParameterSpec, PKCS8EncodedKeySpec, RSAPrivateCrtKeySpec, X509EncodedKeySpec}
import java.security.{KeyFactory, PrivateKey, PublicKey, Security}
import javax.crypto.Cipher
import javax.crypto.spec.{OAEPParameterSpec, PSource}
import sun.security.util.DerInputStream

/**
 * RSA class
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 1.0
 *
 * @see   [[http://magnus-k-karlsson.blogspot.com/2018/05/how-to-read-pem-pkcs1-or-pkcs8-encoded.html]]
 *        [[https://stackoverflow.com/questions/50298687/bouncy-castle-vs-java-default-rsa-with-oaep]]
 */
object RSA {
  Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider())

  // PKCS1
  val PKCS1PrivateHeader = "-----BEGIN RSA PRIVATE KEY-----"
  val PKCS1PrivateFooter = "-----END RSA PRIVATE KEY-----"
  val PKCS1PublicHeader = "-----BEGIN RSA PUBLIC KEY-----"
  val PKCS1PublicFooter = "-----END RSA PUBLIC KEY-----"
  // PKCS8
  val PKCS8PrivateHeader = "-----BEGIN PRIVATE KEY-----"
  val PKCS8PrivateFooter = "-----END PRIVATE KEY-----"
  val PKCS8PublicHeader = "-----BEGIN PUBLIC KEY-----"
  val PKCS8PublicFooter = "-----END PUBLIC KEY-----"

  val encryptionAlgorithm = "RSA"
  val providerName = "BC"
  val hashAlgorithm= "SHA-512"

  /**
   * Encrypts with passed message with the public key
   */
  def encrypt(msg: Seq[Byte], publicKey: String): Seq[Byte] = {
    val cipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA512AndMGF1Padding", providerName)
    val oaepParamSpec = new OAEPParameterSpec(hashAlgorithm, "MGF1", MGF1ParameterSpec.SHA512, PSource.PSpecified.DEFAULT)
    val pk = publicKeyFrom(publicKey)
    cipher.init(Cipher.ENCRYPT_MODE, pk, oaepParamSpec)
    cipher.doFinal(msg.toArray)
  }

  /**
   * Decrypts message using given private key
   */
  def decrypt(encrypted: Seq[Byte], privateKey: String): Seq[Byte] = {
    val cipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA512AndMGF1Padding", providerName)
    val oaepParamSpec = new OAEPParameterSpec(hashAlgorithm, "MGF1", MGF1ParameterSpec.SHA512, PSource.PSpecified.DEFAULT)
    val sk = privateKeyFrom(privateKey)
    cipher.init(Cipher.DECRYPT_MODE, sk, oaepParamSpec)
    cipher.doFinal(encrypted.toArray)
  }

  private def publicKeyFrom(publicKey: String): PublicKey = {
    val pkPEM = if (publicKey.startsWith(PKCS1PublicHeader)) {
      readPEM(publicKey, PKCS1PublicHeader, PKCS1PublicFooter)
    } else if (publicKey.startsWith(PKCS8PublicHeader)) {
      readPEM(publicKey, PKCS8PublicHeader, PKCS8PublicFooter)
    } else throw new Exception("invalid public key: wrong PEM data")
    val encoded = Base64(pkPEM).getBytes
    KeyFactory.getInstance(encryptionAlgorithm).generatePublic(new X509EncodedKeySpec(encoded.toArray))
  }

  private def privateKeyFrom(privateKey: String): PrivateKey = {
    val kf = KeyFactory.getInstance(encryptionAlgorithm)
    if (privateKey.startsWith(PKCS1PrivateHeader)) {
      val skPEM = readPEM(privateKey, PKCS1PrivateHeader, PKCS1PrivateFooter)
      val decoded = Base64(skPEM).getBytes
      val derReader = new DerInputStream(decoded.toArray)
      val seq = derReader.getSequence(0)
      val modulus = seq(1).getBigInteger
      val publicExp = seq(2).getBigInteger
      val privateExp = seq(3).getBigInteger
      val prime1 = seq(4).getBigInteger
      val prime2 = seq(5).getBigInteger
      val exp1 = seq(6).getBigInteger
      val exp2 = seq(7).getBigInteger
      val crtCoef = seq(8).getBigInteger
      val keySpec = new RSAPrivateCrtKeySpec(modulus, publicExp, privateExp, prime1, prime2, exp1, exp2, crtCoef)
      kf.generatePrivate(keySpec)
    } else if (privateKey.startsWith(PKCS8PrivateHeader)) {
       val skPEM = readPEM(privateKey, PKCS8PrivateHeader, PKCS8PrivateFooter)
      val decoded = Base64(skPEM).getBytes
      kf.generatePrivate(new PKCS8EncodedKeySpec(decoded.toArray))
    } else throw new Exception("invalid private key: wrong PEM data")
  }

  final private def readPEM(key: String, pkcsHeader: String, pkcsFooter: String): String = {
    key.replaceAll(pkcsHeader, "")
      .replaceAll(pkcsFooter, "")
      .replaceAll("\n+", "")
  }
}
