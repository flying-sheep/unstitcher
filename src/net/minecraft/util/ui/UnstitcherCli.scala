package net.minecraft.util.ui

import java.io.File
import net.minecraft.util.Loggable
import net.minecraft.util.Unstitcher

object UnstitcherCli extends App with Loggable {
	if (List("-h", "--help").exists(args contains)) {
		help
		sys exit 0
	}
	args match {
		case Array() ⇒ UnstitcherGui()
		case Array(inputPath) ⇒
			val input  = new File(inputPath)
			val output = new File(input.getParentFile, "converted-" + input.getName)
			Unstitcher(input, output, this).run
		case Array(inputPath, outputPath) ⇒
			Unstitcher(new File(inputPath), new File(outputPath), this).run
		case _ ⇒ help
	}
	
	def help = println("Usage: java -jar unstitcher.jar [-h | <input>.zip [<output>.zip]]")
	
	def log(text: String, args: Any*) = println(text format (args: _*))
}