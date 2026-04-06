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
		niceName = "Accessory",
		color = ACCESSORY_COLOR,
		sheetName = "misc",
		sheetRow = 0,
	))
	result.add(FatItemType(
		flashName = "invn",
		displayName = "INVENTION",
		niceName = "-",
		color = WEAPON_COLOR,
		sheetName = "misc",
		sheetRow = 1,
	))
	result.add(FatItemType(
		flashName = "item",
		displayName = "EXPENDABLE ITEM",
		niceName = "Expendable item",
		color = CONSUMABLE_ITEM_COLOR,
		sheetName = "misc",
		sheetRow = 2,
	))
	result.add(FatItemType(
		flashName = "gems",
		displayName = "GEMSTONE",
		niceName = "Gemstone",
		color = ACCESSORY_COLOR,
		sheetName = "misc",
		sheetRow = 3,
	))
	result.add(FatItemType(
		flashName = "misc",
		displayName = "MISCELLANEOUS ITEM",
		niceName = "Miscellaneous item",
		color = BASE_ITEM_COLOR,
		sheetName = "misc",
		sheetRow = 5,
	))
	result.add(FatItemType(
		flashName = "song",
		displayName = "MUSIC SHEET",
		niceName = "Music sheet",
		color = ARMOR_COLOR,
		sheetName = "misc",
		sheetRow = 6,
	))
	result.add(FatItemType(
		flashName = "Sh",
		displayName = "SHIELD",
		niceName = "Shield",
		color = ARMOR_COLOR,
		sheetName = "armour",
		sheetRow = 0
	))
	result.add(FatItemType(
		flashName = "Ar0",
		displayName = "ARMOUR: CLOTHING",
		niceName = "Clothing",
		color = ARMOR_COLOR,
		sheetName = "armour",
		sheetRow = 1
	))
	result.add(FatItemType(
		flashName = "Ar1",
		displayName = "ARMOUR: LIGHT",
		niceName = "Light armour",
		color = ARMOR_COLOR,
		sheetName = "armour",
		sheetRow = 2
	))
	result.add(FatItemType(
		flashName = "Ar2",
		displayName = "ARMOUR: MEDIUM",
		niceName = "Medium armour",
		color = ARMOR_COLOR,
		sheetName = "armour",
		sheetRow = 3
	))
	result.add(FatItemType(
		flashName = "Ar3",
		displayName = "ARMOUR: HEAVY",
		niceName = "Heavy armour",
		color = ARMOR_COLOR,
		sheetName = "armour",
		sheetRow = 4
	))
	result.add(FatItemType(
		flashName = "ArR",
		displayName = "ARMOUR: ROBE",
		niceName = "Robe",
		color = ARMOR_COLOR,
		sheetName = "armour",
		sheetRow = 5
	))
	result.add(FatItemType(
		flashName = "ArM",
		displayName = "ARMOUR: LEGION",
		niceName = "Robot armour",
		color = ARMOR_COLOR,
		sheetName = "armour",
		sheetRow = 6,
	))
	result.add(FatItemType(
		flashName = "ArS",
		displayName = "ARMOUR: STOLE",
		niceName = "Stole",
		color = ARMOR_COLOR,
		sheetName = "armour",
		sheetRow = 7,
	))
	result.add(FatItemType(
		flashName = "H0",
		displayName = "HELMET: HAT",
		niceName = "Hat",
		color = ARMOR_COLOR,
		sheetName = "armour",
		sheetRow = 8
	))
	result.add(FatItemType(
		flashName = "H1",
		displayName = "HELMET: HALF HELM",
		niceName = "Half helm",
		color = ARMOR_COLOR,
		sheetName = "armour",
		sheetRow = 8
	))
	result.add(FatItemType(
		flashName = "H2",
		displayName = "HELMET: FULL HELM",
		niceName = "Full helm",
		color = ARMOR_COLOR,
		sheetName = "armour",
		sheetRow = 8
	))

	fun weaponSound(raw: String) = content.audio.effects.find { effect -> effect.flashName == "hit_$raw" }!!

	result.add(FatItemType(
		flashName = "SWORD",
		displayName = "WEAPON: SWORD",
		niceName = "Sword",
		color = WEAPON_COLOR,
		sheetName = "weapons",
		sheetRow = 0,
		soundEffect = weaponSound("MARTIAL")
	))
	result.add(FatItemType(
		flashName = "SPEAR",
		displayName = "WEAPON: SPEAR",
		niceName = "Spear",
		color = WEAPON_COLOR,
		sheetName = "weapons",
		sheetRow = 1,
		soundEffect = weaponSound("POLEARMS")
	))
	result.add(FatItemType(
		flashName = "GREATSWORD",
		displayName = "WEAPON: GREATSWORD",
		niceName = "Greatsword",
		color = WEAPON_COLOR,
		sheetName = "weapons",
		sheetRow = 2,
		soundEffect = weaponSound("2HSWORDS")
	))
	result.add(FatItemType(
		flashName = "GREATAXE",
		displayName = "WEAPON: GREATAXE",
		niceName = "Greataxe",
		color = WEAPON_COLOR,
		sheetName = "weapons",
		sheetRow = 3,
		soundEffect = weaponSound("MARTIAL")
	))
	result.add(FatItemType(
		flashName = "DAGGER",
		displayName = "WEAPON: DAGGER",
		niceName = "Dagger",
		color = WEAPON_COLOR,
		sheetName = "weapons",
		sheetRow = 4,
		soundEffect = weaponSound("MARTIAL")
	))
	result.add(FatItemType(
		flashName = "DOUBLESWORD",
		displayName = "WEAPON: DOUBLESWORD",
		niceName = "Doublesword",
		color = WEAPON_COLOR,
		sheetName = "weapons",
		sheetRow = 5,
		soundEffect = weaponSound("MARTIAL")
	))
	result.add(FatItemType(
		flashName = "GREATMACE",
		displayName = "WEAPON: GREATMACE",
		niceName = "Greatmace",
		color = WEAPON_COLOR,
		sheetName = "weapons",
		sheetRow = 6,
		soundEffect = weaponSound("MARTIAL")
	))
	result.add(FatItemType(
		flashName = "GUN",
		displayName = "WEAPON: GUN",
		niceName = "Handgun",
		color = WEAPON_COLOR,
		sheetName = "weapons",
		sheetRow = 7,
		soundEffect = weaponSound("GUNS")
	))
	result.add(FatItemType(
		flashName = "ROD",
		displayName = "WEAPON: ROD",
		niceName = "Wand",
		color = WEAPON_COLOR,
		sheetName = "weapons",
		sheetRow = 8,
		soundEffect = weaponSound("MARTIAL")
	))
	result.add(FatItemType(
		flashName = "STAFF",
		displayName = "WEAPON: STAFF",
		niceName = "Staff",
		color = WEAPON_COLOR,
		sheetName = "weapons",
		sheetRow = 9,
		soundEffect = weaponSound("STAVES")
	))
	result.add(FatItemType(
		flashName = "WALKINGSTICK",
		displayName = "WEAPON: WALKING STICK",
		niceName = "Walkingstick",
		color = WEAPON_COLOR,
		sheetName = "weapons",
		sheetRow = 10,
		soundEffect = weaponSound("MARTIAL")
	))
	result.add(FatItemType(
		flashName = "CLAW",
		displayName = "WEAPON: CLAW",
		niceName = "Claws",
		color = WEAPON_COLOR,
		sheetName = "weapons",
		sheetRow = 11,
		soundEffect = weaponSound("POLEARMS")
	))
	result.add(FatItemType(
		flashName = "HARP",
		displayName = "WEAPON: HARP",
		niceName = "Harp",
		color = WEAPON_COLOR,
		sheetName = "weapons",
		sheetRow = 12,
		soundEffect = weaponSound("MARTIAL")
	))
	result.add(FatItemType(
		flashName = "SCYTHE",
		displayName = "WEAPON: SCYTHE",
		niceName = "Scythe",
		color = WEAPON_COLOR,
		sheetName = "weapons",
		sheetRow = 13,
		soundEffect = weaponSound("MARTIAL")
	))
	result.add(FatItemType(
		flashName = "ROBOTARM",
		displayName = "WEAPON: ROBOT ARM",
		niceName = "Robotic Arms",
		color = WEAPON_COLOR,
		sheetName = "weapons",
		sheetRow = 14,
		soundEffect = weaponSound("MARTIAL")
	))
	result.add(FatItemType(
		flashName = "FIST",
		displayName = "WEAPON: FISTS",
		niceName = "Fists",
		color = WEAPON_COLOR,
		sheetName = "weapons",
		sheetRow = 14,
		soundEffect = weaponSound("MARTIAL")
	))
	result.add(FatItemType(
		flashName = "KATANA",
		displayName = "WEAPON: KATANA",
		niceName = "Katana",
		color = WEAPON_COLOR,
		sheetName = "weapons",
		sheetRow = 14,
		soundEffect = weaponSound("MARTIAL")
	))
	result.add(FatItemType(
		flashName = "RAPIER",
		displayName = "WEAPON: RAPIER",
		niceName = "Rapier",
		color = WEAPON_COLOR,
		sheetName = "weapons",
		sheetRow = 14,
		soundEffect = weaponSound("MARTIAL")
	))
	result.add(FatItemType(
		flashName = "UNKNOWN",
		displayName = "WEAPON: UNKNOWN",
		niceName = "Unknown",
		color = WEAPON_COLOR,
		sheetName = "weapons",
		sheetRow = 14,
		soundEffect = weaponSound("MARTIAL")
	))
	result.add(FatItemType(
		flashName = "AQUILA",
		displayName = "WEAPON: AQUILA",
		niceName = "Aquila",
		color = WEAPON_COLOR,
		sheetName = "weapons",
		sheetRow = 14,
		soundEffect = weaponSound("MARTIAL")
	))

	for (fatType in result) {
		content.items.itemTypes.add(ItemType(
			displayName = fatType.displayName,
			gridColor = fatType.color,
			niceName = fatType.niceName,
		))
	}

	return result
}
