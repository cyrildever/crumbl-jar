package io.crumbl.utils

import java.util.logging.Logger

/**
 * Logging trait
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 1.0
 */
trait Logging { self =>
  val logger: Logger = Logger.getLogger(self.getClass.getCanonicalName)
}
