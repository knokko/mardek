package mardek.importer.inventory

import mardek.assets.inventory.InventoryAssets
import mardek.assets.inventory.WeaponType
import mardek.importer.area.parseFlashString
import mardek.importer.util.parseActionScriptObject

fun importWeaponTypes(assets: InventoryAssets, rawWeaponIDs: String, rawWeaponSounds: String) {
	val weaponSounds = parseActionScriptObject(rawWeaponSounds)

	for (weaponName in parseActionScriptObject(rawWeaponIDs).keys) {
		val rawWeaponSound = weaponSounds[weaponName]
		assets.weaponTypes.add(WeaponType(
			flashName = weaponName,
			soundEffect = if (rawWeaponSound != null) parseFlashString(rawWeaponSound, "weapon sound") else null
		))
	}
}
