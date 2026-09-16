package mardek.editor.view

import com.github.knokko.bitser.connection.StructConnectionView
import mardek.content.BITSER
import mardek.content.inventory.ItemType

fun generateDummyView1(): StructConnectionView {
	val view = StructConnectionView(BITSER.getProtocol(ItemType::class.java))
	view.markAllSimpleFields(true, true)
	view.finishRegistration()
	return view
}
