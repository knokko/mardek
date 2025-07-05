package mardek.renderer.battle.ui

import com.github.knokko.boiler.utilities.ColorPacker.*
import com.github.knokko.text.placement.TextAlignment
import mardek.content.sprite.KimSprite
import mardek.renderer.batch.KimBatch
import mardek.renderer.batch.KimRequest
import mardek.renderer.battle.BattleRenderContext
import mardek.renderer.ui.ResourceBarRenderer
import mardek.renderer.ui.ResourceType
import mardek.renderer.ui.renderDescription
import mardek.state.ingame.battle.BattleMoveSelectionItem
import mardek.state.ingame.battle.BattleMoveSelectionSkill
import mardek.state.ingame.battle.BattleStateMachine
import mardek.state.title.AbsoluteRectangle
import kotlin.math.roundToInt

class SkillOrItemDescriptionRenderer(
	private val context: BattleRenderContext,
	private val region: AbsoluteRectangle,
) {

	private lateinit var batch1: KimBatch
	private lateinit var batch2: KimBatch
	private val charsPerLine = (7f * region.width / region.height).roundToInt()

	private val selectedElement = run {
		val state = context.battle.state
		if (state !is BattleStateMachine.SelectMove) return@run null
		val selectedMove = state.selectedMove
		if (charsPerLine < 10) null
		else if (selectedMove is BattleMoveSelectionSkill && selectedMove.skill != null && selectedMove.target == null) {
			val skill = selectedMove.skill!!
			val playerState = context.campaign.characterStates[state.onTurn.player]!!
			SelectedElement(
				name = skill.name, icon = skill.element.sprite, description = skill.description,
				currentMastery = playerState.skillMastery[skill] ?: 0, maxMastery = skill.masteryPoints
			)
		} else if (selectedMove is BattleMoveSelectionItem && selectedMove.item != null && selectedMove.target == null) {
			val item = selectedMove.item!!
			SelectedElement(
				name = item.flashName, icon = item.sprite, description = item.description,
				currentMastery = null, maxMastery = null
			)
		} else null
	}

	fun beforeRendering() {
		if (selectedElement == null) return

		if (selectedElement.icon.version == 1 ||
			(selectedElement.currentMastery != null && selectedElement.currentMastery >= selectedElement.maxMastery!!)
		) batch1 = context.resources.kim1Renderer.startBatch()
		if (selectedElement.icon.version == 2) batch2 = context.resources.kim2Renderer.startBatch()

		val iconRequest = KimRequest(
			x = region.minX + region.width / 100, y = region.minY + region.height / 20,
			scale = 0.9f * region.height / selectedElement.icon.height, sprite = selectedElement.icon
		)
		if (selectedElement.icon.version == 2) batch2.requests.add(iconRequest)
		else batch1.requests.add(iconRequest)

		if (selectedElement.currentMastery != null && selectedElement.maxMastery != null) {
			if (selectedElement.currentMastery >= selectedElement.maxMastery) {
				batch1.requests.add(KimRequest(
					x = region.minX + region.width / 50 + region.height,
					y = region.minY + 5 * region.height / 9,
					scale = 0.25f * region.height / context.content.ui.mastered.height,
					sprite = context.content.ui.mastered
				))
			}
		}
	}

	fun render() {
		if (selectedElement == null) return

		run {
			val rectangles = context.resources.rectangleRenderer
			rectangles.beginBatch(context, 1)
			val leftColor = srgbToLinear(rgba(60, 45, 30, 230))
			val rightColor = srgbToLinear(rgba(120, 90, 40, 230))
			rectangles.gradient(
				region.minX, region.minY, region.maxX, region.maxY,
				leftColor, rightColor, leftColor
			)
			rectangles.endBatch(context.recorder)
		}

		val textColor = srgbToLinear(rgb(238, 203, 127))
		context.uiRenderer.beginBatch()
		context.uiRenderer.drawString(
			context.resources.font, selectedElement.name, textColor, IntArray(0),
			region.minX + region.width / 100 + region.height, region.minY, region.maxX, region.maxY,
			region.minY + 2 * region.height / 5, region.height / 5, 1, TextAlignment.LEFT
		)
		if (selectedElement.currentMastery != null && selectedElement.maxMastery != null) {
			if (selectedElement.currentMastery < selectedElement.maxMastery) {
				val masteryRenderer = ResourceBarRenderer(context, ResourceType.SkillMastery, AbsoluteRectangle(
					minX = region.minX + region.width / 100 + region.height,
					minY = region.minY + 3 * region.height / 5,
					width = 3 * region.height / 2, height = region.height / 6
				))
				masteryRenderer.renderBar(selectedElement.currentMastery, selectedElement.maxMastery)
				masteryRenderer.renderTextOverBar(selectedElement.currentMastery, selectedElement.maxMastery)
			}
		}
		var descriptionY = region.minY + region.height / 3
		renderDescription(selectedElement.description, charsPerLine) { line ->
			context.uiRenderer.drawString(
				context.resources.font, line, textColor, IntArray(0),
				region.minX + 3 * region.width / 10, region.minY, region.maxX, region.maxY,
				descriptionY, region.height / 6, 1, TextAlignment.LEFT
			)
			descriptionY += region.height / 4
		}
		context.uiRenderer.endBatch()

		if (this::batch1.isInitialized) context.resources.kim1Renderer.submit(batch1, context)
		if (this::batch2.isInitialized) context.resources.kim2Renderer.submit(batch2, context)
	}
}

private class SelectedElement(
	val name: String,
	val icon: KimSprite,
	val description: String,
	val currentMastery: Int?,
	val maxMastery: Int?,
)
