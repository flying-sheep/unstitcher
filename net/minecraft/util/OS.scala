package net.minecraft.util

import java.io.File

sealed abstract trait OS
case object WINDOWS extends OS
case object MACOS   extends OS
case object SOLARIS extends OS
case object LINUX   extends OS
case object UNKNOWN extends OS

object OS {
	val platform = sys.props("os.name").toLowerCase match {
		case os if os contains "win"     ⇒ WINDOWS
		case os if os contains "mac"     ⇒ MACOS
		case os if os contains "solaris" ⇒ SOLARIS
		case os if os contains "sunos"   ⇒ SOLARIS
		case os if os contains "linux"   ⇒ LINUX
		case os if os contains "unix"    ⇒ LINUX
		case _ ⇒ UNKNOWN
	}
	
	val home = sys.props.getOrElse("user.home", ".")
	val minecraftDir = platform match {
		case LINUX | SOLARIS ⇒ new File(home, ".minecraft/")
		case WINDOWS ⇒ sys.env.get("APPDATA") match {
			case Some(a) ⇒ new File(a,    ".minecraft/")
			case None    ⇒ new File(home, ".minecraft/")
		}
		case MACOS ⇒ new File(home, "Library/Application Support/minecraft")
		case _     ⇒ new File(home, "minecraft/")
	}
}