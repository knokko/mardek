package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dImageBatch
import com.github.knokko.vk2d.batch.Vk2dKim3Batch
import com.github.knokko.vk2d.text.TextAlignment
import mardek.content.sprite.BcSprite
import mardek.content.sprite.KimSprite
import mardek.renderer.glyph.MardekGlyphBatch
import mardek.renderer.util.ResourceBarRenderer
import mardek.renderer.util.ResourceType
import mardek.renderer.util.renderDescription
import mardek.renderer.util.renderFancyMasteredText
import mardek.state.ingame.battle.BattleMoveSelectionItem
import mardek.state.ingame.battle.BattleMoveSelectionSkill
import mardek.state.ingame.battle.BattleStateMachine
import mardek.state.util.Rectangle
import kotlin.math.roundToInt

internal fun renderSkillOrItemDescription(
	battleContext: BattleRenderContext, colorBatch: Vk2dColorBatch,
	spriteBatch: Vk2dKim3Batch, imageBatch: Vk2dImageBatch, textBatch: MardekGlyphBatch, region: Rectangle,
) {
	battleContext.run {
		val charsPerLine = (7f * region.width / region.height).roundToInt()
		if (charsPerLine < 10) return

		val selectedElement = run {
			val stateMachine = battle.state
			if (stateMachine !is BattleStateMachine.SelectMove) return

			val selectedMove = stateMachine.selectedMove
			if (selectedMove is BattleMoveSelectionSkill && selectedMove.skill != null && selectedMove.target == null) {
				val skill = selectedMove.skill!!
				val playerState = context.campaign.characterStates[stateMachine.onTurn.player]!!
				SelectedDescriptionElement(
					name = skill.name, sprite = null, image = skill.element.thinSprite, description = skill.description,
					currentMastery = playerState.skillMastery[skill] ?: 0, maxMastery = skill.masteryPoints
				)
			} else if (selectedMove is BattleMoveSelectionItem && selectedMove.item != null && selectedMove.target == null) {
				val item = selectedMove.item!!
				SelectedDescriptionElement(
					name = item.displayName, sprite = item.sprite, image = null,
					description = item.description, currentMastery = null, maxMastery = null
				)
			} else return
		}

		if (selectedElement.sprite != null) {
			spriteBatch.simple(
				region.minX + region.width / 100, region.minY + region.height / 20,
				0.9f * region.height / selectedElement.sprite.height, selectedElement.sprite.index,
			)
		}
		if (selectedElement.image != null) {
			imageBatch.simpleScale(
				region.minX + region.width * 0.01f, region.minY + region.height * 0.05f,
				0.9f * region.height / selectedElement.image.height, selectedElement.image.index,
			)
		}

		if (selectedElement.currentMastery != null && selectedElement.maxMastery != null) {
			if (selectedElement.currentMastery >= selectedElement.maxMastery) {
				renderFancyMasteredText(
					context, textBatch,
					region.minX + region.width * 0.02f + region.height,
					region.minY + region.height * 0.8f, 0.25f * region.height
				)
			}
		}

		run {
			val leftColor = srgbToLinear(rgba(60, 45, 30, 230))
			val rightColor = srgbToLinear(rgba(120, 90, 40, 230))
			colorBatch.gradient(
				region.minX, region.minY + 1, region.maxX, region.maxY - 1,
				leftColor, rightColor, leftColor
			)

			val upperLineColor = srgbToLinear(rgb(126, 111, 74))
			val lowerLineColor = srgbToLinear(rgb(208, 193, 142))
			colorBatch.gradient(
				region.minX, region.minY, region.maxX, region.minY,
				upperLineColor, 0, upperLineColor
			)
			colorBatch.fill(
				region.minX, region.maxY, region.maxX, region.maxY, lowerLineColor
			)
		}

		val textColor = srgbToLinear(rgb(238, 203, 127))
		var shadowColor = rgba(0f, 0f, 0f, 0.8f)
		val shadowOffset = 0.02f * region.height
		val unknownFont = context.bundle.getFont(context.content.fonts.basic2.index)
		textBatch.drawShadowedString(
			selectedElement.name, region.minX + 0.01f * region.width + region.height,
			region.minY + 0.4f * region.height, 0.2f * region.height, unknownFont, textColor,
					rgb(0, 0, 0), 0.02f * region.height, shadowColor,
			shadowOffset, shadowOffset, TextAlignment.LEFT,
		)
		if (selectedElement.currentMastery != null && selectedElement.maxMastery != null) {
			if (selectedElement.currentMastery < selectedElement.maxMastery) {
				val masteryRenderer = ResourceBarRenderer(
					context, ResourceType.SkillMastery, Rectangle(
						minX = region.minX + region.width / 100 + region.height,
						minY = region.minY + 3 * region.height / 5,
						width = 3 * region.height / 2, height = region.height / 6
					), colorBatch, textBatch,
				)
				masteryRenderer.renderBar(selectedElement.currentMastery, selectedElement.maxMastery)
				masteryRenderer.renderTextOverBar(selectedElement.currentMastery, selectedElement.maxMastery)
			}
		}

		var descriptionY = region.minY + region.height * 0.33f
		shadowColor = srgbToLinear(rgb(90, 52, 22))
		renderDescription(selectedElement.description, charsPerLine) { line ->
			textBatch.drawShadowedString(
				line, region.minX + 0.3f * region.width, descriptionY,
				0.16f * region.height, unknownFont, textColor,
				0, 0f, shadowColor,
				shadowOffset, shadowOffset, TextAlignment.LEFT,
			)
			descriptionY += region.height * 0.25f
		}
	}
}

private class SelectedDescriptionElement(
	val name: String,
	val sprite: KimSprite?,
	val image: BcSprite?,
	val description: String,
	val currentMastery: Int?,
	val maxMastery: Int?,
)
