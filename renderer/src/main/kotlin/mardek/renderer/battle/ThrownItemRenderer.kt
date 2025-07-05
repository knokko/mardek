package mardek.renderer.battle

import mardek.renderer.batch.KimBatch
import mardek.renderer.batch.KimRequest
import mardek.state.ingame.battle.BattleStateMachine
import kotlin.math.pow
import kotlin.math.roundToInt

class ThrownItemRenderer(private val context: BattleRenderContext) {

	private val state = context.battle.state
	private lateinit var batch: KimBatch

	fun beforeRendering() {
		if (state !is BattleStateMachine.UseItem) return

		val passedTime = System.nanoTime() - state.startTime
		val rotationPeriod = 1000_000_000L
		val relativeTime = (passedTime % rotationPeriod).toFloat() / rotationPeriod.toFloat()

		val itemX = relativeTime * state.target.lastRenderedPosition.first +
				(1f - relativeTime) * state.thrower.lastRenderedPosition.first
		var itemY = relativeTime * state.target.lastRenderedPosition.second +
				(1f - relativeTime) * state.thrower.lastRenderedPosition.second

		val throwHeight = 1f - 4f * (relativeTime - 0.5f).pow(2)
		itemY -= 0.25f * throwHeight

		batch = context.resources.kim1Renderer.startBatch()
		val sprite = state.item.sprite
		val scale = context.viewportHeight / 200f
		batch.requests.add(KimRequest(
			TransformedCoordinates.intX(itemX, context.viewportWidth) - (0.5f * scale * sprite.width).roundToInt(),
			TransformedCoordinates.intY(itemY, context.viewportHeight) - (0.5f * scale * sprite.height).roundToInt(),
			scale, sprite, rotation = 360f * relativeTime,
		))
	}

	fun render() {
		if (this::batch.isInitialized) {
			context.resources.kim1Renderer.submit(batch, context)
		}
	}
}
