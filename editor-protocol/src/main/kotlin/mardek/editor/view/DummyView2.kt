package mardek.editor.view

import com.github.knokko.bitser.connection.StructConnectionView
import mardek.content.BITSER
import mardek.content.inventory.EquipmentProperties
import mardek.content.inventory.ItemType
import mardek.content.inventory.WeaponProperties

fun generateDummyView2(): StructConnectionView {

	val weaponView = StructConnectionView(BITSER.getProtocol(WeaponProperties::class.java))
	weaponView.markAllSimpleFields(true, true)
	weaponView.finishRegistration()

	val equipmentView = StructConnectionView(BITSER.getProtocol(EquipmentProperties::class.java))
	equipmentView.markChildStructField(null, "weapon", weaponView, true)
	equipmentView.finishRegistration()

	return equipmentView
}
