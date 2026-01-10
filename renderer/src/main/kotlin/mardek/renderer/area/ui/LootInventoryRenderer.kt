package mardek.renderer.area.ui

import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import mardek.state.UsedPartyMember

internal fun renderLootInventoryGrid(
	colorBatch: Vk2dColorBatch, party: List<UsedPartyMember>,
	minX: Int, minY: Int, columnWidth: Int, scale: Int
) {
	for ((column, _, characterState) in party) {
		val inventory = characterState.inventory
		if (inventory.size % 8 != 0) throw Error("Huh? inventory size is ${inventory.size}")

		for (row in 0 until 8) {
			for (inventoryColumn in 0 until 8) {
				val x = minX + columnWidth * column + scale * inventoryColumn
				val y = minY + scale * row

				val itemStack = characterState.inventory[8 * row + inventoryColumn]
				if (itemStack != null) {
					colorBatch.fill(
						x, y, x + scale - 1, y + scale - 1,
						srgbToLinear(itemStack.item.type.gridColor),
					)
				}
			}
		}
	}
}
