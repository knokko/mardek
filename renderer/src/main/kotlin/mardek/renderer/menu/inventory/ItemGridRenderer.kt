package mardek.renderer.menu.inventory

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.text.TextAlignment
import mardek.content.inventory.ItemStack
import mardek.renderer.menu.MenuRenderContext
import mardek.renderer.util.gradientWithBorder
import mardek.state.ingame.menu.InventoryTab

internal const val SIMPLE_SLOT_SIZE = 18
private val LINE_COLOR = srgbToLinear(rgb(179, 162, 116))
private val LIGHT_SLOT_COLOR = srgbToLinear(rgb(100, 80, 48))
private val DARK_SLOT_COLOR = srgbToLinear(rgb(74, 48, 30))

internal fun renderInventoryGrid(menuContext: MenuRenderContext, startX: Int, startY: Int, scale: Int) {
	menuContext.run {
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

		val tab = menu.currentTab as InventoryTab
		val hoveredItem = tab.hoveringItem
		if (hoveredItem != null) {
			val hoverLineColor = srgbToLinear(rgb(165, 205, 254))
			val hoverLightColor = srgbToLinear(rgb(25, 68, 118))
			val hoverDarkColor = srgbToLinear(rgb(64, 43, 36))
			if (hoveredItem.slotIndex >= 0) {
				val slotX = hoveredItem.slotIndex % 8
				val slotY = hoveredItem.slotIndex / 8

				val x = startX + 1 + slotX * fullSlotSize
				val y = startY + 1 + slotY * fullSlotSize
				val maxX = x + fullSlotSize - 1
				val maxY = y + fullSlotSize - 1
				gradientWithBorder(
					colorBatch, x, y, maxX, maxY, 1, 1, hoverLineColor,
					hoverLightColor, hoverLightColor, hoverDarkColor
				)
			}
		}

		val font = context.bundle.getFont(context.content.fonts.basic2.index)
		fun renderAmount(stack: ItemStack, itemX: Int, itemY: Int) {
			if (stack.amount == 1) return

			val textColor = srgbToLinear(rgb(238, 203, 127))
			val shadowColor = rgb(0, 0, 0)
			textBatch.drawString(
				stack.amount.toString(), itemX + 12f * scale, itemY + 15f * scale, 6f * scale,
				font, textColor, shadowColor, 1.0f * scale, TextAlignment.CENTERED
			)
		}

		val pickedItem = tab.pickedUpItem

		val selectedCharacterPair = context.campaign.allPartyMembers()[tab.partyIndex]
		if (selectedCharacterPair != null) {
			val inventory = selectedCharacterPair.second.inventory
			for (y in 0 until 8) {
				for (x in 0 until 8) {
					val itemStack = inventory[x + 8 * y] ?: continue
					if (pickedItem != null && itemStack === pickedItem.get()) continue
					val itemX = startX + 1 + fullSlotSize * x + scale
					val itemY = startY + 1 + fullSlotSize * y + scale
					spriteBatch.simple(itemX, itemY, scale, itemStack.item.sprite.index)
					renderAmount(itemStack, itemX, itemY)
				}
			}
		}

		if (pickedItem != null && tab.mouseX >= 0 && tab.mouseY >= 0) {
			spriteBatch.simple(tab.mouseX, tab.mouseY, scale, pickedItem.get()!!.item.sprite.index)
			renderAmount(pickedItem.get()!!, tab.mouseX, tab.mouseY)
		}

		tab.renderItemsStartX = startX + 1
		tab.renderItemsStartY = startY + 1
		tab.renderItemSlotSize = fullSlotSize
	}
}
