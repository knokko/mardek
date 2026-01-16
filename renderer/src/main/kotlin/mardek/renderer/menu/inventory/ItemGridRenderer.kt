package mardek.renderer.menu.inventory

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.text.TextAlignment
import com.github.knokko.vk2d.text.Vk2dFont
import mardek.content.inventory.ItemStack
import mardek.renderer.glyph.MardekGlyphBatch
import mardek.renderer.util.gradientWithBorder
import mardek.state.ingame.menu.inventory.InventoryInteractionState
import mardek.state.ingame.menu.inventory.InventorySlotReference
import mardek.state.ingame.menu.inventory.ItemGridRenderInfo

internal const val SIMPLE_SLOT_SIZE = 18
private val LINE_COLOR = srgbToLinear(rgb(179, 162, 116))
private val LIGHT_SLOT_COLOR = srgbToLinear(rgb(100, 80, 48))
private val DARK_SLOT_COLOR = srgbToLinear(rgb(74, 48, 30))

internal fun renderItemStackAmount(
	stack: ItemStack, itemX: Int, itemY: Int, itemScale: Int,
	textBatch: MardekGlyphBatch, font: Vk2dFont,
) {
	if (stack.amount == 1) return

	val textColor = srgbToLinear(rgb(238, 203, 127))
	val shadowColor = rgb(0, 0, 0)
	textBatch.drawString(
		stack.amount.toString(), itemX + 12f * itemScale, itemY + 15f * itemScale, 6f * itemScale,
		font, textColor, shadowColor, 1.0f * itemScale, TextAlignment.CENTERED
	)
}

internal fun renderItemGrid(
	inventoryContext: InventoryRenderContext,
	inventory: Array<ItemStack?>,
	interaction: InventoryInteractionState,
	startX: Int, startY: Int, scale: Int,
) : ItemGridRenderInfo {
	inventoryContext.run {
		val fullSlotSize = scale * SIMPLE_SLOT_SIZE
		val size = 8 * fullSlotSize + 2

		colorBatch.fill(startX, startY, startX + size - 1, startY, LINE_COLOR)
		for (row in 0 until 8) {
			val minY = 1 + startY + row * fullSlotSize
			val maxY = minY + fullSlotSize - 1
			gradientWithBorder(
				colorBatch, startX, minY, startX + size - 1, maxY,
				2, 1, LINE_COLOR,
				LIGHT_SLOT_COLOR, LIGHT_SLOT_COLOR, DARK_SLOT_COLOR
			)
		}
		colorBatch.fill(
			startX, startY + size - 1, startX + size - 1,
			startY + size - 1, LINE_COLOR
		)

		for (column in 1 until 8) {
			val x = startX + column * fullSlotSize
			colorBatch.fill(
				x, startY + 2, x + 1,
				startY + 8 * fullSlotSize - 1, LINE_COLOR
			)
		}

		val hoveringItem = interaction.hoveredSlot
		if (hoveringItem is InventorySlotReference && hoveringItem.inventory === inventory) {
			val hoverLineColor = srgbToLinear(rgb(165, 205, 254))
			val hoverLightColor = srgbToLinear(rgb(25, 68, 118))
			val hoverDarkColor = srgbToLinear(rgb(64, 43, 36))
			val slotX = hoveringItem.index % 8
			val slotY = hoveringItem.index / 8

			val x = startX + 1 + slotX * fullSlotSize
			val y = startY + 1 + slotY * fullSlotSize
			val maxX = x + fullSlotSize - 1
			val maxY = y + fullSlotSize - 1
			gradientWithBorder(
				colorBatch, x, y, maxX, maxY, 1, 1, hoverLineColor,
				hoverLightColor, hoverLightColor, hoverDarkColor
			)
		}

		val font = context.bundle.getFont(context.content.fonts.basic2.index)

		for (y in 0 until 8) {
			for (x in 0 until 8) {
				val itemStack = inventory[x + 8 * y] ?: continue
				val itemX = startX + 1 + fullSlotSize * x + scale
				val itemY = startY + 1 + fullSlotSize * y + scale
				spriteBatch.simple(itemX, itemY, scale, itemStack.item.sprite.index)
				renderItemStackAmount(itemStack, itemX, itemY, scale, textBatch, font)
			}
		}

		val pickedUpItem = context.campaign.cursorItemStack
		if (pickedUpItem != null && interaction.mouseX >= 0 && interaction.mouseY >= 0) {
			val itemX = interaction.mouseX - 10
			val itemY = interaction.mouseY - 10
			val itemScale = 3
			context.addKim3Batch(2).simple(
				itemX, itemY, itemScale,
				pickedUpItem.item.sprite.index,
			)
			renderItemStackAmount(
				pickedUpItem, itemX, itemY + itemScale, itemScale,
				context.addFancyTextBatch(10), font,
			)
		}

		return ItemGridRenderInfo(startX + 1, startY + 1, fullSlotSize)
	}
}
