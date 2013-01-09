package net.minecraft.util

/** declares a printf-like function used for logging.
  * defines apply as alias to it,
  * so it can be used as “log function” itself. */
trait Loggable {
	def   log(msg: String, args: Any*)
	def apply(msg: String, args: Any*) = log(msg, args: _*)
}