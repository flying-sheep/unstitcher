package net.minecraft.util

import java.awt.image.BufferedImage
import java.io._
import java.net.URL
import java.util.zip.{ ZipEntry, ZipFile, ZipOutputStream }
import javax.imageio.ImageIO
import scala.collection.JavaConversions._
import scala.io.Source
import scala.collection.mutable

object Unstitcher {
	val SIDE = 16
	def apply(i: File, o: File, l: Loggable) = new Unstitcher(i: File, o: File, l: Loggable)
	implicit def asMultiMap[K, V](pairs: Iterator[(K, V)]): mutable.MultiMap[K, V] =
		new mutable.HashMap[K, Set[V]] with mutable.MultiMap[K, V] {
			for ((k, v) ← pairs) this.addBinding(k, v)
		}
	
	def parseMapping(pos: URL): mutable.MultiMap[(Int, Int), String] =
		Source.fromURL(pos).getLines() map { line ⇒
			val Array(coords, name) = line.split(" - ", 2)
			val Array(x, y) = coords.split(",").map(_.toInt)
			(x, y) → name
		}
	
	object StitchInfo { val values = (Set(Blocks, Items) map { i ⇒ i.original → i }).toMap }
	class StitchInfo(val typ: String, val folder: String, val original: String) {
		val map = parseMapping(classOf[Unstitcher].getResource(s"/$folder.txt"))
	}
	val Blocks = new StitchInfo("terrain", "blocks", "terrain.png")
	val Items  = new StitchInfo("item",    "items",  "gui/items.png")
}

import net.minecraft.util.Unstitcher.{ StitchInfo, SIDE }

class Unstitcher(inputFile: File, outputFile: File, log: Loggable) extends Runnable {
	def run() = try {
		log(s"Selected texturepack '${inputFile.getAbsolutePath}'")
		log(s"Output will be saved to '${inputFile.getAbsolutePath}'")
		
		val input  = new ZipFile(inputFile)
		val result = new ZipOutputStream(new FileOutputStream(outputFile))
		
		log("Creating a copy of the texturepack...")
		
		for (entry ← input.entries if !entry.isDirectory) {
			val is = input getInputStream entry
			entry.getName match {
				case anim if anim startsWith "anim/custom_" ⇒
					log(s"Animation '$anim' detected")
					handleAnim(is, result, anim)
				case name ⇒ StitchInfo.values.get(name) match {
					case Some(info) ⇒
						log(s"Unstitching ${info.typ}…")
						unstitchAll(ImageIO read is, result, info)
					case None =>
						log(s"Copying $name")
						result putNextEntry new ZipEntry(name)
						for (byte ← Iterator.continually(is.read).takeWhile(-1 !=))
							result write Array(byte toByte)
						result.closeEntry()
				}
			}
		}
		
		log("All done!")
		log("Your items.png and terrain.png have been replaced with any images not cut from the image.")
		log("The unstitched images can be found in textures/blocks/*.png and textures/items/*.png respectively.")
		
		input.close()
		result.close()
	} catch { case t: Throwable ⇒
		log("Error unstitching file!")
		val msg = s"$t\n${t.getStackTraceString}"
		log(msg)
		sys.error(msg)
		log("Stopping…")
	}
	
	def handleAnim(input: InputStream, output: ZipOutputStream, animPath: String) {
		//TODO: real handling of lava and water
		val animR = """^anim/custom_(.+)\.png$""".r
		val animR(anim) = animPath
		
		val lavaWaterR = """^(lava|water)_flowing$""".r
		val terrainItemR = """^(terrain|item)_(\d+)$""".r
		
		val target = anim match {
			case lavaWaterR(lavaWater) ⇒
				s"textures/blocks/${lavaWater}_flow.png"
			case terrainItemR(terrainItem, number) ⇒ terrainItem match {
				case "terrain" ⇒ "textures/blocks/%s.png"
				case "item"    ⇒ "textures/items/%s.png"
				//TODO utilize parsemapping here
			}
			case _ ⇒
				log(s"failed to find correct place for $animPath animation.")
				animPath
		}
		log(s"Copying $animPath to $target")
		output putNextEntry new ZipEntry(target)
		for (byte ← Iterator.continually(input.read).takeWhile(-1 !=))
			output.write(Array(byte.toByte))
		output.closeEntry()
	}
	
	/** Unstitches one spritesheet */
	def unstitchAll(stitched: BufferedImage, output: ZipOutputStream, info: StitchInfo) {
		for (((x, y), names) ← info.map) {
			val width  = stitched.getWidth  / SIDE
			val height = stitched.getHeight / SIDE
			
			val image = mkimg(stitched, x, y, width, height)
			
			for (name ← names) {
				log(s"Cutting out ${info.typ} '$name'…")
				output putNextEntry new ZipEntry(s"textures/${info.folder}/$name.png")
				ImageIO write (image, "png", output)
				output.closeEntry()
			}
		}
	}
	
	def mkimg(stitched: BufferedImage, x0: Int, y0: Int, w: Int, h: Int): BufferedImage =
		new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR) {
			for (x ← 0 until w; y ← 0 until h) 
				setRGB(x, y, stitched.getRGB(x0*w + x, y0*h + y))
		}
}
