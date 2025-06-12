package mardek.renderer.area

import mardek.renderer.InGameRenderContext
import mardek.state.ingame.characters.CharacterState

fun renderLootInventoryGrid(
	context: InGameRenderContext, party: List<CharacterState?>,
	minX: Int, minY: Int, columnWidth: Int, scale: Int
) {
	context.resources.colorGridRenderer.startBatch(context.recorder)
	for ((column, characterState) in party.withIndex()) {
		if (characterState == null) continue
		val inventory = characterState.inventory
		if (inventory.size % 8 != 0) throw Error("Huh? inventory size is ${inventory.size}")

		val colorIndexBuffer = context.resources.colorGridRenderer.drawGrid(
			context.recorder, context.targetImage, minX + columnWidth * column, minY,
			8, inventory.size / 8, 0, 2 * scale
		)

		for (row in 0 until inventory.size / 8) {
			var indices = 0u
			for (itemColumn in 0 until 8) {
				var localBits = 0u
				val itemStack = inventory[itemColumn + 8 * row]
				if (itemStack != null) {
					val item = itemStack.item
					localBits = 1u

					if (item.consumable != null) localBits = 2u
					val equipment = item.equipment
					if (equipment != null) {
						localBits = 5u
						if (equipment.weapon != null) localBits = 3u
						if (equipment.armorType != null) localBits = 4u
					}
				}
				indices = indices or (localBits shl (4 * itemColumn))
			}
			colorIndexBuffer.put(indices.toInt())
		}
	}
	context.resources.colorGridRenderer.endBatch()
}
