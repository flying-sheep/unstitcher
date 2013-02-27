package net.minecraft.util

/** declares a printf-like function used for logging.
  * defines apply as alias to it,
  * so it can be used as “log function” itself. */
trait Loggable {
	def   log(msg: String)
	def apply(msg: String) = log(msg)
}