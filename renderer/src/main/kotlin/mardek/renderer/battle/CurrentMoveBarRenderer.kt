package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dGlyphBatch
import com.github.knokko.vk2d.batch.Vk2dImageBatch
import com.github.knokko.vk2d.batch.Vk2dKim3Batch
import com.github.knokko.vk2d.text.TextAlignment
import mardek.state.ingame.battle.BattleStateMachine
import mardek.state.util.Rectangle
import kotlin.math.max

internal fun renderCurrentMoveBar(
	battleContext: BattleRenderContext, colorBatch: Vk2dColorBatch,
	spriteBatch: Vk2dKim3Batch, imageBatch: Vk2dImageBatch, textBatch: Vk2dGlyphBatch, region: Rectangle
) {
	battleContext.run {
		val stateMachine = battle.state
		val currentSkill = when (stateMachine) {
			is BattleStateMachine.MeleeAttack -> stateMachine.skill
			is BattleStateMachine.BreathAttack -> stateMachine.skill
			is BattleStateMachine.CastSkill -> stateMachine.skill
			else -> null
		}
		val currentItem = if (stateMachine is BattleStateMachine.UseItem) stateMachine.item else null

		if (currentSkill == null && currentItem == null) return

		if (currentSkill != null) {
			val sprite = currentSkill.element.thickSprite
			imageBatch.simpleScale(
				region.minX + 0.25f * region.width, region.minY.toFloat(),
				region.height.toFloat() / sprite.height, sprite.index,
			)
		}
		if (currentItem != null) {
			spriteBatch.simple(
				region.minX + region.width / 4, region.minY,
				region.height.toFloat() / currentItem.sprite.height,
				currentItem.sprite.index,
			)
		}

		val lightBottomColor = srgbToLinear(rgba(80, 65, 55, 220))
		val lightTopColor = srgbToLinear(rgba(120, 110, 110, 220))
		val lightRightColor = srgbToLinear(rgba(130, 110, 70, 220))
		val darkLeftColor = srgbToLinear(rgba(38, 32, 32, 220))
		val darkRightColor = srgbToLinear(rgba(100, 90, 50, 220))
		val midY = region.minY + region.height / 2
		val borderHeight = max(1, region.height / 20)
		colorBatch.fill(
			region.minX, region.minY, region.maxX, region.minY + borderHeight - 1,
			srgbToLinear(rgb(208, 193, 142)),
		)
		colorBatch.fill(
			region.minX, region.maxY - borderHeight, region.maxX, region.maxY,
			srgbToLinear(rgb(208, 193, 142)),
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
		val textColor = srgbToLinear(rgb(238, 203, 127))
		val name = currentSkill?.name ?: currentItem!!.displayName
		val font = context.bundle.getFont(context.content.fonts.fat.index)
		textBatch.drawString(
			name, textX, region.maxY - region.height * 0.25f, 0.45f * region.height,
			font, textColor, rgba(0f, 0f, 0f, 0.5f),
			0.07f * region.height, TextAlignment.LEFT,
		)
	}
}
