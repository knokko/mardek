package mardek.importer.inventory

import com.github.knokko.compressor.Kim1Compressor
import mardek.assets.combat.CombatAssets
import mardek.assets.inventory.InventoryAssets
import mardek.assets.inventory.Item
import mardek.assets.skill.SkillAssets
import mardek.importer.util.parseActionScriptResource
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.*
import java.awt.Color
import java.awt.image.BufferedImage

fun importInventoryAssets(
	combatAssets: CombatAssets, skillAssets: SkillAssets, resourcePath: String
): InventoryAssets {
	val itemData = parseActionScriptResource(resourcePath)
	val weaponSheet = sheet("weapons")
	val armorSheet = sheet("armour")
	val miscSheet = sheet("misc")

	val assets = InventoryAssets()
	importItemTypes(assets, itemData.variableAssignments["sheetIDs"]!!, itemData.variableAssignments["STACKABLE_TYPES"]!!)
	importWeaponTypes(assets, itemData.variableAssignments["wpnIDs"]!!, itemData.variableAssignments["WeaponSFXType"]!!)
	importArmorTypes(assets, itemData.variableAssignments["ARMOUR_TYPES"]!!)
	importItems(combatAssets, skillAssets, assets, itemData.variableAssignments["ItemList"]!!)

	val pixelBuffer = memCalloc(4 * 16 * 16)

	fun putImage(image: BufferedImage) {
		pixelBuffer.position(0)
		for (x in 0 until image.width) {
			for (y in 0 until image.height) {
				val color = Color(image.getRGB(x, y), true)
				val index = 4 * (x + y * image.width)
				pixelBuffer.put(index, color.red.toByte())
				pixelBuffer.put(index + 1, color.green.toByte())
				pixelBuffer.put(index + 2, color.blue.toByte())
				pixelBuffer.put(index + 3, color.alpha.toByte())
			}
		}
	}

	fun assignSprite(item: Item, image: BufferedImage) {
		putImage(image)
		val compressor = Kim1Compressor(pixelBuffer, image.width, image.height, 4)

		stackPush().use { stack ->
			val spriteBuffer = stack.calloc(4 * compressor.intSize)
			compressor.compress(spriteBuffer)
			item.sprite = IntArray(compressor.intSize) { index -> spriteBuffer.getInt(4 * index) }
		}
	}

	for ((rowIndex, miscType) in arrayOf("accs", "invn", "item", "gems", "plot", "misc", "song").withIndex()) {
		for ((columnIndex, item) in assets.items.filter { it.type.flashName == miscType }.withIndex()) {
			assignSprite(item, miscSheet.getSubimage(16 * columnIndex, 16 * rowIndex, 16, 16))
		}
	}
	for ((rowIndex, armorType) in arrayOf("Sh", "Ar0", "Ar1", "Ar2", "Ar3", "ArR", "ArM", "ArS").withIndex()) {
		for ((columnIndex, item) in assets.items.filter { it.equipment?.armor?.type?.key == armorType }.withIndex()) {
			assignSprite(item, armorSheet.getSubimage(16 * columnIndex, 16 * rowIndex, 16, 16))
		}
	}

	for ((columnIndex, item) in assets.items.filter { it.type.flashName == "helm" }.withIndex()) {
		assignSprite(item, armorSheet.getSubimage(16 * columnIndex, 16 * 8, 16, 16))
	}

	for ((rowIndex, weaponType) in assets.weaponTypes.withIndex()) {
		for ((columnIndex, item) in assets.items.filter { it.equipment?.weapon?.type == weaponType }.withIndex()) {
			assignSprite(item, weaponSheet.getSubimage(16 * columnIndex, 16 * rowIndex, 16, 16))
		}
	}

	memFree(pixelBuffer)

	return assets
}

class ItemParseException(message: String): RuntimeException(message)
