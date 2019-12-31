package io.crumbl

import io.crumbl.client._
import io.crumbl.utils.Logging

import CrumblWorker._

/**
 * Main application entry point for the executable
 */
object Main extends App with Logging {
  try {
    // Prepare operation
    val config = Config.init(args = args)
    val mode: CrumblMode = {
      if (config.c) CREATION
      else if (config.x) EXTRACTION
      else throw new Exception("invalid flag mode: use -c or -x")
    }
    if (!config.checks) throw new Exception("bad arguments: verify usage")

    // Process operation
    val worker = CrumblWorker(mode, config)
    worker.process(false)
  } catch {
    case e: Exception =>
      logger.severe(e.getMessage)
      e.getStackTrace foreach println
      println
      println(Config.getUsage)
      System.exit(1)
  }
}
