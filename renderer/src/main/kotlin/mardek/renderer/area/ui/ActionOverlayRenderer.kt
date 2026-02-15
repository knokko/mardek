package mardek.renderer.area.ui

import com.github.knokko.boiler.utilities.ColorPacker.alpha
import com.github.knokko.boiler.utilities.ColorPacker.interpolateColors
import mardek.content.action.ActionSetOverlayColor
import mardek.content.action.FixedActionNode
import mardek.renderer.area.AreaRenderContext
import mardek.state.ingame.area.AreaSuspensionActions
import kotlin.math.min

internal fun renderActionOverlayColor(areaContext: AreaRenderContext) {
	areaContext.run {
		val suspension = state.suspension
		if (suspension !is AreaSuspensionActions) return

		var overlayColor = suspension.actions.overlayColor
		val node = suspension.actions.node
		if (node is FixedActionNode) {
			val action = node.action
			if (action is ActionSetOverlayColor) {
				val progress = min(
					1.0,
					(state.currentTime - suspension.actions.startOverlayTransitionTime) / action.transitionTime,
				)
				overlayColor = interpolateColors(overlayColor, action.color, progress.toFloat())
			}
		}

		if (alpha(overlayColor) != 0.toByte()) {
			context.addColorBatch(2).fill(
				region.minX, region.minY, region.maxX, region.maxY, overlayColor
			)
		}
	}
}
