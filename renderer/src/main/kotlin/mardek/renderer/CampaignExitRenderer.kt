package mardek.renderer

import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dSimpleTextBatch
import mardek.renderer.area.ui.CHOICE_CHAR
import mardek.renderer.area.ui.renderDialogueLines
import mardek.state.ingame.ConsiderCampaignExit
import mardek.state.util.Rectangle
import kotlin.math.roundToInt

internal fun renderCampaignExitModal(
	consider: ConsiderCampaignExit, opacity: Float, context: RenderContext, region: Rectangle
): Pair<Vk2dColorBatch, Vk2dSimpleTextBatch> {
	val textBatch = context.addTextBatch(500)
	val font = context.bundle.getFont(context.content.fonts.basic2.index)
	val textAlpha = (opacity * 255f).roundToInt()
	renderDialogueLines(
		"Do you want to Quit?\n$CHOICE_CHAR Press \$ESC% again to return to the Title Screen.\n$CHOICE_CHAR Press \$Q% to return to the game.",
		123456f,
		region.minX + 0.3f * region.height, region.minX + 0.3f * region.height,
		region.maxX.toFloat(),
		region.minY + 0.42f * region.height, region.maxY.toFloat(),
		0.025f * region.height, 0f, 0.08f * region.height,
		textBatch, font,
		MardekTextStyles.ExitCampaign.base(textAlpha),
		MardekTextStyles.ExitCampaign.bold(textAlpha),
		MardekTextStyles.ExitCampaign.shadow(textAlpha),
	)

	val colorBatch = context.addColorBatch(38)

	val confirmTime = consider.confirmedAt
	if (confirmTime != null) {
		val fadeAlpha = context.timing.interpolate(
			confirmTime, 0,
			ConsiderCampaignExit.EXIT_FADE_OUT, 255, true
		)
		if (fadeAlpha > 0) {
			val fadeColor = rgba(0, 0, 0, fadeAlpha)
			colorBatch.fill(region.minX, region.minY, region.maxX, region.maxY, fadeColor)
		}
	}

	return Pair(colorBatch, textBatch)
}
