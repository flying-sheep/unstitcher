package net.minecraft.util.ui

import javax.swing._
import javax.swing.event.ListDataListener

import scala.swing._
import scala.collection.mutable
import scala.collection.Traversable

/** A ListModel wrapping a manipulatable Buffer
  * Implementation based on DefaultListModel for the sake of simplicity */
class BufferListModel[A] extends DefaultListModel[A] with mutable.Buffer[A] with mutable.IndexedSeqOptimized[A, BufferListModel[A]] {
	def += (elem: A) = { Swing.onEDT(     addElement(elem)   ); this }
	def +=:(elem: A) = { Swing.onEDT(insertElementAt(elem, 0)); this }
	def update(n: Int, elem: A) = Swing.onEDT(setElementAt(elem, n))
	
	def insertAll(n: Int, elems: Traversable[A]) =
		for ((elem, i) ← elems.toIterator.zipWithIndex)
			insertElementAt(elem, n + i)
	
	def apply(n: Int) = getElementAt(n)
	
	def length = size
}

/** Version of ListView with a Buffer as listData.
  * allows to manipulate data there */
class MutableListView[A] extends ListView[A] {
	/** Java 7 has genericized many things
	  * so this makes the peer available under its true type */
	val typedPeer = peer.asInstanceOf[JList[A]]
	
	override val listData = new BufferListModel[A]
	typedPeer setModel listData
	
	override def listData_=(items: Seq[A]) {
		listData.clear()
		listData ++= items
	}
}