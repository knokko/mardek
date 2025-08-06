package mardek.renderer.util

import com.github.knokko.vk2d.batch.Vk2dColorBatch

internal fun gradientWithBorder(
	batch: Vk2dColorBatch, minX: Int, minY: Int, maxX: Int, maxY: Int, borderWidth: Int, borderHeight: Int,
	borderColor: Int, baseColor: Int, rightColor: Int, upColor: Int,
) {
	batch.fill(minX, minY, maxX, minY + borderHeight - 1, borderColor)
	batch.fill(minX, maxY + 1 - borderHeight, maxX, maxY, borderColor)
	batch.fill(minX, minY, minX + borderWidth - 1, maxY, borderColor)
	batch.fill(maxX + 1 - borderWidth, minY, maxX, maxY, borderColor)
	batch.gradient(
		minX + borderWidth, minY + borderHeight,
		maxX - borderWidth, maxY - borderHeight,
		baseColor, rightColor, upColor
	)
}
