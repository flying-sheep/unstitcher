package net.minecraft.util.ui

import java.io.File
import net.minecraft.util.Loggable
import net.minecraft.util.Unstitcher

object UnstitcherCli extends App with Loggable {
	if (List("-h", "--help").exists(args contains)) {
		help
		sys exit 0
	}
	args.length match {
		case 0 ⇒ UnstitcherGui()
		case 1 ⇒
			val input  = new File(args(0))
			val output = new File(input.getParentFile, "converted-" + input.getName)
			Unstitcher(input, output, this).run
		case 2 ⇒
			val Array(input, output) = args.map(new File(_))
			Unstitcher(input, output, this).run
		case n ⇒ help
	}
	
	def help = println("Usage: java -jar unstitcher.jar [-h | <input>.zip [<output>.zip]]")
	
	def log(text: String, args: Any*) = println(text format (args: _*))
}