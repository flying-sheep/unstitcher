package net.minecraft.util

import java.awt.image.BufferedImage
import java.io._
import java.net.URL
import java.util.zip._

import javax.imageio.ImageIO

import scala.collection.JavaConversions._
import scala.io.Source
import scala.collection.mutable

object Unstitcher {
	val SIDE = 16
	def apply(i: File, o: File, l: Loggable) = new Unstitcher(i: File, o: File, l: Loggable)
	class MultiMap[K, V](pairs: Iterator[(K, V)])
			extends mutable.HashMap[K, mutable.Set[V]] with mutable.MultiMap[K, V] {
		for ((k, v) ← pairs) this.addBinding(k, v)
	}
	
	def parseMapping(pos: URL) = new MultiMap(
		Source.fromURL(pos).getLines() map { line ⇒
			val Array(coords, name) = line.split(" - ", 2)
			val Array(x, y) = coords.split(",").map(_.toInt)
			(x, y) → name
		})
	
	object StitchInfo {
		val values     = Set(Blocks, Items)
		val byType     = (values map { i ⇒ i.typ      → i }).toMap
		val byFolder   = (values map { i ⇒ i.folder   → i }).toMap
		val byOriginal = (values map { i ⇒ i.original → i }).toMap
	}
	case class StitchInfo(typ: String, folder: String, original: String) {
		val map = parseMapping(classOf[Unstitcher].getResource(s"/$folder.txt"))
	}
	val Blocks = StitchInfo("terrain", "blocks", "terrain.png")
	val Items  = StitchInfo("item",    "items",  "gui/items.png")
}

import net.minecraft.util.Unstitcher.{ StitchInfo, SIDE, Blocks, Items }

class Unstitcher(inputFile: File, outputFile: File, log: Loggable) extends Runnable {
	def run() = try {
		log(s"Selected texturepack '${inputFile.getAbsolutePath}'")
		log(s"Output will be saved to '${inputFile.getAbsolutePath}'")
		
		val input  = new ZipFile(inputFile)
		val result = new ZipOutputStream(new FileOutputStream(outputFile))
		
		log("Creating a copy of the texturepack…")
		
		val (anims, nonAnims) = input.entries
			.filter(!_.isDirectory)
			.map(entry ⇒ (entry.getName, entry))
			.partition(_._1 startsWith "anim/custom_")
		
		//handle just animations first to ensure
		//that those end up in the pack
		for ((name, anim) ← anims)
			handleAnim(input getInputStream anim, result, name)
		
		for ((name, entry) ← nonAnims) {
			val is = input getInputStream entry
			StitchInfo.byOriginal get name match {
				case Some(info) ⇒
					log(s"Unstitching ${info.typ}…")
					unstitchAll(ImageIO read is, result, info)
				case None ⇒
					log(s"Copying $name")
					try {
						result putNextEntry new ZipEntry(name)
						for (byte ← Iterator continually is.read takeWhile (-1 !=))
							result write Array(byte toByte)
						result.closeEntry()
					} catch { case _: ZipException ⇒
						log(s"Animation with name $name exists: not copying this")
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
		log(   html"$t\n${t.getStackTraceString}")
		sys error s"$t\n${t.getStackTraceString}"
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
				s"textures/blocks/${lavaWater}_flow.png" //TODO 32×32
			case terrainItemR(typ, number) ⇒
				val num = number.toInt
				val xy = (num % SIDE, num / SIDE)
				
				val info = StitchInfo byType typ
				val name = info.map(xy).head
				s"textures/${info.folder}/$name.png"
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
				try {
					output putNextEntry new ZipEntry(s"textures/${info.folder}/$name.png")
					ImageIO write (image, "png", output)
					output.closeEntry()
				} catch { case _: ZipException ⇒
					log(s"Animation with name $name exists: not copying block")
				}
			}
		}
	}
	
	def mkimg(stitched: BufferedImage, x0: Int, y0: Int, w: Int, h: Int) =
		new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR) {
			for (x ← 0 until w; y ← 0 until h) 
				setRGB(x, y, stitched.getRGB(x0*w + x, y0*h + y))
		}
}
