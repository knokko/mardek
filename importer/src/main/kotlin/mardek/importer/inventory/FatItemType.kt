package mardek.importer.inventory

import mardek.content.audio.SoundEffect

class FatItemType(
	val flashName: String,
	val displayName: String,
	val color: Int,
	val sheetName: String,
	val sheetRow: Int,
	val soundEffect: SoundEffect? = null,
) {
	override fun toString() = displayName
}