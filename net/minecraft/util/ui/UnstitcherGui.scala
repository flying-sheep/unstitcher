package net.minecraft.util.ui

import java.awt.{ BorderLayout, Dimension }
import java.awt.event.{ WindowAdapter, WindowEvent }
import java.awt.image.BufferedImage
import java.io._
import java.util.Enumeration
import java.util.zip.{ ZipEntry, ZipFile, ZipOutputStream }

import javax.imageio.ImageIO
import javax.swing._
import javax.swing.filechooser.FileNameExtensionFilter

import scala.collection.JavaConversions._

import net.minecraft.util._

object UnstitcherGui {
	def apply() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName)
		} catch { case e ⇒ }
		new JFrame("Minecraft Texture Unstitcher") {
			add(new UnstitcherGui)
			pack
			setLocationRelativeTo(null) //center
			setVisible(true)
			setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
		}
	}
}

class UnstitcherGui extends JPanel with Loggable {
	private val log = new JTextArea {
		setEditable(false)
		setWrapStyleWord(true)
		setLineWrap(true)
	}
	setPreferredSize(new Dimension(670, 480))
	setLayout(new BorderLayout)
	add(new JScrollPane(log, 22, 31))
	
	def log(text: String, args: Any*) {
		log.append((text format (args: _*)) + "\n")
		log.setCaretPosition(log.getDocument.getLength)
	}
	
	new JFileChooser {
		log("Initialized. Please select a texturepack (zip only) for conversion. The output will be saved to the same directory in a separate zip.")
		setFileFilter(new FileNameExtensionFilter("Zip texture packs", "zip"))
		if (OS.minecraftDir.exists) {
			val dir = new File(OS.minecraftDir, "texturepacks")
			if (dir.isDirectory) setCurrentDirectory(dir)
		}
		if (showDialog(this, "Convert Texturepack") == 0 && getSelectedFile.isFile) {
			val input  = getSelectedFile
			val output = new File(input.getParentFile, "converted-" + input.getName)
			new Thread(Unstitcher(input, output, UnstitcherGui.this)).start
		} else {
			sys exit 1
		}
	}
}
