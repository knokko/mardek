package mardek.renderer.area

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import mardek.state.ingame.characters.CharacterState

private val BASE_ITEM_COLOR = srgbToLinear(rgb(193, 145, 89))
private val CONSUMABLE_ITEM_COLOR = srgbToLinear(rgb(81, 113, 217))
private val WEAPON_COLOR = srgbToLinear(rgb(224, 128, 80))
private val ARMOR_COLOR = srgbToLinear(rgb(145, 209, 89))
private val ACCESSORY_COLOR = srgbToLinear(rgb(209, 209, 89))

internal fun renderLootInventoryGrid(
	colorBatch: Vk2dColorBatch, party: List<CharacterState?>,
	minX: Int, minY: Int, columnWidth: Int, scale: Int
) {
	for ((column, characterState) in party.withIndex()) {
		if (characterState == null) continue
		val inventory = characterState.inventory
		if (inventory.size % 8 != 0) throw Error("Huh? inventory size is ${inventory.size}")

		for (row in 0 until 8) {
			for (inventoryColumn in 0 until 8) {
				val x = minX + columnWidth * column + scale * inventoryColumn
				val y = minY + scale * row

				val itemStack = characterState.inventory[8 * row + inventoryColumn]
				if (itemStack != null) {
					var slotColor = BASE_ITEM_COLOR
					val item = itemStack.item
					if (item.consumable != null) slotColor = CONSUMABLE_ITEM_COLOR
					val equipment = item.equipment
					if (equipment != null) {
						slotColor = ACCESSORY_COLOR
						if (equipment.weapon != null) slotColor = WEAPON_COLOR
						if (equipment.armorType != null) slotColor = ARMOR_COLOR
					}
					colorBatch.fill(x, y, x + scale - 1, y + scale - 1, slotColor)
				}
			}
		}
	}
}
