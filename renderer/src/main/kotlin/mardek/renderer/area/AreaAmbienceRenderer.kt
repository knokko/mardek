package mardek.renderer.area

import com.github.knokko.boiler.utilities.ColorPacker.interpolateColors
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import mardek.content.action.ActionChangeAmbience
import mardek.content.action.FixedActionNode
import mardek.content.animation.ColorTransform
import mardek.state.ingame.area.AreaSuspensionActions
import kotlin.time.Duration

internal fun renderAreaAmbience(areaContext: AreaRenderContext) {
	areaContext.apply {
		var ambience = context.campaign.story.evaluate(
			area.properties.ambience, context.campaign.expressionContext()
		)

		val suspension = state.suspension
		if (suspension is AreaSuspensionActions) {
			val defaultAmbience = ambience
			ambience = suspension.actions.overrideAmbience ?: ambience
			val node = suspension.actions.node
			if (node is FixedActionNode && suspension.actions.currentNodeStartTime >= Duration.ZERO) {
				val action = node.action
				if (action is ActionChangeAmbience && action.transitionTime > Duration.ZERO) {
					val mixer = ((state.currentTime - suspension.actions.currentNodeStartTime) / action.transitionTime).toFloat()
					val nextAmbience = action.overrideAmbience ?: defaultAmbience
					ambience = ColorTransform(
						addColor = interpolateColors(ambience.addColor, nextAmbience.addColor, mixer),
						multiplyColor = interpolateColors(ambience.multiplyColor, nextAmbience.multiplyColor, mixer),
						subtractColor = interpolateColors(ambience.subtractColor, nextAmbience.subtractColor, mixer),
					)
				}
			}
		}

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
