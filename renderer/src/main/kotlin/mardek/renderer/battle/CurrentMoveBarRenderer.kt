package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dImageBatch
import com.github.knokko.vk2d.text.TextAlignment
import mardek.renderer.area.AreaSpriteBatch
import mardek.renderer.glyph.MardekGlyphBatch
import mardek.renderer.util.ResourceBarRenderer
import mardek.renderer.util.ResourceType
import mardek.renderer.util.renderFancyMasteredText
import mardek.state.ingame.battle.BattleStateMachine
import mardek.state.ingame.battle.PlayerCombatantState
import mardek.state.util.Rectangle
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal fun renderCurrentMoveBar(
	battleContext: BattleRenderContext, colorBatch: Vk2dColorBatch,
	spriteBatch: AreaSpriteBatch, imageBatch: Vk2dImageBatch, textBatch: MardekGlyphBatch, region: Rectangle
) {
	battleContext.run {
		val stateMachine = battle.state
		val (currentSkill, caster) = when (stateMachine) {
			is BattleStateMachine.MeleeAttack -> Pair(stateMachine.skill, stateMachine.attacker)
			is BattleStateMachine.BreathAttack -> Pair(stateMachine.skill, stateMachine.attacker)
			is BattleStateMachine.CastSkill -> Pair(stateMachine.skill, stateMachine.caster)
			else -> Pair(null, null)
		}
		val currentItem = if (stateMachine is BattleStateMachine.UseItem) stateMachine.item else null

		if (currentSkill == null && currentItem == null) return

		var opacity = 1f
		val stateStartTime = when (stateMachine) {
			is BattleStateMachine.MeleeAttack.MoveTo -> stateMachine.startTime
			is BattleStateMachine.BreathAttack.MoveTo -> stateMachine.startTime
			is BattleStateMachine.CastSkill -> stateMachine.startTime
			is BattleStateMachine.UseItem -> stateMachine.startTime
			else -> null
		}
		if (stateStartTime != null) {
			val passedTime = renderTime - stateStartTime
			val fadeInTime = 500_000_000L
			opacity = min(passedTime.toFloat() / fadeInTime, 1f)
		}
		val alpha = (255f * opacity).roundToInt()

		if (currentSkill != null) {
			val sprite = currentSkill.element.thickSprite
			imageBatch.coloredScale(
				region.minX + 0.25f * region.width, region.minY.toFloat(),
				region.height.toFloat() / sprite.height, sprite.index,
				0, rgba(1f, 1f, 1f, opacity),
			)
		}
		if (currentItem != null) {
			spriteBatch.draw(
				currentItem.sprite, region.minX + region.width / 4, region.minY,
				region.height.toFloat() / currentItem.sprite.height,
				0, opacity
			)
		}

		val weakerAlpha = (220f * opacity).roundToInt()
		val lightBottomColor = srgbToLinear(rgba(80, 65, 55, weakerAlpha))
		val lightTopColor = srgbToLinear(rgba(120, 110, 110, weakerAlpha))
		val lightRightColor = srgbToLinear(rgba(130, 110, 70, weakerAlpha))
		val darkLeftColor = srgbToLinear(rgba(38, 32, 32, weakerAlpha))
		val darkRightColor = srgbToLinear(rgba(100, 90, 50, weakerAlpha))
		val midY = region.minY + region.height / 2
		val borderHeight = max(1, region.height / 20)
		colorBatch.fill(
			region.minX, region.minY, region.maxX, region.minY + borderHeight - 1,
			srgbToLinear(rgba(208, 193, 142, alpha)),
		)
		colorBatch.fill(
			region.minX, region.maxY - borderHeight, region.maxX, region.maxY,
			srgbToLinear(rgba(208, 193, 142, alpha)),
		)
		colorBatch.gradient(
			region.minX, region.minY + borderHeight, region.maxX, midY - 1,
			lightBottomColor, lightRightColor, lightTopColor
		)
		colorBatch.gradient(
			region.minX, midY, region.maxX, region.maxY - borderHeight,
			darkLeftColor, darkRightColor, darkLeftColor
		)

		val textX = region.minX + region.width * 0.25f + 1.15f * region.height
		val textColor = srgbToLinear(rgba(238, 203, 127, alpha))
		val name = currentSkill?.name ?: currentItem!!.displayName
		val font = context.bundle.getFont(context.content.fonts.fat.index)
		textBatch.drawString(
			name, textX, region.maxY - region.height * 0.25f, 0.45f * region.height,
			font, textColor, rgba(0f, 0f, 0f, 0.5f * opacity),
			0.07f * region.height, TextAlignment.LEFT,
		)

		if (currentSkill != null && currentSkill.masteryPoints >= 0) {
			val barRegion = Rectangle(
				region.maxX - 3 * region.height, midY - region.height / 8,
				5 * region.height / 2, region.height / 3,
			)

			if (caster is PlayerCombatantState) {
				val currentMastery = state.characterStates[caster.player]!!.skillMastery[currentSkill] ?: 0
				if (currentMastery < currentSkill.masteryPoints) {
					val masteryBar = ResourceBarRenderer(context, ResourceType.SkillMastery, barRegion, colorBatch, textBatch)
					masteryBar.renderBar(currentMastery, currentSkill.masteryPoints, opacity)
					masteryBar.renderTextOverBar(currentMastery, currentSkill.masteryPoints, opacity)
				} else {
					renderFancyMasteredText(
						context, textBatch, barRegion.minX - 1.5f * barRegion.height,
						barRegion.maxY.toFloat(), barRegion.height * 1.3f, alpha,
					)
				}
			}
		}
	}
}
