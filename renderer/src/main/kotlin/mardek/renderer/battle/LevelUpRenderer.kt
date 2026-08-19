package mardek.renderer.battle

import com.github.knokko.vk2d.batch.Vk2dFancyTextBatch
import com.github.knokko.vk2d.text.TextAlignment
import mardek.renderer.MardekTextStyles
import mardek.state.util.Rectangle
import kotlin.math.roundToInt

internal fun renderLevelUps(battleContext: BattleRenderContext, textBatch: Vk2dFancyTextBatch, region: Rectangle) {
	battleContext.run {
		for (combatant in battle.allPlayers() + battle.allOpponents()) {
			for (indicator in combatant.renderInfo.levelUpHistory.get(battleContext.context.timing)) {
				val alpha = (indicator.opacity * 255f).roundToInt()
				val font = context.bundle.getFont(context.content.fonts.basic1.index)
				val renderPoint = combatant.renderInfo.hitPoint
				val renderY = renderPoint.y - 0.01f * region.height - 0.03f * region.height * indicator.relativeY
				val textScaleX = 1f + 2.5f * (1f - indicator.heightFactor)
				textBatch.drawString(
					"Level Up!", renderPoint.x, renderY, 0f,
					0.03f * region.height * indicator.heightFactor, font,
					MardekTextStyles.BattleIndicators.levelUp(alpha),
					TextAlignment.CENTERED, textScaleX,
				)
				textBatch.drawString(
					"Level ${indicator.amount}", renderPoint.x, renderY + 0.035f * region.height,
					0f, 0.025f * region.height * indicator.heightFactor, font,
					MardekTextStyles.BattleIndicators.newLevel(alpha),
					TextAlignment.LEFT, textScaleX,
				)
			}
		}
	}
}
