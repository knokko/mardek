package mardek.renderer.battle

import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import com.github.knokko.boiler.utilities.ColorPacker.*
import com.github.knokko.text.placement.TextAlignment
import mardek.content.Content
import mardek.content.characters.PlayableCharacter
import mardek.content.stats.Element
import mardek.renderer.SharedResources
import mardek.renderer.batch.KimBatch
import mardek.renderer.batch.KimRequest
import mardek.state.ingame.battle.*
import mardek.state.ingame.characters.CharacterState
import mardek.state.title.AbsoluteRectangle
import kotlin.math.abs
import kotlin.math.roundToInt

class TargetSelectionRenderer(
	private val content: Content,
	private val battle: BattleState,
	private val characterStates: Map<PlayableCharacter, CharacterState>,
	private val resources: SharedResources,
	private val recorder: CommandRecorder,
	private val targetImage: VkbImage,
	frameIndex: Int,
	private val region: AbsoluteRectangle,
) {

	private val uiRenderer = resources.uiRenderers[frameIndex]
	private val elementX = region.maxX - region.width / 3
	private val elementY = region.maxY - region.height / 10

	private val target = run {
		val selectedMove = battle.selectedMove
		if (selectedMove is BattleMoveSelectionAttack && selectedMove.target != null) {
			BattleSkillTargetSingle(selectedMove.target!!)
		} else if (selectedMove is BattleMoveSelectionSkill && selectedMove.target != null) {
			selectedMove.target!!
		} else if (selectedMove is BattleMoveSelectionItem && selectedMove.target != null) {
			BattleSkillTargetSingle(selectedMove.target!!)
		} else null
	}

	private val action = run {
		if (target == null) return@run null
		val selectedMove = battle.selectedMove
		if (selectedMove is BattleMoveSelectionAttack) {
			val player = characterStates[battle.players[battle.onTurn!!.index]!!]!!
			Action("Attack", 0, player.equipment[0]!!.element!!)
		} else if (selectedMove is BattleMoveSelectionSkill && selectedMove.target != null) {
			var manaCost = selectedMove.skill!!.manaCost
			if (target is BattleSkillTargetAllAllies || target is BattleSkillTargetAllEnemies) manaCost *= 2
			Action(selectedMove.skill!!.name, manaCost, selectedMove.skill!!.element)
		} else if (selectedMove is BattleMoveSelectionItem && selectedMove.target != null) {
			val player = characterStates[battle.players[battle.onTurn!!.index]!!]!!
			Action(selectedMove.item!!.flashName, 0, player.equipment[0]!!.element!!)
		} else throw IllegalStateException()
	}

	private lateinit var kimBatch: KimBatch
	private lateinit var batch2: KimBatch

	fun beforeRendering() {
		if (target == null) return

		kimBatch = resources.kim1Renderer.startBatch()

		val pointer = content.ui.horizontalPointer
		val pointerScale = 0.05f * region.height / pointer.height
		val pointerOffsetX = run {
			val passedTime = System.nanoTime() - battle.startTime
			val period = 750_000_000L
			val relevantTime = passedTime % period
			val relativeOffset = 2f * abs(period / 2 - relevantTime) / period.toFloat()
			(0.01f * relativeOffset * region.width).roundToInt()
		}
		for ((index, position) in battle.playerLayout.positions.withIndex()) {
			var isTargeted = false
			if (target is BattleSkillTargetAllAllies) isTargeted = battle.playerStates[index] != null
			if (target is BattleSkillTargetSingle) isTargeted = target.target.isPlayer && index == target.target.index
			if (!isTargeted) continue

			val floatCoordinates = transformBattleCoordinates(position, 1f, targetImage)
			kimBatch.requests.add(KimRequest(
				x = floatCoordinates.intX(targetImage.width) - (pointerScale * pointer.width).roundToInt() + pointerOffsetX,
				y = floatCoordinates.intY(targetImage.height) - (pointerScale * pointer.height / 2).roundToInt(),
				scale = pointerScale, sprite = pointer, opacity = 1f
			))
		}

		for ((index, position) in battle.battle.enemyLayout.positions.withIndex()) {
			var isTargeted = false
			if (target is BattleSkillTargetAllEnemies) isTargeted = battle.enemyStates[index] != null
			if (target is BattleSkillTargetSingle) isTargeted = !target.target.isPlayer && index == target.target.index
			if (!isTargeted) continue

			val floatCoordinates = transformBattleCoordinates(position, -1f, targetImage)
			kimBatch.requests.add(KimRequest(
				x = floatCoordinates.intX(targetImage.width) - pointerOffsetX,
				y = floatCoordinates.intY(targetImage.height) - (pointerScale * pointer.height / 2).roundToInt(),
				scale = pointerScale, sprite = content.ui.flippedPointer, opacity = 1f
			))
		}

		run {
			val text = content.ui.targetingMode
			val width = region.width / 4
			val scale = width.toFloat() / text.width
			kimBatch.requests.add(KimRequest(
				x = region.maxX - width - region.width / 50,
				y = region.minY + region.height / 50,
				scale = scale, sprite = text, opacity = 1f
			))
		}

		run {
			batch2 = resources.kim2Renderer.startBatch()
			val elementSprite = action!!.element.sprite
			val desiredHeight = region.height / 15f
			batch2.requests.add(KimRequest(
				x = elementX, y = elementY,
				scale = desiredHeight / elementSprite.height,
				sprite = elementSprite, opacity = 1f
			))
		}
	}

	fun render() {
		if (target == null) return

		uiRenderer.beginBatch()
		val backgroundColor = rgba(0, 0, 0, 100)
		val minY = elementY + region.height / 100
		val maxY = elementY + region.height / 20 + region.height / 100
		uiRenderer.fillColor(
			region.minX + region.width / 50, minY,
			region.minX + region.width / 4 + region.width / 50, maxY, backgroundColor
		)
		val textColor = srgbToLinear(rgb(238, 203, 127))
		uiRenderer.drawString(
			resources.font, "MP Cost:", textColor, IntArray(0),
			region.minX + region.width / 30, minY, region.minX + region.width / 4, maxY,
			minY + 6 * (maxY - minY) / 7, 2 * (maxY - minY) / 3, 1, TextAlignment.LEFT
		)
		val manaString = if (action!!.manaCost > 0) action.manaCost.toString() else "-"
		val playerState = battle.playerStates[battle.onTurn!!.index]!!
		val manaColor = if (action.manaCost > playerState.currentMana) srgbToLinear(rgb(254, 81, 81))
		else srgbToLinear(rgb(50, 203, 254))
		uiRenderer.drawString(
			resources.font, manaString, manaColor, IntArray(0),
			region.minX + region.width / 5, region.minY, region.minX + region.width / 4, maxY,
			minY + 6 * (maxY - minY) / 7, maxY - minY, 1, TextAlignment.LEFT
		)
		uiRenderer.fillColor(
			elementX - region.width / 50, minY, elementX + region.width / 4, maxY, backgroundColor
		)
		uiRenderer.drawString(
			resources.font, action.name, textColor, IntArray(0),
			elementX + region.height / 15 + region.width / 50, minY, region.maxX, maxY,
			minY + 6 * (maxY - minY) / 7, 2 * (maxY - minY) / 3, 1, TextAlignment.LEFT
		)
		uiRenderer.endBatch()

		resources.kim1Renderer.submit(kimBatch, recorder, targetImage)
		resources.kim2Renderer.submit(batch2, recorder, targetImage)
	}
	// TODO Unit test
}

private class Action(val name: String, val manaCost: Int, val element: Element)
