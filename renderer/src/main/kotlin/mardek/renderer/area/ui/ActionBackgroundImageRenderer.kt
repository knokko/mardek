package mardek.renderer.area.ui

import mardek.renderer.area.AreaRenderContext
import mardek.state.ingame.area.AreaSuspensionActions

internal fun renderActionBackgroundImage(areaContext: AreaRenderContext) {
	areaContext.run {
		val suspension = state.suspension
		if (suspension !is AreaSuspensionActions) return

		val backgroundImage = suspension.actions.backgroundImage ?: return

		actionsImageBatch.fillWithoutDistortion(
			region.minX.toFloat(), region.minY.toFloat(),
			region.boundX.toFloat(), region.boundY.toFloat(),
			backgroundImage.sprite.index,
		)
	}
}
