package mardek.renderer.menu.inventory

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.text.TextAlignment
import mardek.content.sprite.KimSprite
import mardek.state.ingame.CampaignState
import mardek.state.ingame.menu.inventory.EquipmentRowRenderInfo
import mardek.state.ingame.menu.inventory.InventoryInteractionState
import mardek.state.util.Rectangle
import kotlin.math.roundToInt

internal fun renderInventoryOverlay(
	inventoryContext: InventoryRenderContext, region: Rectangle,
	scale: Int, state: CampaignState, interaction: InventoryInteractionState,
	equipmentRenderInfo: Collection<EquipmentRowRenderInfo>, oldSortRegion: Rectangle?, oldThrashRegion: Rectangle?,
): Pair<Rectangle, Rectangle> {
	inventoryContext.apply {
		val goldX = (region.minX + region.maxX) * 3 / 4
		simpleSpriteBatch?.simple(
			goldX, region.minY - 20 * scale,
			scale, context.content.ui.goldIcon.index,
		)
		areaSpriteBatch?.draw(context.content.ui.goldIcon, goldX, region.minY - 20 * scale, scale)
		simpleTextBatch.drawShadowedString(
			state.gold.toString(), goldX + 20f * scale, region.minY - 6f * scale,
			10f * scale, context.bundle.getFont(context.content.fonts.large1.index),
			srgbToLinear(rgb(255, 225, 124)), 0, 0f,
			srgbToLinear(rgba(83, 66, 50, 100)),
			1.5f * scale, TextAlignment.LEFT,
		)

		var sortIcon = context.content.ui.sortIcon1
		var thrashIcon = context.content.ui.closedThrashIcon
		val iconHeight = region.height * 0.075f

		var label = ""
		for (equipmentInfo in equipmentRenderInfo) {
			if (interaction.mouseY in equipmentInfo.startY until equipmentInfo.startY + equipmentInfo.slotSpacing) {
				if (interaction.mouseX >= equipmentInfo.startX) {
					val slotIndex = (interaction.mouseX - equipmentInfo.startX) / equipmentInfo.slotSpacing
					if (slotIndex < equipmentInfo.owner.characterClass.equipmentSlots.size) {
						label = equipmentInfo.owner.characterClass.equipmentSlots[slotIndex].displayName
					}
				}
			}
		}

		oldSortRegion?.let {
			if (it.contains(interaction.mouseX, interaction.mouseY)) {
				sortIcon = context.content.ui.sortIcon2
				label = "Sort"
			}
		}

		oldThrashRegion?.let {
			if (it.contains(interaction.mouseX, interaction.mouseY)) {
				if (state.cursorItemStack != null) thrashIcon = context.content.ui.openThrashIcon
				label = "Discard"
			}
		}

		fun renderIcon(icon: KimSprite, offsetX: Int, offsetY: Int): Rectangle {
			val minX = region.minX + region.height * 2 / 5 + offsetX
			val minY = region.minY - region.height / 12
			val scale = iconHeight / icon.height
			simpleSpriteBatch?.simple(minX, minY + offsetY, scale, icon.index)
			areaSpriteBatch?.draw(icon, minX, minY + offsetY, scale)
			return Rectangle(minX, minY, (scale * icon.width).roundToInt(), (scale * icon.height).roundToInt())
		}

		val newSortRegion = renderIcon(sortIcon, 0, (iconHeight * 0.025f).roundToInt())
		val newThrashRegion = renderIcon(thrashIcon, (iconHeight * 1.2f).roundToInt(), 0)

		if (label.isNotEmpty()) {
			val font = context.bundle.getFont(context.content.fonts.fat.index)
			simpleTextBatch.drawString(
				label, interaction.mouseX + 30,
				interaction.mouseY - 6, 12,
				font, srgbToLinear(rgb(238, 203, 127))
			)

			val minX = interaction.mouseX + 15
			val minY = interaction.mouseY - 22
			val maxX = minX + 180
			val maxY = minY + 22
			lateColorBatch.gradientUnaligned(
				minX + 10, maxY, rgba(0f, 0f, 0f, 0.9f),
				maxX, maxY, rgba(0f, 0f, 0f, 0.3f),
				maxX - 10, minY, rgba(0f, 0f, 0f, 0.3f),
				minX, minY, rgba(0f, 0f, 0f, 0.9f),
			)
		}
		return Pair(newSortRegion, newThrashRegion)
	}
}
