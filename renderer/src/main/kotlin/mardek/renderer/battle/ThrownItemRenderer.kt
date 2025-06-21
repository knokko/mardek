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

		val throwPosition = transformBattleCoordinates(
			state.thrower.getPosition(context.battle),
			if (state.thrower.isOnPlayerSide) 1f else -1f, context.targetImage
		)
		val targetPosition = transformBattleCoordinates(
			state.target.getPosition(context.battle),
			if (state.target.isOnPlayerSide) 1f else -1f, context.targetImage
		)

		targetPosition.x = relativeTime * targetPosition.x + (1f - relativeTime) * throwPosition.x
		targetPosition.y = relativeTime * targetPosition.y + (1f - relativeTime) * throwPosition.y

		val throwHeight = 1f - 4f * (relativeTime - 0.5f).pow(2)
		targetPosition.y -= 0.25f * throwHeight

		batch = context.resources.kim1Renderer.startBatch()
		val sprite = state.item.sprite
		val scale = context.targetImage.height / 200f
		batch.requests.add(KimRequest(
			targetPosition.intX(context.targetImage.width) - (0.5f * scale * sprite.width).roundToInt(),
			targetPosition.intY(context.targetImage.height) - (0.5f * scale * sprite.height).roundToInt(),
			scale, sprite, rotation = 360f * relativeTime,
		))
	}

	fun render() {
		if (this::batch.isInitialized) {
			context.resources.kim1Renderer.submit(batch, context.recorder, context.targetImage)
		}
	}
}
