package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.changeAlpha
import com.github.knokko.boiler.utilities.ColorPacker.multiplyColors
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
		var midX = combatant.renderInfo.statusEffectPoint.x
		var midY = combatant.renderInfo.statusEffectPoint.y

		if (currentEntry.type == StatusEffectHistory.Type.Remove) {
			midY -= currentEntry.relativeTime * imageBatch.height / 20f
			val spriteSize = imageBatch.height / 20f
			val sprite = currentEntry.effect.icon
			val opacity = 1f - 4f * (0.5f - currentEntry.relativeTime).pow(2)
			imageBatch.coloredScale(
				midX - spriteSize * 0.5f, midY - spriteSize * 0.5f,
				spriteSize / sprite.height, sprite.index,
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

			val strongColor = changeAlpha(
				srgbToLinear(currentEntry.effect.textColor),
				255 - (250 * f).roundToInt()
			)
			val weakColor = multiplyColors(strongColor, rgb(0.7f, 0.7f, 0.7f))
			val unknownFont = context.bundle.getFont(context.content.fonts.basic2.index)

			// TODO Check this & fix weakColor
			textBatch.drawFancyString(
				currentEntry.effect.shortName, midX, midY, imageBatch.height / 30f, unknownFont,
				weakColor, rgb(0, 0, 0), imageBatch.height / 150f,
				TextAlignment.CENTERED, weakColor, strongColor, strongColor,
				weakColor, 0.2f, 0.2f, 0.8f, 0.8f,
			)
//			context.uiRenderer.drawString(
//				context.resources.font, currentEntry.effect.shortName, color, IntArray(0),
//				midX - width / 5, 0, midX + width / 5, height,
//				midY, height / 30, 1, TextAlignment.CENTER
//			)
		}
	}
}
