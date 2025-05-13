package mardek.importer.inventory

import mardek.content.Content
import mardek.content.inventory.WeaponType
import mardek.importer.area.parseFlashString
import mardek.importer.util.parseActionScriptObject
import java.lang.Integer.parseInt

internal fun importWeaponTypes(
	content: Content, rawWeaponIDs: String, rawWeaponSounds: String
) {
	val weaponSounds = parseActionScriptObject(rawWeaponSounds)

	for (weaponName in parseActionScriptObject(rawWeaponIDs).entries.sortedBy { parseInt(it.value) }.map { it.key }) {
		val rawWeaponSound = weaponSounds[weaponName]
		val defaultHitSound = if (rawWeaponSound != null) {
			val soundName = parseFlashString(rawWeaponSound, "weapon sound")!!
			content.audio.effects.find { it.flashName == "hit_$soundName" }!!
		} else null
		content.items.weaponTypes.add(WeaponType(flashName = weaponName, soundEffect = defaultHitSound))
	}
}
