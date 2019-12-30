package io.crumbl.client

import io.crumbl.core.{Crumbl, Uncrumbl}
import io.crumbl.crypto
import io.crumbl.decrypter.Uncrumb
import io.crumbl.models.core.Signer
import io.crumbl.utils.{Converter, Logging}
import java.nio.file.{Files, Paths}
import scala.io.Source
import scala.util.{Success, Using}

import CrumblWorker._

/**
 * CrumblWorker client class
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 1.0
 */
case class CrumblWorker(
  mode: CrumblMode,
  input: Option[String],
  output: Option[String],
  ownerKeys: Option[String],
  ownerSecret: Option[String],
  signerKeys: Option[String],
  signerSecret: Option[String],
  verificationHash: Option[String],
  var data: Seq[String]
) extends Logging {
  /**
   * Main method to get things done in the Crumbl&trade; system.
   *
   * @param returnResult  Pass `false` if you don't need the CrumblWorker to return the stringified result
   * @return the optional result (depending on the value of the above argument)
   */
  def process(returnResult: Boolean = true): Option[String] = {
    // Check mode
    if (mode != CREATION && mode != EXTRACTION) {
      val e = new Exception(s"invalid mode: ${mode}")
      if (!check(e, returnResult)) return None
    }

    // Build data if need be
    if (data.isEmpty) {
      if (input.isEmpty || input.getOrElse("").isEmpty) {
        val e = new Exception("invalid data: not enough arguments and/or no input file to use")
        if (!check(e, returnResult)) return None
      } else {
        input match {
          case Some(filename) if filename.nonEmpty =>
            // TODO Add multiple-line handling (using one crumbl per line in input file)
            Using(Source.fromFile(filename)) { content => content.mkString } match {
              case Success(src) if src.nonEmpty => data = src.split("\\s+")
              case _ =>
            }
          case _ =>
        }
      }
    } else {
      // Any data in an input file should be prepended to the data from the command-line arguments
      input match {
        case Some(filename) if filename.nonEmpty =>
          // In this case where there are arguments and an input file, there's no possible multiline handling
          val tmp = data
          Using(Source.fromFile(filename)) { content => content.mkString } match {
            case Success(src) if src.nonEmpty => data = src.split("\\s+") ++ tmp
            case _ =>
          }
        case _ =>
      }
    }

    // Get algorithm and keys
    val ownersMap = fillMap(ownerKeys)
    val signersMap = fillMap(signerKeys)
    if (ownersMap.isEmpty && (mode == CREATION || (mode == EXTRACTION && signersMap.isEmpty))) {
      val e = new Exception("missing public key for the data owner")
      if (!check(e, returnResult)) return None
    }
    if (signersMap.isEmpty && (mode == CREATION || (mode == EXTRACTION && ownersMap.isEmpty))) {
      val e = new Exception("missing public keys for trusted signers")
      if (!check(e, returnResult)) return None
    }

    // Check data
    if (mode == EXTRACTION && (verificationHash.isEmpty || verificationHash.getOrElse("").isEmpty)) {
      logger.warning("verification hash is missing")
    }
    if (data.isEmpty) {
      val e = new Exception("no data to use")
      if (!check(e, returnResult)) return None
    }

    if (mode == CREATION) {
      val owners = buildSigners(ownersMap)
      val trustees = buildSigners(signersMap)

      val crumbl = Crumbl(data.head, crypto.DEFAULT_HASH_ENGINE, owners, trustees)
      output match {
        case Some(filename) if filename.nonEmpty => {
          val res = crumbl.toFile(filename)
          if (returnResult) Some(res)
          else None
        }
        case _ => {
          val res = crumbl.toStdOut()
          if (returnResult) Some(res)
          else None
        }
      }
    } else if (mode == EXTRACTION) {
      val (user, isOwner) = ownerSecret match {
        case Some(oSecret) if fileExists(oSecret) => {
          if (ownersMap.size != 1) {
            val e = new Exception("too many public keys for a data owner")
            if (!check(e, returnResult)) return None
          }
          val sk = Using(Source.fromFile(oSecret)) { source => source.mkString } match {
            case Success(src) if src.nonEmpty => src
            case _ => throw new Exception("invalid empty private key for the data owner")
          }
          val algo = ownersMap.head._2
          val privkey = algo match {
            case crypto.ECIES_ALGORITHM => Some(Right(Converter.hexToBytes(sk)))
            case crypto.RSA_ALGORITHM => Some(Left(sk))
            case _ => None
          }
          if (privkey.isDefined) {
            (Signer(algo, privkey, None), true)
          } else throw new Exception("invalid empty private key for the data owner")
        }
        case _ => signerSecret match {
          case Some(sSecret) if fileExists(sSecret) => {
            if (signersMap.size != 1) {
              val e = new Exception("too many public keys for a single uncrumbler")
              if (!check(e, returnResult)) return None
            }
            val sk = Using(Source.fromFile(sSecret)) { source => source.mkString } match {
              case Success(src) if src.nonEmpty => src
              case _ => throw new Exception("invalid empty private key for the trusted third-party")
            }
            val algo = signersMap.head._2
            val privkey = algo match {
              case crypto.ECIES_ALGORITHM => Some(Right(Converter.hexToBytes(sk)))
              case crypto.RSA_ALGORITHM => Some(Left(sk))
              case _ => None
            }
            if (privkey.isDefined) {
              (Signer(algo, privkey, None), false)
            } else throw new Exception("invalid empty private key for the trusted third-party")
          }
          case _ =>
            val e = new Exception("invalid keys: no signer was detected")
            if (!check(e, returnResult)) return None
            else (Signer.EMPTY, false)
        }
      }
      if (!user.isEmpty) {
        // TODO Add multiple-line handling (using one crumbl per line in input file)
        val uncrumbs = new scala.collection.mutable.ArrayBuffer[Uncrumb]()
        if (data.size > 1) {
          data.drop(1).foreach(u => {
            val parts = u.split("\\.", 2)
            if (parts(1) != Crumbl.VERSION) {
              logger.warning(s"wrong version for uncrumb: ${u}")
            } else {
              val vh = parts(0).substring(0, crypto.DEFAULT_HASH_LENGTH)
              if (vh == verificationHash.getOrElse("")) {
                val us = parts(0).substring(crypto.DEFAULT_HASH_LENGTH)
                us.split(Uncrumb.PARTIAL_PREFIX).foreach(unc => if (unc.nonEmpty) {
                  val uncrumb = Uncrumb.toUncrumb(unc)
                  uncrumbs += uncrumb
                })
              }
            }
          })
        }
        val uncrumbl = Uncrumbl(data.head, if (uncrumbs.nonEmpty) Some(uncrumbs.toSeq) else None, verificationHash, user, isOwner)
        output match {
          case Some(filename) if filename.nonEmpty => {
            val res = uncrumbl.toFile(filename)
            if (returnResult) Some(Converter.bytesToString(res))
            else None
          }
          case _ => {
            val res = uncrumbl.toStdOut()
            if (returnResult) Some(Converter.bytesToString(res))
            else None
          }
        }
      } else {
        None
      }
    } else {
      None
    }
  }

  private def buildSigners(withMap: Map[String, String]): Seq[Signer] = {
    withMap.map { case (pk, algo) => {
      val pubkey = algo match {
        case crypto.ECIES_ALGORITHM => Some(Right(Converter.hexToBytes(pk)))
        case crypto.RSA_ALGORITHM => Some(Left(pk))
        case _ => None
      }
      Signer(algo, None, pubkey)
    }}
  }.toSeq

  private def check(e: Exception, returnResult: Boolean): Boolean = {
    if (e.getMessage.nonEmpty) {
      logger.throwing(getClass.getName, Thread.currentThread.getStackTrace()(2).getMethodName, e)
      if (returnResult) {
        return false
      } else {
        println(Config.getUsage)
        System.exit(1)
      }
    }
    true
  }

  private def fileExists(path: String): Boolean = Files.exists(Paths.get(path))

  private def fillMap(ownerKeys: Option[String]): Map[String, String] = {
    val theMap = scala.collection.mutable.Map[String, String]()
    ownerKeys match {
      case Some(owks) => owks.split(",").foreach(tuple => if (tuple.nonEmpty) {
        val parts = tuple.split(":")
        if (parts.length == 2) {
          val algo = parts(0)
          val path = parts(1)
          if (path.nonEmpty && fileExists(path)) {
            Using(Source.fromFile(path)) { content => content.mkString } match {
              case Success(key) => if (crypto.existsAlgorithm(algo)) {
                theMap += key -> algo
              } else {
                logger.warning(s"invalid encryption algorithm in ${tuple}")
              }
              case _ =>
            }
          } else {
            logger.warning(s"invalid file path in ${tuple}")
          }
        }
      })
      case None =>
    }
    theMap.toMap
  }
}
object CrumblWorker {
  val CREATION: CrumblMode = "crumbl"
  val EXTRACTION: CrumblMode = "uncrumbl"

  final type CrumblMode = String

  /**
   * Creates a new CrumblWorker from Config
   */
  def apply(mode: CrumblMode, config: Config): CrumblWorker = CrumblWorker(
    mode,
    config.in,
    config.out,
    config.ownerKeys,
    config.ownerSecret,
    config.signerKeys,
    config.signerSecret,
    config.verificationHash,
    config.data
  )
}
