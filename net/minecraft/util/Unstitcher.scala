package net.minecraft.util

import java.awt.image.BufferedImage
import java.io._
import java.net.URL
import java.util.zip.{ ZipEntry, ZipFile, ZipOutputStream }
import javax.imageio.ImageIO
import scala.collection.JavaConversions._
import scala.io.Source

object Unstitcher {
	val SIDE = 16
	def apply(i: File, o: File, l: Loggable) = new Unstitcher(i: File, o: File, l: Loggable)
}

import Unstitcher.SIDE

class Unstitcher(inputFile: File, outputFile: File, log: Loggable) extends Runnable {
	def run = try {
		log.log("Selected texturepack '%s'", inputFile.getAbsolutePath)
		log.log("Output will be saved to '%s'", outputFile.getAbsolutePath)
		
		val input  = new ZipFile(inputFile)
		val result = new ZipOutputStream(new FileOutputStream(outputFile))
		
		log.log("Creating a copy of the texturepack...")
		
		for (entry ← input.entries if !entry.isDirectory) {
			val is = input getInputStream entry
			entry.getName match {
				case "terrain.png" ⇒
					log.log("Unstitching terrain.png")
					unstitchAll(is, result, "terrain", "blocks", "terrain.png")
				case "gui/items.png" ⇒
					log.log("Unstitching items.png")
					unstitchAll(is, result, "item", "items", "gui/items.png")
				case name ⇒
					log.log("Copying %s", entry.getName)
					result.putNextEntry(new ZipEntry(name))
					for (byte ← Iterator.continually(is.read).takeWhile(-1 !=))
						result.write(Array(byte.toByte))
					result.closeEntry
			}
		}
		
		log.log("All done!")
		log.log("Your items.png and terrain.png have been replaced with any images not cut from the image.")
		log.log("The unstitched images can be found in textures/blocks/*.png and textures/items/*.png respectively.")
		
		input.close
		result.close
	} catch { case t ⇒
		log.log("Error unstitching file!")
		log.log(t.getStackTraceString)
		sys.error(t.getStackTraceString)
		log.log("Stopping...")
	}
	
	def unstitchAll(input: InputStream,
		output: ZipOutputStream,
		typ: String,
		folder: String,
		original: String) {
		val posFile = classOf[Unstitcher].getResource("/%s.txt" format folder)
		val stitched = ImageIO read input
		for ((name, image) ← unstitch(stitched, posFile)) {
			log.log("Cutting out %s '%s'…", typ, name)
			output.putNextEntry(new ZipEntry("textures/%s/%s.png" format (folder, name)))
			ImageIO.write(image, "png", output)
			output.closeEntry
		}
		output.putNextEntry(new ZipEntry(original))
		ImageIO.write(stitched, "png", output)
		output.closeEntry
	}
	
	def unstitch(stitched: BufferedImage, pos: URL) = {
		val width  = stitched.getWidth  / SIDE
		val height = stitched.getHeight / SIDE
		
		def mkimg(xo: Int, yo: Int): BufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR) {
			val x0 = xo * width
			val y0 = yo * height
			
			for (x ← 0 until width; y ← 0 until height) {
				setRGB(x, y, stitched.getRGB(x0 + x, y0 + y))
				stitched.setRGB(x0 + x, y0 + y, 0)
			}
		}
		
		def parseLine(line: String) = {
			val Array(coords, name) = line.split(" - ", 2)
			val Array(x, y) = coords.split(",").map(_.toInt)
			(name, mkimg(x, y))
		}
		
		Source.fromURL(pos).getLines.map(parseLine)
	}
}
