package net.minecraft.util

import java.awt.image.BufferedImage
import java.io._
import java.net.URL
import java.util.zip.{ ZipEntry, ZipFile, ZipOutputStream }
import javax.imageio.ImageIO
import scala.collection.JavaConversions._
import scala.io.Source
import scala.collection.mutable.{ MultiMap, HashMap, Set }

object Unstitcher {
	val SIDE = 16
	def apply(i: File, o: File, l: Loggable) = new Unstitcher(i: File, o: File, l: Loggable)
	implicit def asMultiMap[K, V](pairs: Iterator[(K, V)]): MultiMap[K, V] =
		new HashMap[K, Set[V]] with MultiMap[K, V] {
			for ((k, v) ← pairs) this.addBinding(k, v)
		}
	
	def parseMapping(pos: URL): MultiMap[(Int, Int), String] =
		Source.fromURL(pos).getLines.map { line ⇒
			val Array(coords, name) = line.split(" - ", 2)
			val Array(x, y) = coords.split(",").map(_.toInt)
			(x, y) → name
		}
	
	object StitchInfo { val values = Set(blocks, items) }
	class StitchInfo(val typ: String, val folder: String, val original: String) {
		val map = parseMapping(classOf[Unstitcher].getResource("/%s.txt" format folder))
	}
	val blocks = new StitchInfo("terrain", "blocks", "terrain.png")
	val items  = new StitchInfo("item",    "items",  "gui/items.png")
}

import Unstitcher._

class Unstitcher(inputFile: File, outputFile: File, log: Loggable) extends Runnable {
	def run = try {
		log("Selected texturepack '%s'", inputFile.getAbsolutePath)
		log("Output will be saved to '%s'", outputFile.getAbsolutePath)
		
		val input  = new ZipFile(inputFile)
		val result = new ZipOutputStream(new FileOutputStream(outputFile))
		
		log("Creating a copy of the texturepack...")
		
		for (entry ← input.entries if !entry.isDirectory) {
			val is = input getInputStream entry
			entry.getName match {
				case original if StitchInfo.values map (_.original) contains original ⇒
					val info = StitchInfo.values find (_.original == original) get
					
					log("Unstitching %s...", info.typ)
					unstitchAll(ImageIO read is, result, info)
				case anim if anim startsWith "anim/custom_" ⇒
					log("Animation '%s' detected", anim)
					handleAnim(is, result, anim)
				case name ⇒
					log("Copying %s", entry.getName)
					result putNextEntry new ZipEntry(name)
					for (byte ← Iterator.continually(is.read).takeWhile(-1 !=))
						result write Array(byte toByte)
					result.closeEntry
			}
		}
		
		log("All done!")
		log("Your items.png and terrain.png have been replaced with any images not cut from the image.")
		log("The unstitched images can be found in textures/blocks/*.png and textures/items/*.png respectively.")
		
		input.close
		result.close
	} catch { case t ⇒
		log("Error unstitching file!")
		log("%s\n%s", t, t.getStackTraceString)
		sys.error(t + "\n" + t.getStackTraceString)
		log("Stopping...")
	}
	
	def handleAnim(input: InputStream, output: ZipOutputStream, animPath: String) {
		//TODO: real handling of lava and water
		val animR = """^anim/custom_(.+)\.png$""".r
		val animR(anim) = animPath
		
		val lavaWaterR = """^(lava|water)_flowing$""".r
		val terrainItemR = """^(terrain|item)_(\d+)$""".r
		
		val target = anim match {
			case lavaWaterR(lavaWater) ⇒
				"textures/blocks/%s_flow.png" format (lavaWater)
			case terrainItemR(terrainItem, number) ⇒ terrainItem match {
				case "terrain" ⇒ "textures/blocks/%s.png"
				case "item"    ⇒ "textures/items/%s.png"
				//TODO utilize parsemapping here
			}
			case _ ⇒
				log("failed to find correct place for %s animation.", animPath)
				animPath
		}
		log("Copying %s to %s", animPath, target)
		output.putNextEntry(new ZipEntry(target))
		for (byte ← Iterator.continually(input.read).takeWhile(-1 !=))
			output.write(Array(byte.toByte))
		output.closeEntry
	}
	
	/** Unstitches one spritesheet */
	def unstitchAll(stitched: BufferedImage, output: ZipOutputStream, info: StitchInfo) {
		for {
			(names, mkimage) ← unstitch(stitched, info.map)
			image = mkimage()
			name ← names
		} {
			log("Cutting out %s '%s'…", info.typ, name)
			output.putNextEntry(new ZipEntry("textures/%s/%s.png" format (info.folder, name)))
			ImageIO.write(image, "png", output)
			output.closeEntry
		}
		//output.putNextEntry(new ZipEntry(info.original))
		//ImageIO.write(stitched, "png", output)
		//output.closeEntry
	}
	
	def mkimg(stitched: BufferedImage, xo: Int, yo: Int, w: Int, h: Int): BufferedImage =
		new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR) {
			val x0 = xo * w
			val y0 = yo * h
			
			for (x ← 0 until w; y ← 0 until h) {
			//	setRGB(x, y, stitched.getRGB(x0 + x, y0 + y))
				stitched.setRGB(x0 + x, y0 + y, 0)
			}
		}
	
	/** Creates an iterator over all tiles in the input images
	  * which have a mapping in the positions file. */
	def unstitch(stitched: BufferedImage, map: MultiMap[(Int, Int), String]) = {
		val width  = stitched.getWidth  / SIDE
		val height = stitched.getHeight / SIDE
		
		map map { case ((x, y), names) ⇒ (names, () ⇒ mkimg(stitched, x, y, width, height)) }
	}
}
