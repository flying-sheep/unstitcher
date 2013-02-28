package net.minecraft.util.ui

import java.awt.FileDialog
import java.io._

import javax.swing.UIManager

import net.minecraft.util._
import scala.swing._

object UnstitcherGui {
	def apply(inputFile: Option[File] = None) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName)
		} catch { case e: Throwable ⇒ }
		
		new MainFrame {
			title = "Minecraft Texture Unstitcher"
			contents = new UnstitcherGui(inputFile)
			centerOnScreen()
		}.visible = true
	}
}

class UnstitcherGui(inputFile: Option[File] = None) extends ScrollPane with Loggable { gui ⇒
	private val logPanel = new MutableListView[String]
	contents = logPanel
	preferredSize = new Dimension(670, 480)
	
	verticalScrollBarPolicy   = ScrollPane.BarPolicy.AsNeeded
	horizontalScrollBarPolicy = ScrollPane.BarPolicy.Never
	
	def log(msg: String) = logPanel.listData += msg
	
	val dialog = new FileDialog(null.asInstanceOf[java.awt.Frame], "Convert Texturepack") {
		val _dir = inputFile getOrElse new File(OS.minecraftDir, "texturepacks")
		setDirectory(_dir.getCanonicalPath)
		log(html"Initialized. Please select a texturepack (zip only) for conversion.\nThe output will be saved to the same directory in a separate zip.")
		setFilenameFilter(new FilenameFilter {
			def accept(dir: File, name: String) = name endsWith ".zip"
		})
		setVisible(true)
	}
	
	Option(dialog.getFile) match {
		case Some(name) ⇒
			val input  = new File(dialog.getDirectory, name)
			val output = new File(dialog.getDirectory, s"converted-$name")
			new Thread(Unstitcher(input, output, UnstitcherGui.this)).start()
		case None ⇒ sys exit 1
	}
	
	dialog.dispose()
}
