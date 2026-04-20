package mardek.renderer.area

import com.github.knokko.boiler.utilities.ColorPacker.alpha
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.vk2d.batch.Vk2dOvalBatch
import mardek.content.action.ActionMoveAreaEffect
import mardek.content.action.FixedActionNode
import mardek.content.action.effect.AreaRingEffect
import mardek.content.action.effect.AreaEffectsEmitter
import mardek.state.ingame.area.AreaSuspensionActions
import kotlin.time.Duration

internal fun renderAreaActionEffects(areaContext: AreaRenderContext) {
	areaContext.apply {
		val suspension = state.suspension
		if (suspension !is AreaSuspensionActions) return
		val node = suspension.actions.node
		val moveAction = if (node is FixedActionNode) {
			when (val nodeAction = node.action) {
				is ActionMoveAreaEffect -> nodeAction
				else -> null
			}
		} else null

		val deltaTime = state.currentTime - suspension.actions.currentNodeStartTime
		for ((instance, oldState) in suspension.actions.effects) {
			var x = scale * oldState.x.toFloat()
			var y = scale * oldState.y.toFloat()
			if (moveAction != null) {
				val progress = (deltaTime / moveAction.duration).toFloat()
				x = (1f - progress) * x + progress * scale * moveAction.destinationX
				y = (1f - progress) * y + progress * scale * moveAction.destinationY
			}

			val renderX = region.minX + x + region.width / 2 - cameraX
			val renderY = region.minY + y + region.height / 2 - cameraY
			val context = EffectRenderContext(ovalBatch, renderX, renderY, scale)

			for (emitter in instance.effect.emitters) {
				renderEmitter(emitter, state.currentTime - oldState.spawnTime, context)
			}
		}
	}
}

private class EffectRenderContext(
	val batch: Vk2dOvalBatch,
	val x: Float,
	val y: Float,
	val scale: Int,
)

private fun renderEmitter(emitter: AreaEffectsEmitter, timeSinceSpawn: Duration, context: EffectRenderContext) {
	for (dispatch in emitter.getRelevantDispatches(timeSinceSpawn)) {
		val dispatchedAt = emitter.firstDispatchAfter + emitter.period * dispatch
		val timeSinceDispatch = timeSinceSpawn - dispatchedAt

		for (ring in emitter.rings) {
			renderRingEffect(ring, timeSinceDispatch, context)
		}
	}
}

private fun renderRingEffect(
	ring: AreaRingEffect, timeSinceDispatch: Duration, context: EffectRenderContext
) {
	val innerRadius = context.scale * ring.innerBorder.radius.getNonNegative(timeSinceDispatch)
	val outerRadius = context.scale * ring.outerBorder.radius.getNonNegative(timeSinceDispatch)
	if (innerRadius <= 0f && outerRadius <= 0f) return
	if (innerRadius > outerRadius) return
	val safeRadius = outerRadius + 2f

	val innerColor = getColor(ring.innerBorder, timeSinceDispatch)
	val outerColor = getColor(ring.outerBorder, timeSinceDispatch)
	if (alpha(innerColor) == 0.toByte() && alpha(outerColor) == 0.toByte()) return

	context.batch.complex(
		(context.x - safeRadius).toInt(), (context.y - safeRadius).toInt(),
		(context.x + safeRadius).toInt(), (context.y + safeRadius).toInt(),
		context.x, context.y, outerRadius, outerRadius, 0,
		0, innerColor, outerColor, 0,
		innerRadius / outerRadius, innerRadius / outerRadius, 1f, 1f,
	)
}

private fun getColor(border: AreaRingEffect.Border, timeSinceDispatch: Duration) = rgba(
	border.red.getColorComponent(timeSinceDispatch),
	border.green.getColorComponent(timeSinceDispatch),
	border.blue.getColorComponent(timeSinceDispatch),
	border.alpha.getColorComponent(timeSinceDispatch)
)
