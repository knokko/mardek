package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.text.TextAlignment
import mardek.renderer.glyph.MardekGlyphBatch
import mardek.state.ingame.battle.LevelUpIndicator
import mardek.state.ingame.battle.PlayerCombatantState
import mardek.state.util.Rectangle
import kotlin.math.pow
import kotlin.math.roundToInt

internal fun renderLevelUps(battleContext: BattleRenderContext, textBatch: MardekGlyphBatch, region: Rectangle) {
	battleContext.run {
		for (combatant in battle.allPlayers() + battle.allOpponents()) {
			if (combatant !is PlayerCombatantState) continue

			val levelUp = combatant.lastLevelUp ?: continue
			var passedTime = renderTime - levelUp.startTime
			if (passedTime >= LevelUpIndicator.TOTAL_DURATION) continue

			var offsetY = 0f
			var alpha = 255
			if (passedTime < LevelUpIndicator.JUMP_DURATION) {
				val relativeTime = passedTime.toFloat() / LevelUpIndicator.JUMP_DURATION
				val fromMidTime = 0.5f - relativeTime
				offsetY = (0.25f - fromMidTime.pow(2)) * 0.1f * region.height
			}
			passedTime -= LevelUpIndicator.JUMP_DURATION + LevelUpIndicator.STABLE_DURATION
			if (passedTime > 0) {
				alpha = (255f * (1f - passedTime.toFloat() / LevelUpIndicator.FADE_DURATION)).roundToInt()
			}

			if (alpha > 0) {
				val fatFont = context.bundle.getFont(context.content.fonts.basic1.index)
				val renderPoint = combatant.renderInfo.statusEffectPoint
				val renderY = renderPoint.y - offsetY
				val outerColor = srgbToLinear(rgba(253, 235, 154, alpha))
				val innerColor = srgbToLinear(rgba(253, 252, 235, alpha))
				textBatch.drawFancyString(
					"Level Up!", renderPoint.x, renderY, 0.035f * region.height,
					fatFont, outerColor, rgba(0, 0, 0, alpha),
					0.005f * region.height, TextAlignment.CENTERED,
					outerColor, innerColor, innerColor, outerColor,
					0.2f, 0.2f, 0.8f, 0.8f,
				)
				textBatch.drawString(
					"Level ${levelUp.newLevel}", renderPoint.x, renderY + 0.035f * region.height,
					0.03f * region.height, fatFont,
					srgbToLinear(rgba(247, 216, 132, alpha)),
					rgba(0, 0, 0, alpha), 0.004f * region.height,
					TextAlignment.LEFT,
				)
			}
		}
	}
}
