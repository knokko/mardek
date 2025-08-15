package mardek.renderer.area

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear

internal fun renderObtainedGold(areaContext: AreaRenderContext) {
	areaContext.run {
		val obtainedGold = state.obtainedGold ?: return
		val baseX = region.minX + tileSize * obtainedGold.chestX + region.width / 2 - cameraX
		val baseY = region.minY + tileSize * obtainedGold.chestY + region.height / 2 - cameraY - 4 * scale
		spriteBatch.draw(
			context.content.ui.goldIcon,
			baseX - tileSize * 19 / 32,
			baseY - tileSize * 17 / 32,
			scale / 2f
		)

		val font = context.bundle.getFont(context.content.fonts.basic1.index)
		textBatch.drawString(
			"+${state.obtainedGold!!.amount}", baseX, baseY - 1 * scale, 6 * scale,
			font, srgbToLinear(rgb(255, 204, 51)),
			srgbToLinear(rgb(53, 37, 22)), 1f * scale
		)
	}
}
