package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dGlyphBatch
import com.github.knokko.vk2d.batch.Vk2dImageBatch
import com.github.knokko.vk2d.batch.Vk2dKimBatch
import com.github.knokko.vk2d.batch.Vk2dOvalBatch
import com.github.knokko.vk2d.text.TextAlignment
import mardek.content.sprite.BcSprite
import mardek.content.sprite.KimSprite
import mardek.state.ingame.battle.BattleMoveSelectionItem
import mardek.state.ingame.battle.BattleMoveSelectionSkill
import mardek.state.ingame.battle.BattleStateMachine
import mardek.state.util.Rectangle
import kotlin.math.max
import kotlin.math.roundToInt

internal fun renderSkillOrItemSelection(
	battleContext: BattleRenderContext, colorBatch: Vk2dColorBatch, ovalBatch: Vk2dOvalBatch,
	kimBatch: Vk2dKimBatch, imageBatch: Vk2dImageBatch, textBatch: Vk2dGlyphBatch, region: Rectangle
) {
	battleContext.run {
		val stateMachine = battle.state
		val marginX = region.height / 20
		val entryHeight = region.height / 15

		val entries = run {
			if (stateMachine !is BattleStateMachine.SelectMove) return
			val player = stateMachine.onTurn.player
			val playerState = context.campaign.characterStates[player]!!

			val selectedMove = stateMachine.selectedMove
			if (selectedMove is BattleMoveSelectionSkill && selectedMove.skill != null && selectedMove.target == null) {
				val skills = player.characterClass.skillClass.actions.filter { playerState.canCastSkill(it) }
				skills.map { skill -> SkillOrItemEntry(
					sprite = null, image = skill.element.thickSprite, name = skill.name,
					rightNumber = if (skill.manaCost > 0) skill.manaCost else null,
					isSelected = selectedMove.skill === skill
				) }
			} else if (selectedMove is BattleMoveSelectionItem && selectedMove.item != null && selectedMove.target == null) {
				val playerState = context.campaign.characterStates[player]!!
				val itemSet = playerState.inventory.filter {
					it != null && it.item.consumable != null
				}.mapNotNull { it!!.item }.toSet()
				itemSet.map { item -> SkillOrItemEntry(
					sprite = item.sprite, image = null, name = item.flashName,
					rightNumber = playerState.inventory.sumOf { if (it != null && it.item === item) it.amount else 0 },
					isSelected = selectedMove.item === item
				) }
			} else return
		}

		fun backgroundColor(alpha: Int) = srgbToLinear(rgba(82, 62, 37, alpha))
		val borderColor = srgbToLinear(rgb(208, 193, 142))
		val borderWidth = max(1, region.width / 100)
		colorBatch.gradient(
			region.minX, region.minY, region.maxX, region.maxY,
			backgroundColor(240), backgroundColor(210), backgroundColor(240)
		)
		colorBatch.fill(
			region.minX + borderWidth, region.minY + borderWidth,
			region.maxX - borderWidth, region.minY + 2 * borderWidth - 1, borderColor
		)
		colorBatch.fill(
			region.minX + borderWidth, region.maxY - 2 * borderWidth + 1,
			region.maxX - borderWidth, region.maxY - borderWidth, borderColor
		)
		colorBatch.fill(
			region.minX + borderWidth, region.minY + borderWidth,
			region.minX + 2 * borderWidth - 1, region.maxY - borderWidth, borderColor
		)
		colorBatch.fill(
			region.maxX - 2 * borderWidth + 1, region.minY + borderWidth,
			region.maxX - borderWidth, region.maxY - borderWidth, borderColor
		)

		var minY = region.minY + region.height / 20
		for (entry in entries) {
			var darkColor = rgba(6, 3, 0, 150)
			var textColor = srgbToLinear(rgb(236, 200, 126))
			var shadowColor = srgbToLinear(rgb(61, 35, 18))
			if (entry.isSelected) {
				darkColor = rgba(6, 15, 20, 150)
				textColor = srgbToLinear(rgb(157, 195, 243))
				shadowColor = srgbToLinear(rgb(0, 51, 255))
			}

			var radius = entryHeight * 0.5f
			val oldRadius = radius
			if (entry.image != null) {
				radius *= 1.2f
				imageBatch.coloredScale(
					region.minX + marginX.toFloat(), minY.toFloat() + oldRadius - radius,
					(2f * radius) / entry.image.height, entry.image.index,
					0, rgba(1f, 1f, 1f, 0.5f),
				)
			}
			if (entry.sprite != null) {
				kimBatch.simple(
					region.minX + marginX, minY,
					entryHeight.toFloat() / entry.sprite.height,
					entry.sprite.index
				)
			}
			if (entry.isSelected) {
				val pointer = context.content.ui.pointer
				val scale = 0.9f * entryHeight / pointer.height.toFloat()
				val width = scale * pointer.width
				val x = region.minX + marginX * 0.6f - width
				imageBatch.simpleScale(x, minY + 0.1f * entryHeight, scale, pointer.index)
			}

			val ovalX1 = (region.minX + marginX + radius).roundToInt()
			val ovalX2 = (region.maxX - marginX - oldRadius).roundToInt()
			colorBatch.fill(ovalX1 + 1, minY, ovalX2 - 1, minY + entryHeight, darkColor)
			ovalBatch.antiAliased(
				region.minX, minY, ovalX1, region.maxY,
				ovalX1.toFloat(), minY + radius, radius, radius,
				0.1f, darkColor,
			)
			ovalBatch.antiAliased(
				ovalX2, minY, region.maxX, region.maxY,
				ovalX2.toFloat(), minY + oldRadius, oldRadius, oldRadius,
				0.1f, darkColor
			)

			val font = context.bundle.getFont(context.content.fonts.basic2.index)
			val shadowOffset = 0.05f * entryHeight
			textBatch.drawShadowedString(
				entry.name, ovalX1 + radius + 0.25f * marginX, minY + 0.8f * entryHeight,
				0.66f * entryHeight, font, textColor, 0, 0f,
				shadowColor, shadowOffset, shadowOffset, TextAlignment.LEFT,
			)
			if (entry.rightNumber != null) {
				textBatch.drawShadowedString(
					entry.rightNumber.toString(), region.maxX - marginX - radius,
					minY + 0.8f * entryHeight, 0.66f * entryHeight, font,
					textColor, 0, 0f, shadowColor,
					shadowOffset, shadowOffset, TextAlignment.RIGHT,
				)
			}
			minY += region.height / 12
		}
	}
}

private class SkillOrItemEntry(
	val sprite: KimSprite?, val image: BcSprite?,
	val name: String, val rightNumber: Int?, val isSelected: Boolean
)
