package mardek.importer.inventory

import mardek.content.inventory.ItemsContent
import mardek.content.inventory.WeaponType
import mardek.importer.area.parseFlashString
import mardek.importer.util.parseActionScriptObject
import java.lang.Integer.parseInt

internal fun importWeaponTypes(
	assets: ItemsContent, rawWeaponIDs: String, rawWeaponSounds: String
) {
	val weaponSounds = parseActionScriptObject(rawWeaponSounds)

	for (weaponName in parseActionScriptObject(rawWeaponIDs).entries.sortedBy { parseInt(it.value) }.map { it.key }) {
		val rawWeaponSound = weaponSounds[weaponName]
		assets.weaponTypes.add(WeaponType(
			flashName = weaponName,
			soundEffect = if (rawWeaponSound != null) parseFlashString(rawWeaponSound, "weapon sound") else null
		))
	}
}
