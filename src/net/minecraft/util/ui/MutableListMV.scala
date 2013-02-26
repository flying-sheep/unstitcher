package net.minecraft.util.ui

import javax.swing._
import javax.swing.event.ListDataListener

import scala.swing._

/** A ListModel wrapping a manipulatable Buffer
  * Implementation based on DefaultListModel for the sake of simplicity */
class BufferListModel[A] extends BufferWrapper[A] with ListModel[A] {
	val underlying = new DefaultListModel[A]
	
	def +=(a: A) = { Swing.onEDT(underlying addElement a); this }
	def insertAt(n: Int, item: A) = Swing.onEDT(underlying insertElementAt (item, n))
	
	def remove(n: Int): A = underlying remove n //TODO: onEDT
	
	def        apply(n: Int) = underlying getElementAt n
	def getElementAt(n: Int) = underlying getElementAt n
	
	def length  = underlying.size
	def getSize = underlying.size
	
	def    addListDataListener(l: ListDataListener) = underlying addListDataListener l
	def removeListDataListener(l: ListDataListener) = underlying removeListDataListener l
}

/** Version of ListView with a Buffer as listData.
  * allows to manipulate data there */
class MutableListView[A] extends ListView[A] {
	/** Java 7 has genericized many things
	  * so this makes the peer available under its true type */
	val typedPeer = peer.asInstanceOf[JList[A]]
	
	override val listData = new BufferListModel[A]
	typedPeer setModel listData
	
	override def listData_=(items: Seq[A]) = {
		listData.clear
		listData ++= items
	}
}