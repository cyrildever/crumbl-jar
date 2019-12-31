package io.crumbl

import java.security.MessageDigest

package object crypto {
  val DEFAULT_HASH_ENGINE = "sha-256"
  val DEFAULT_HASH_LENGTH = 64

  val ECIES_ALGORITHM = "ecies"
  val RSA_ALGORITHM = "rsa"
  val authorizedAlgorithms = List(ECIES_ALGORITHM, RSA_ALGORITHM)

  def existsAlgorithm(name: String): Boolean = authorizedAlgorithms.contains(name)

  /**
   * Hashes the passed byte array using the passed hash engine,
   * ie. only SHA-256 hash algorithm as of the latest version of the Crumbl&trade;
   */
  def hash(input: Seq[Byte], engine: String): Seq[Byte] = if (engine == DEFAULT_HASH_ENGINE) {
    val digest = MessageDigest.getInstance(engine)
    digest.digest(input.toArray)
  } else throw new Exception("invalid hash engine")
}
