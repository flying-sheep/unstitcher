package net.minecraft.util

import java.io.File

sealed trait OS

object OS {
	case object Windows extends OS
	case object OSX     extends OS
	case object Solaris extends OS
	case object Linux   extends OS
	case object Unknown extends OS
	
	val platform = sys.props("os.name").toLowerCase match {
		case os if os contains "win"     ⇒ Windows
		case os if os contains "mac"     ⇒ OSX
		case os if os contains "solaris" ⇒ Solaris
		case os if os contains "sunos"   ⇒ Solaris
		case os if os contains "linux"   ⇒ Linux
		case os if os contains "unix"    ⇒ Linux
		case _ ⇒ Unknown
	}
	
	val home = sys.props.getOrElse("user.home", ".")
	val minecraftDir = platform match {
		case Linux | Solaris ⇒ new File(home, ".minecraft/")
		case Windows ⇒ sys.env.get("APPDATA") match {
			case Some(a) ⇒ new File(a,    ".minecraft/")
			case None    ⇒ new File(home, ".minecraft/")
		}
		case OSX ⇒ new File(home, "Library/Application Support/minecraft")
		case _   ⇒ new File(home, "minecraft/")
	}
}