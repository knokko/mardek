package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.changeAlpha
import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dImageBatch
import com.github.knokko.vk2d.text.TextAlignment
import mardek.renderer.glyph.MardekGlyphBatch
import mardek.state.ingame.battle.CombatantState
import mardek.state.ingame.battle.StatusEffectHistory
import kotlin.math.pow
import kotlin.math.roundToInt

internal fun renderEffectHistory(
	battleContext: BattleRenderContext, combatant: CombatantState,
	imageBatch: Vk2dImageBatch, textBatch: MardekGlyphBatch, lateColorBatch: Vk2dColorBatch,
) {
	battleContext.run {
		val currentEntry = combatant.renderInfo.effectHistory.get(renderTime) ?: return
		val midX = combatant.renderInfo.core.x
		var midY = combatant.renderInfo.core.y

		if (currentEntry.type == StatusEffectHistory.Type.Remove) {
			midY -= currentEntry.relativeTime * imageBatch.height / 20f

			val opacity = 1f - 4f * (0.5f - currentEntry.relativeTime).pow(2)
			val backgroundSpriteSize = imageBatch.height / 12f
			val backgroundSprite = context.content.ui.statusRemoveBackground
			imageBatch.coloredScale(
				midX - backgroundSpriteSize * 0.5f, midY - backgroundSpriteSize * 0.5f,
				backgroundSpriteSize / backgroundSprite.height, backgroundSprite.index,
				0, rgba(1f, 1f, 1f, opacity)
			)

			val mainSpriteSize = imageBatch.height / 20f
			val mainSprite = currentEntry.effect.icon
			imageBatch.coloredScale(
				midX - mainSpriteSize * 0.5f, midY - mainSpriteSize * 0.5f,
				mainSpriteSize / mainSprite.height, mainSprite.index,
				0, rgba(1f, 1f, 1f, opacity)
			)

			val crossColor = srgbToLinear(rgba(199, 0, 0, 128))
			val l = imageBatch.height / 28
			val w = imageBatch.height / 250
			val p = (l - 2 * l * (1f - currentEntry.relativeTime)).roundToInt()

			val iMidX = midX.roundToInt()
			val iMidY = midY.roundToInt()
			lateColorBatch.fillUnaligned(
				iMidX - l + w, iMidY - l - w, iMidX - l - w, iMidY - l + w,
				iMidX + p - w, iMidY + p + w, iMidX + p + w, iMidY + p - w, crossColor
			)
			lateColorBatch.fillUnaligned(
				iMidX + l - w, iMidY - l - w, iMidX + l + w, iMidY - l + w,
				iMidX - p + w, iMidY + p + w, iMidX - p - w, iMidY + p - w, crossColor
			)
		} else {
			val f = currentEntry.relativeTime.pow(2)
			midY -= f * imageBatch.height / 20f

			val alpha = 255 - (250 * f).roundToInt()
			val innerColor = changeAlpha(srgbToLinear(currentEntry.effect.innerTextColor), alpha)
			val outerColor = changeAlpha(srgbToLinear(currentEntry.effect.outerTextColor), alpha)
			val effectFont = context.bundle.getFont(context.content.fonts.basic1.index)

			textBatch.drawFancyString(
				currentEntry.effect.shortName, midX, midY, imageBatch.height / 30f, effectFont,
				outerColor, rgb(0, 0, 0), imageBatch.height / 200f,
				TextAlignment.CENTERED, outerColor, innerColor, innerColor,
				outerColor, 0.2f, 0.2f, 0.8f, 0.8f,
			)
		}
	}
}
