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
	equipmentRenderInfo: Collection<EquipmentRowRenderInfo>, oldThrashRegion: Rectangle?,
): Rectangle {
	inventoryContext.apply {
		val goldX = (region.minX + region.maxX) * 3 / 4
		spriteBatch.simple(
			goldX, region.minY - 20 * scale,
			scale, context.content.ui.goldIcon.index,
		)
		textBatch.drawShadowedString(
			state.gold.toString(), goldX + 20f * scale, region.minY - 6f * scale,
			10f * scale, context.bundle.getFont(context.content.fonts.large1.index),
			srgbToLinear(rgb(255, 225, 124)), 0, 0f,
			srgbToLinear(rgba(83, 66, 50, 100)), 1.5f * scale,
			1.5f * scale, TextAlignment.LEFT,
		)

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

		oldThrashRegion?.let {
			if (it.contains(interaction.mouseX, interaction.mouseY)) {
				if (state.cursorItemStack != null) thrashIcon = context.content.ui.openThrashIcon
				label = "Discard"
			}
		}

		fun renderIcon(icon: KimSprite, offsetX: Int): Rectangle {
			val minX = region.minX + region.height * 2 / 5 + offsetX
			val minY = region.minY - region.height / 12
			val scale = iconHeight / icon.height
			spriteBatch.simple(minX, minY, scale, icon.index)
			return Rectangle(minX, minY, (scale * icon.width).roundToInt(), (scale * icon.height).roundToInt())
		}
		val newThrashRegion = renderIcon(thrashIcon, 0)
		// TODO CHAP2 tab.sortRegion = renderIcon(sortIcon, region.height)

		if (label.isNotEmpty()) {
			val font = context.bundle.getFont(context.content.fonts.fat.index)
			textBatch.drawString(
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
		return newThrashRegion
	}
}
