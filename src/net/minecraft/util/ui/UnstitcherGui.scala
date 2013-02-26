package net.minecraft.util.ui

import java.awt.Dimension
import java.io._

import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter

import scala.collection.JavaConversions._

import net.minecraft.util._
import scala.swing._

object UnstitcherGui {
	def apply(inputFile: Option[File] = None) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName)
		} catch { case e: Throwable ⇒ }
		
		new Frame {
			title = "Minecraft Texture Unstitcher"
			contents = new UnstitcherGui(inputFile)
			centerOnScreen
			pack
			visible = true

			override def closeOperation = dispose
		}
	}
}

class UnstitcherGui(inputFile: Option[File] = None) extends ScrollPane with Loggable { gui ⇒
	private val logPanel = new MutableListView[String]
	contents = logPanel
	preferredSize = new Dimension(670, 480)
	
	verticalScrollBarPolicy   = ScrollPane.BarPolicy.AsNeeded
	horizontalScrollBarPolicy = ScrollPane.BarPolicy.Never
	
	def log(text: String, args: Any*) = logPanel.listData += (text format (args: _*)) + "\n"
	
	new FileChooser(inputFile getOrElse new File(OS.minecraftDir, "texturepacks")) {
		log("Initialized. Please select a texturepack (zip only) for conversion. The output will be saved to the same directory in a separate zip.")
		fileFilter = new FileNameExtensionFilter("Zip texture packs", "zip")
		
		if (showDialog(gui, "Convert Texturepack") == FileChooser.Result.Approve && selectedFile.isFile) {
			val output = new File(selectedFile.getParentFile, "converted-" + selectedFile.getName)
			new Thread(Unstitcher(selectedFile, output, UnstitcherGui.this)).start
		} else {
			sys exit 1
		}
	}
}
