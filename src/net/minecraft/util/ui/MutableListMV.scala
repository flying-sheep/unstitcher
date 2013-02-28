package net.minecraft.util.ui

import javax.swing._
import javax.swing.event.ListDataListener

import scala.swing._
import scala.collection.{ mutable, Traversable }

/** A ListModel wrapping a manipulatable Buffer
  * Implementation based on DefaultListModel for the sake of simplicity */
class BufferListModel[A] extends ListModel[A] with mutable.Buffer[A] {
	val lm = new DefaultListModel[A]
	
	def += (elem: A) = { Swing.onEDT(lm      addElement elem     ); this }
	def +=:(elem: A) = { Swing.onEDT(lm insertElementAt (elem, 0)); this }
	def insertAll(n: Int, elems: Traversable[A]) =
		for ((elem, i) ← elems.toIterator.zipWithIndex)
			lm insertElementAt (elem, n + i)
	
	def remove(n: Int) = lm remove n
	def clear() = lm.clear()
	
	def update(n: Int, elem: A) = Swing.onEDT(lm setElementAt (elem, n))
	
	def apply       (n: Int) = lm getElementAt n
	def getElementAt(n: Int) = lm getElementAt n
	
	def length  = lm.size
	def getSize = lm.size
	def iterator = Iterator.range(0, size).map(i ⇒ lm getElementAt i)

	def    addListDataListener(l: ListDataListener) = lm    addListDataListener l
	def removeListDataListener(l: ListDataListener) = lm removeListDataListener l
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