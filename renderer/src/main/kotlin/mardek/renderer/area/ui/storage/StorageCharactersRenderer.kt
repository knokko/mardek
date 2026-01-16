package mardek.renderer.area.ui.storage

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import mardek.renderer.menu.inventory.InventoryRenderContext
import mardek.renderer.menu.referenceTime
import mardek.renderer.util.gradientWithBorder
import mardek.state.ingame.actions.ItemStorageCharacter
import mardek.state.ingame.menu.inventory.InventoryInteractionState
import mardek.state.util.Rectangle

internal fun renderItemStorageCharacterSlots(
	inventoryContext: InventoryRenderContext, scale: Int, region: Rectangle, inventory: InventoryInteractionState,
) = inventoryContext.run {
	val availableCharacters = context.content.playableCharacters.filter {
		context.campaign.story.evaluate(it.isAvailable) != null ||
				context.campaign.story.evaluate(it.isInventoryAvailable) != null ||
				context.campaign.party.contains(it)
	}

	val baseSlotSpacing = 22
	val fullSlotSpacing = scale * baseSlotSpacing
	val numRows = region.height / fullSlotSpacing
	val numColumns = region.width / fullSlotSpacing
	val baseSlotSize = 18
	val fullSlotSize = scale * baseSlotSize

	val result = mutableListOf<ItemStorageCharacter>()
	for (column in 0 until numColumns) {
		for (row in 0 until numRows) {
			val baseX = region.minX + fullSlotSpacing * column
			val baseY = region.minY + fullSlotSpacing * row
			val characterRegion = Rectangle(baseX, baseY, fullSlotSize, fullSlotSize)

			var borderColor = rgb(208, 193, 142)
			var bottomColor = rgb(99, 78, 45)
			var topColor = rgb(69, 46, 29)
			if (inventory.mouseX in baseX .. characterRegion.maxX && inventory.mouseY in baseY .. characterRegion.maxY) {
				borderColor = rgb(165, 205, 254)
				bottomColor = rgb(25, 68, 118)
				topColor = rgb(64, 43, 36)
			}
			gradientWithBorder(
				colorBatch, baseX, baseY, characterRegion.maxX, characterRegion.maxY,
				1, 1,
				srgbToLinear(borderColor),
				srgbToLinear(bottomColor),
				srgbToLinear(bottomColor),
				srgbToLinear(topColor),
			)

			val characterIndex = row + column * numRows
			if (characterIndex < availableCharacters.size) {
				val character = availableCharacters[characterIndex]
				var spriteIndex = 0
				val passedTime = System.nanoTime() - referenceTime
				val animationPeriod = 700_000_000L
				if (passedTime % animationPeriod >= animationPeriod / 2) spriteIndex = 1

				spriteBatch.simple(
					baseX + scale, baseY + 2 * scale - 1, scale,
					character.areaSprites.sprites[spriteIndex].index
				)
				result.add(ItemStorageCharacter(character, characterRegion))
			} else result.add(ItemStorageCharacter(null, characterRegion))
		}
	}

	result.toTypedArray()
}
