package mardek.renderer.battle

import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import com.github.knokko.boiler.utilities.ColorPacker.*
import com.github.knokko.text.placement.TextAlignment
import com.github.knokko.ui.renderer.Gradient
import mardek.content.Content
import mardek.content.characters.PlayableCharacter
import mardek.content.sprite.KimSprite
import mardek.renderer.SharedResources
import mardek.renderer.batch.KimBatch
import mardek.renderer.batch.KimRequest
import mardek.state.ingame.battle.BattleMoveSelectionItem
import mardek.state.ingame.battle.BattleMoveSelectionSkill
import mardek.state.ingame.battle.BattleState
import mardek.state.ingame.characters.CharacterState
import mardek.state.title.AbsoluteRectangle
import kotlin.math.max
import kotlin.math.roundToInt

class SkillOrItemSelectionRenderer(
	private val content: Content,
	private val resources: SharedResources,
	frameIndex: Int,
	private val battle: BattleState,
	private val characterStates: Map<PlayableCharacter, CharacterState>,
	private val region: AbsoluteRectangle,
	private val recorder: CommandRecorder,
	private val targetImage: VkbImage,
) {

	private val uiRenderer = resources.uiRenderers[frameIndex]
	private val selectedMove = battle.selectedMove
	private val marginX = region.width / 15
	private val entryHeight = region.height / 15

	private val entries = run {
		if (selectedMove is BattleMoveSelectionSkill && selectedMove.skill != null && selectedMove.target == null) {
			val player = battle.players[battle.onTurn!!.index]!!
			val playerState = characterStates[player]!!
			val skills = player.characterClass.skillClass.actions.filter { playerState.canCastSkill(it) }
			skills.map { skill -> Entry(
				icon = skill.element.sprite, name = skill.name,
				rightNumber = if (skill.manaCost > 0) skill.manaCost else null,
				isSelected = selectedMove.skill === skill
			) }
		} else if (selectedMove is BattleMoveSelectionItem && selectedMove.item != null && selectedMove.target == null) {
			val player = battle.players[battle.onTurn!!.index]!!
			val playerState = characterStates[player]!!
			val itemSet = playerState.inventory.filter {
				it != null && it.item.consumable != null
			}.mapNotNull { it!!.item }.toSet()
			itemSet.map { item -> Entry(
				icon = item.sprite, name = item.flashName,
				rightNumber = playerState.inventory.sumOf { if (it != null && it.item === item) it.amount else 0 },
				isSelected = selectedMove.item === item
			) }
		} else emptyList()
	}

	private lateinit var batch1: KimBatch
	private lateinit var batch2: KimBatch

	private fun backgroundColor(alpha: Int) = srgbToLinear(rgba(82, 62, 37, alpha))

	fun beforeRendering() {
		if (entries.isEmpty()) return

		batch1 = resources.kim1Renderer.startBatch()
		if (entries.any { it.icon.version == 2 }) batch2 = resources.kim2Renderer.startBatch()

		var minY = region.minY + region.height / 20
		for (entry in entries) {
			val spriteRequest = KimRequest(
				x = region.minX + marginX, y = minY,
				scale = entryHeight.toFloat() / entry.icon.height.toFloat(),
				sprite = entry.icon, opacity = 1f
			)
			if (entry.icon.version == 1) batch1.requests.add(spriteRequest) else batch2.requests.add(spriteRequest)
			if (entry.isSelected) {
				val pointer = content.ui.horizontalPointer
				val scale = 0.9f * entryHeight / pointer.height.toFloat()
				val width = scale * pointer.width
				val x = region.minX + marginX / 3 - width
				batch1.requests.add(KimRequest(
					x = x.roundToInt(), y = minY + entryHeight / 10, scale = scale, sprite = pointer, opacity = 1f
				))
			}
			minY += region.height / 12
		}
	}

	fun render() {
		if (entries.isEmpty()) return

		val borderColor = srgbToLinear(rgb(208, 193, 142))
		val borderWidth = max(1, region.width / 100)
		uiRenderer.beginBatch()
		uiRenderer.fillColor(region.minX, region.minY, region.maxX, region.maxY, 0, Gradient(
			0, 0, region.width, region.height,
			backgroundColor(240), backgroundColor(210), backgroundColor(240)
		))
		uiRenderer.fillColor(
			region.minX + borderWidth, region.minY + borderWidth,
			region.maxX - borderWidth, region.minY + 2 * borderWidth - 1, borderColor
		)
		uiRenderer.fillColor(
			region.minX + borderWidth, region.maxY - 2 * borderWidth + 1,
			region.maxX - borderWidth, region.maxY - borderWidth, borderColor
		)
		uiRenderer.fillColor(
			region.minX + borderWidth, region.minY + borderWidth,
			region.minX + 2 * borderWidth - 1, region.maxY - borderWidth, borderColor
		)
		uiRenderer.fillColor(
			region.maxX - 2 * borderWidth + 1, region.minY + borderWidth,
			region.maxX - borderWidth, region.maxY - borderWidth, borderColor
		)

		var minY = region.minY + region.height / 20
		for (entry in entries) {
			var darkColor = rgba(6, 3, 0, 150)
			var textColor = srgbToLinear(rgb(236, 200, 126))
			if (entry.isSelected) {
				darkColor = rgba(6, 15, 20, 150)
				textColor = srgbToLinear(rgb(157, 195, 243))
			}

			uiRenderer.fillColor(
				region.minX + marginX + entryHeight / 2, minY,
				region.maxX - marginX - entryHeight / 2, minY + entryHeight, darkColor
			)
			uiRenderer.fillCircle(
				region.minX + marginX, minY,
				region.minX + marginX + entryHeight, minY + entryHeight, darkColor
			)
			uiRenderer.fillCircle(
				region.maxX - marginX - entryHeight, minY,
				region.maxX - marginX, minY + entryHeight, darkColor
			)
			uiRenderer.drawString(
				resources.font, entry.name, textColor, IntArray(0),
				region.minX + 3 * marginX / 2 + entryHeight, region.minY, region.maxX, region.maxY,
				minY + 4 * entryHeight / 5, 2 * entryHeight / 3, 1, TextAlignment.LEFT
			)
			if (entry.rightNumber != null) {
				uiRenderer.drawString(
					resources.font, entry.rightNumber.toString(), textColor, IntArray(0),
					region.minX, region.minY, region.maxX - marginX - entryHeight / 2, region.maxY,
					minY + 4 * entryHeight / 5, 2 * entryHeight / 3, 1, TextAlignment.RIGHT
				)
			}
			minY += region.height / 12
		}
		uiRenderer.endBatch()

		resources.kim1Renderer.submit(batch1, recorder, targetImage)
		if (this::batch2.isInitialized) resources.kim2Renderer.submit(batch2, recorder, targetImage)
	}
}

private class Entry(val icon: KimSprite, val name: String, val rightNumber: Int?, val isSelected: Boolean)
