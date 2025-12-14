package mardek.renderer.area

import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear

internal fun renderAreaAmbience(areaContext: AreaRenderContext) {
	areaContext.apply {
		val ambience = context.campaign.story.evaluate(area.properties.ambience)
		if (ambience.addColor != 0) {
			// TODO CHAP2 Implement this: use dual source blending for this?
			throw UnsupportedOperationException("Ambience addColor is not yet supported")
		}
		if (ambience.multiplyColor != -1) {
			multiplyBatch.fill(
				region.minX, region.minY, region.maxX, region.maxY,
				srgbToLinear(ambience.multiplyColor),
			)
		}
		if (ambience.subtractColor != 0) { // TODO CHAP3
			throw UnsupportedOperationException("Ambience subtractColor is not yet supported")
		}
	}
}
