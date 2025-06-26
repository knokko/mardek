package mardek.renderer.battle.ui

import com.github.knokko.boiler.utilities.ColorPacker.*
import com.github.knokko.text.placement.TextAlignment
import mardek.content.stats.Element
import mardek.renderer.batch.KimBatch
import mardek.renderer.batch.KimRequest
import mardek.renderer.battle.BattleRenderContext
import mardek.renderer.battle.transformBattleCoordinates
import mardek.state.ingame.battle.*
import mardek.state.title.AbsoluteRectangle
import kotlin.math.abs
import kotlin.math.roundToInt

class TargetSelectionRenderer(
	private val context: BattleRenderContext,
	private val region: AbsoluteRectangle,
) {

	private val state = context.battle.state
	private val elementX = region.maxX - region.width / 3
	private val elementY = region.maxY - region.height / 10

	private val target = run {
		if (state !is BattleStateMachine.SelectMove) return@run null
		val selectedMove = state.selectedMove
		if (selectedMove is BattleMoveSelectionAttack && selectedMove.target != null) {
			BattleSkillTargetSingle(selectedMove.target!!)
		} else if (selectedMove is BattleMoveSelectionSkill && selectedMove.target != null) {
			selectedMove.target!!
		} else if (selectedMove is BattleMoveSelectionItem && selectedMove.target != null) {
			BattleSkillTargetSingle(selectedMove.target!!)
		} else null
	}

	private val action = run {
		if (target == null || state !is BattleStateMachine.SelectMove) return@run null
		val selectedMove = state.selectedMove
		if (selectedMove is BattleMoveSelectionAttack) {
			Action("Attack", 0, state.onTurn.getEquipment(context.updateContext)[0]!!.element!!)
		} else if (selectedMove is BattleMoveSelectionSkill && selectedMove.target != null) {
			var manaCost = selectedMove.skill!!.manaCost
			if (target is BattleSkillTargetAllAllies || target is BattleSkillTargetAllEnemies) manaCost *= 2
			Action(selectedMove.skill!!.name, manaCost, selectedMove.skill!!.element)
		} else if (selectedMove is BattleMoveSelectionItem && selectedMove.target != null) {
			Action(selectedMove.item!!.flashName, 0, state.onTurn.getEquipment(context.updateContext)[0]!!.element!!)
		} else throw IllegalStateException()
	}

	private lateinit var kimBatch: KimBatch
	private lateinit var batch2: KimBatch

	fun beforeRendering() {
		if (target == null) return

		kimBatch = context.resources.kim1Renderer.startBatch()

		val pointer = context.content.ui.horizontalPointer
		val pointerScale = 0.05f * region.height / pointer.height
		val pointerOffsetX = run {
			val passedTime = System.nanoTime() - context.battle.startTime
			val period = 750_000_000L
			val relevantTime = passedTime % period
			val relativeOffset = 2f * abs(period / 2 - relevantTime) / period.toFloat()
			(0.01f * relativeOffset * region.width).roundToInt()
		}
		for ((index, position) in context.battle.playerLayout.positions.withIndex()) {
			var isTargeted = false
			if (target is BattleSkillTargetAllAllies) isTargeted = context.battle.players[index]?.isAlive() == true
			if (target is BattleSkillTargetSingle) isTargeted = target.target === context.battle.players[index]
			if (!isTargeted) continue

			val floatCoordinates = transformBattleCoordinates(position, 1f, context.targetImage)
			kimBatch.requests.add(KimRequest(
				x = floatCoordinates.intX(context.targetImage.width) - (pointerScale * pointer.width).roundToInt() + pointerOffsetX,
				y = floatCoordinates.intY(context.targetImage.height) - (pointerScale * pointer.height / 2).roundToInt(),
				scale = pointerScale, sprite = pointer
			))
		}

		for ((index, position) in context.battle.battle.enemyLayout.positions.withIndex()) {
			var isTargeted = false
			if (target is BattleSkillTargetAllEnemies) isTargeted = context.battle.opponents[index]?.isAlive() == true
			if (target is BattleSkillTargetSingle) isTargeted = target.target === context.battle.opponents[index]
			if (!isTargeted) continue

			val floatCoordinates = transformBattleCoordinates(position, -1f, context.targetImage)
			kimBatch.requests.add(KimRequest(
				x = floatCoordinates.intX(context.targetImage.width) - pointerOffsetX,
				y = floatCoordinates.intY(context.targetImage.height) - (pointerScale * pointer.height / 2).roundToInt(),
				scale = pointerScale, sprite = context.content.ui.flippedPointer
			))
		}

		run {
			val text = context.content.ui.targetingMode
			val width = region.width / 4
			val scale = width.toFloat() / text.width
			kimBatch.requests.add(KimRequest(
				x = region.maxX - width - region.width / 50,
				y = region.minY + region.height / 50,
				scale = scale, sprite = text
			))
		}

		run {
			batch2 = context.resources.kim2Renderer.startBatch()
			val elementSprite = action!!.element.sprite
			val desiredHeight = region.height / 15f
			batch2.requests.add(KimRequest(
				x = elementX, y = elementY,
				scale = desiredHeight / elementSprite.height,
				sprite = elementSprite
			))
		}
	}

	fun render() {
		if (target == null || state !is BattleStateMachine.SelectMove) return

		context.uiRenderer.beginBatch()
		val backgroundColor = rgba(0, 0, 0, 100)
		val minY = elementY + region.height / 100
		val maxY = elementY + region.height / 20 + region.height / 100
		context.uiRenderer.fillColor(
			region.minX + region.width / 50, minY,
			region.minX + region.width / 4 + region.width / 50, maxY, backgroundColor
		)
		val textColor = srgbToLinear(rgb(238, 203, 127))
		context.uiRenderer.drawString(
			context.resources.font, "MP Cost:", textColor, IntArray(0),
			region.minX + region.width / 30, minY, region.minX + region.width / 4, maxY,
			minY + 6 * (maxY - minY) / 7, 2 * (maxY - minY) / 3, 1, TextAlignment.LEFT
		)
		val manaString = if (action!!.manaCost > 0) action.manaCost.toString() else "-"
		val manaColor = if (action.manaCost > state.onTurn.currentMana) srgbToLinear(rgb(254, 81, 81))
		else srgbToLinear(rgb(50, 203, 254))
		context.uiRenderer.drawString(
			context.resources.font, manaString, manaColor, IntArray(0),
			region.minX + region.width / 5, region.minY, region.minX + region.width / 4, maxY,
			minY + 6 * (maxY - minY) / 7, maxY - minY, 1, TextAlignment.LEFT
		)
		context.uiRenderer.fillColor(
			elementX - region.width / 50, minY, elementX + region.width / 4, maxY, backgroundColor
		)
		context.uiRenderer.drawString(
			context.resources.font, action.name, textColor, IntArray(0),
			elementX + region.height / 15 + region.width / 50, minY, region.maxX, maxY,
			minY + 6 * (maxY - minY) / 7, 2 * (maxY - minY) / 3, 1, TextAlignment.LEFT
		)
		context.uiRenderer.endBatch()

		context.resources.kim1Renderer.submit(kimBatch, context.recorder, context.targetImage)
		context.resources.kim2Renderer.submit(batch2, context.recorder, context.targetImage)
	}
}

private class Action(val name: String, val manaCost: Int, val element: Element)
