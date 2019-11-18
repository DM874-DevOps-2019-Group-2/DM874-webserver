package helper

import play.api.Logger

trait ClassLogger {
  val logger = Logger(this.getClass)
}
