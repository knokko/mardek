package mardek.renderer.area

import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear

internal fun renderAreaLights(areaContext: AreaRenderContext) {
	areaContext.apply {
		val lightBatch = context.addAreaLightBatch(scissor)
		val lightRadius = 24 * scale

		val minLightX = cameraX - region.width / 2 - lightRadius
		val maxLightX = cameraX + region.width / 2 + lightRadius
		val minLightY = cameraY - region.height / 2 - lightRadius
		val maxLightY = cameraY + region.height / 2 + lightRadius
		for (decoration in state.area.objects.decorations) {
			val light = decoration.light ?: continue
			val x = 16 * scale * decoration.x + 8 * scale
			val y = 16 * scale * decoration.y + scale * light.offsetY
			if (x in minLightX .. maxLightX && y in minLightY .. maxLightY) {
				lightBatch.draw(
					region.minX + x + region.width / 2 - cameraX - lightRadius,
					region.minY + y + region.height / 2 - cameraY - lightRadius,
					lightRadius, srgbToLinear(light.color),
				)
			}
		}
	}
}
