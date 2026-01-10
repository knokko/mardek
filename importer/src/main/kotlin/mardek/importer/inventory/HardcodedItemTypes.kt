package mardek.importer.inventory

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import mardek.content.Content
import mardek.content.inventory.ItemType
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

private val BASE_ITEM_COLOR = rgb(193, 145, 89)
private val CONSUMABLE_ITEM_COLOR = rgb(81, 113, 217)
private val WEAPON_COLOR = rgb(224, 128, 80)
private val ARMOR_COLOR = rgb(145, 209, 89)
private val ACCESSORY_COLOR = rgb(209, 209, 89)

internal fun sheet(type: String): BufferedImage {
	val input = ItemParseException::class.java.getResourceAsStream("itemsheet_$type.png")
		?: throw ItemParseException("Can't find sheet $type")
	val sheet = ImageIO.read(input)
	input.close()
	return sheet
}

internal fun hardcodeItemTypes(content: Content): List<FatItemType> {
	val result = mutableListOf<FatItemType>()
	result.add(FatItemType(
		flashName = "accs",
		displayName = "ACCESSORY",
		color = ACCESSORY_COLOR,
		sheetName = "misc",
		sheetRow = 0,
	))
	result.add(FatItemType(
		flashName = "invn",
		displayName = "INVENTION",
		color = WEAPON_COLOR,
		sheetName = "misc",
		sheetRow = 1,
	))
	result.add(FatItemType(
		flashName = "item",
		displayName = "EXPENDABLE ITEM",
		color = CONSUMABLE_ITEM_COLOR,
		sheetName = "misc",
		sheetRow = 2,
	))
	result.add(FatItemType(
		flashName = "gems",
		displayName = "GEMSTONE",
		color = ACCESSORY_COLOR,
		sheetName = "misc",
		sheetRow = 3,
	))
	result.add(FatItemType(
		flashName = "misc",
		displayName = "MISCELLANEOUS ITEM",
		color = BASE_ITEM_COLOR,
		sheetName = "misc",
		sheetRow = 5,
	))
	result.add(FatItemType(
		flashName = "song",
		displayName = "MUSIC SHEET",
		color = ARMOR_COLOR,
		sheetName = "misc",
		sheetRow = 6,
	))
	result.add(FatItemType(
		flashName = "Sh",
		displayName = "SHIELD",
		color = ARMOR_COLOR,
		sheetName = "armour",
		sheetRow = 0
	))
	result.add(FatItemType(
		flashName = "Ar0",
		displayName = "ARMOUR: CLOTHING",
		color = ARMOR_COLOR,
		sheetName = "armour",
		sheetRow = 1
	))
	result.add(FatItemType(
		flashName = "Ar1",
		displayName = "ARMOUR: LIGHT",
		color = ARMOR_COLOR,
		sheetName = "armour",
		sheetRow = 2
	))
	result.add(FatItemType(
		flashName = "Ar2",
		displayName = "ARMOUR: MEDIUM",
		color = ARMOR_COLOR,
		sheetName = "armour",
		sheetRow = 3
	))
	result.add(FatItemType(
		flashName = "Ar3",
		displayName = "ARMOUR: HEAVY",
		color = ARMOR_COLOR,
		sheetName = "armour",
		sheetRow = 4
	))
	result.add(FatItemType(
		flashName = "ArR",
		displayName = "ARMOUR: ROBE",
		color = ARMOR_COLOR,
		sheetName = "armour",
		sheetRow = 5
	))
	result.add(FatItemType(
		flashName = "ArM",
		displayName = "ARMOUR: LEGION",
		color = ARMOR_COLOR,
		sheetName = "armour",
		sheetRow = 6,
	))
	result.add(FatItemType(
		flashName = "ArS",
		displayName = "ARMOUR: STOLE",
		color = ARMOR_COLOR,
		sheetName = "armour",
		sheetRow = 7,
	))
	result.add(FatItemType(
		flashName = "H0",
		displayName = "HELMET: HAT",
		color = ARMOR_COLOR,
		sheetName = "armour",
		sheetRow = 8
	))
	result.add(FatItemType(
		flashName = "H1",
		displayName = "HELMET: HALF HELM",
		color = ARMOR_COLOR,
		sheetName = "armour",
		sheetRow = 8
	))
	result.add(FatItemType(
		flashName = "H2",
		displayName = "HELMET: FULL HELM",
		color = ARMOR_COLOR,
		sheetName = "armour",
		sheetRow = 8
	))

	fun weaponSound(raw: String) = content.audio.effects.find { effect -> effect.flashName == "hit_$raw" }!!

	result.add(FatItemType(
		flashName = "SWORD",
		displayName = "WEAPON: SWORD",
		color = WEAPON_COLOR,
		sheetName = "weapons",
		sheetRow = 0,
		soundEffect = weaponSound("MARTIAL")
	))
	result.add(FatItemType(
		flashName = "SPEAR",
		displayName = "WEAPON: SPEAR",
		color = WEAPON_COLOR,
		sheetName = "weapons",
		sheetRow = 1,
		soundEffect = weaponSound("POLEARMS")
	))
	result.add(FatItemType(
		flashName = "GREATSWORD",
		displayName = "WEAPON: GREATSWORD",
		color = WEAPON_COLOR,
		sheetName = "weapons",
		sheetRow = 2,
		soundEffect = weaponSound("2HSWORDS")
	))
	result.add(FatItemType(
		flashName = "GREATAXE",
		displayName = "WEAPON: GREATAXE",
		color = WEAPON_COLOR,
		sheetName = "weapons",
		sheetRow = 3,
		soundEffect = weaponSound("MARTIAL")
	))
	result.add(FatItemType(
		flashName = "DAGGER",
		displayName = "WEAPON: DAGGER",
		color = WEAPON_COLOR,
		sheetName = "weapons",
		sheetRow = 4,
		soundEffect = weaponSound("MARTIAL")
	))
	result.add(FatItemType(
		flashName = "DOUBLESWORD",
		displayName = "WEAPON: DOUBLESWORD",
		color = WEAPON_COLOR,
		sheetName = "weapons",
		sheetRow = 5,
		soundEffect = weaponSound("MARTIAL")
	))
	result.add(FatItemType(
		flashName = "GREATMACE",
		displayName = "WEAPON: GREATMACE",
		color = WEAPON_COLOR,
		sheetName = "weapons",
		sheetRow = 6,
		soundEffect = weaponSound("MARTIAL")
	))
	result.add(FatItemType(
		flashName = "GUN",
		displayName = "WEAPON: GUN",
		color = WEAPON_COLOR,
		sheetName = "weapons",
		sheetRow = 7,
		soundEffect = weaponSound("GUNS")
	))
	result.add(FatItemType(
		flashName = "ROD",
		displayName = "WEAPON: ROD",
		color = WEAPON_COLOR,
		sheetName = "weapons",
		sheetRow = 8,
		soundEffect = weaponSound("MARTIAL")
	))
	result.add(FatItemType(
		flashName = "STAFF",
		displayName = "WEAPON: STAFF",
		color = WEAPON_COLOR,
		sheetName = "weapons",
		sheetRow = 9,
		soundEffect = weaponSound("STAVES")
	))
	result.add(FatItemType(
		flashName = "WALKINGSTICK",
		displayName = "WEAPON: WALKING STICK",
		color = WEAPON_COLOR,
		sheetName = "weapons",
		sheetRow = 10,
		soundEffect = weaponSound("MARTIAL")
	))
	result.add(FatItemType(
		flashName = "CLAW",
		displayName = "WEAPON: CLAW",
		color = WEAPON_COLOR,
		sheetName = "weapons",
		sheetRow = 11,
		soundEffect = weaponSound("POLEARMS")
	))
	result.add(FatItemType(
		flashName = "HARP",
		displayName = "WEAPON: HARP",
		color = WEAPON_COLOR,
		sheetName = "weapons",
		sheetRow = 12,
		soundEffect = weaponSound("MARTIAL")
	))
	result.add(FatItemType(
		flashName = "SCYTHE",
		displayName = "WEAPON: SCYTHE",
		color = WEAPON_COLOR,
		sheetName = "weapons",
		sheetRow = 13,
		soundEffect = weaponSound("MARTIAL")
	))
	result.add(FatItemType(
		flashName = "ROBOTARM",
		displayName = "WEAPON: ROBOT ARM",
		color = WEAPON_COLOR,
		sheetName = "weapons",
		sheetRow = 14,
		soundEffect = weaponSound("MARTIAL")
	))

	for (fatType in result) {
		content.items.itemTypes.add(ItemType(
			displayName = fatType.displayName,
			gridColor = fatType.color,
		))
	}

	return result
}
