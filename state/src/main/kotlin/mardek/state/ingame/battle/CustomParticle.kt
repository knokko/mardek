package mardek.state.ingame.battle

import mardek.content.sprite.BcSprite
import kotlin.math.floor
import kotlin.math.pow
import kotlin.random.Random

class CustomParticle(
	val sprite: BcSprite,
	val combatant: CombatantState,
	val startX: Float,
	val startY: Float,
	val rotation: Float,

	/**
	 * The initial width, which is always the same as the initial height
	 */
	val startSize: Float,

	/**
	 * The initial opacity:
	 * - an opacity of 0.0 means that the particle is initially transparent
	 * - an opacity of 1.0 means that the particle is initially opaque
	 */
	val startOpacity: Float,

	/**
	 * The X-coordinate is increased by `velocityX` every second (continuously)
	 */
	val velocityX: Float,

	/**
	 * The Y-coordinate is increased by `velocityY` every second (continuously)
	 */
	val velocityY: Float,

	/**
	 * The width and height are increased by `growth` every second (continuously)
	 */
	val growth: Float,

	/**
	 * The width and height are multiplied by `growFactor` every second (continuously)
	 */
	val growFactor: Float,

	/**
	 * - When `discreteFadePeriod` is 0, the opacity is decreased by `fade` every second (continuously)
	 * - Otherwise, the opacity is decreased by `fade` every `discreteFadePeriod` seconds, stepwise/discrete
	 */
	val fade: Float,
	val discreteFadePeriod: Float,

	/**
	 * The `growFactor` is increased by `shiftGrowFactor` every second (continuously)
	 */
	val shiftGrowFactor: Float,
) {

	val spawnTime = System.nanoTime()

	private fun passedTime(time: Long) = (time - spawnTime) / 1000_000_000f

	fun getX(time: Long) = startX + velocityX * passedTime(time)

	fun getY(time: Long) = startY + velocityY * passedTime(time)

	fun getSize(time: Long): Float {
		val passedTime = passedTime(time)
		if (growFactor == 1f && shiftGrowFactor == 0f) return startSize + growth * passedTime
		if (growth != 0f) throw UnsupportedOperationException(
			"This requires a more complicated formula: see ParticleEffectState.computeX for inspiration"
		)

		if (shiftGrowFactor == 0f) return startSize * growFactor.pow(passedTime)

		/*
		 * The recurrence formula for the size is:
		 *
		 * s[t] = s[t-1] * g[t-1] where
		 *   g[t] = g[t-1] + g_s = g[0] + t * g_s
		 *
		 * So s[t] = s[t-1] * (g[0] + (t-1) * g_s) =
		 *     s[0] * g[0] * (g[0] + g_s) * (g[0] + 2g_s) * (g[0] + 3g_s) * ... * (g[0] + (t-1)g_s)
		 *
		 * According to AI, this is a multiplicative, first-order, non-linear recurrence, which does not have a nice
		 * characteristic equation that can be used to solve it. It looks like this formula does not really have an
		 * exact closed-form expression, so let's compute it step-by-step instead.
		 */
		val numSteps = (passedTime * 30).toInt()
		var size = startSize
		for (step in 0 until numSteps) {
			size *= (growFactor + step * shiftGrowFactor)
		}
		val nextSize = size * (growFactor + numSteps * shiftGrowFactor)
		val remainingTime = passedTime - numSteps / 30f
		val factor = remainingTime * 30f
		return (1f - factor) * size + factor * nextSize
	}

	fun getOpacity(time: Long) = startOpacity - fade *
			(if (discreteFadePeriod == 0f) passedTime(time) else floor(passedTime(time) / discreteFadePeriod))

	companion object {

		fun poison(combatant: CombatantState, sprite: BcSprite) = CustomParticle(
			sprite = sprite,
			combatant = combatant,
			startX = 8f * Random.nextFloat(),
			startY = 2f - 8f * Random.nextFloat(),
			rotation = 0f,
			startSize = 4f + 5f * Random.nextFloat(),
			startOpacity = 1f,
			velocityX = 0f,
			velocityY = -30f,
			growth = 0f,
			growFactor = 1.3f,
			fade = 0f,
			discreteFadePeriod = 0f,
			shiftGrowFactor = -0.05f,
		)

		fun blind(combatant: CombatantState, sprite: BcSprite) = CustomParticle(
			sprite = sprite,
			combatant = combatant,
			startX = 8f * Random.nextFloat() - 4f,
			startY = 6f * Random.nextFloat() - 2f,
			rotation = 0f,
			startSize = 8f + 16f * Random.nextFloat(),
			startOpacity = 0.8f,
			velocityX = 0f,
			velocityY = 0f,
			growth = 0f,
			growFactor = 1f,
			fade = 2.4f,
			discreteFadePeriod = 0f,
			shiftGrowFactor = 0f
		)

		fun sleep(combatant: CombatantState, sprite: BcSprite) = CustomParticle(
			sprite = sprite,
			combatant = combatant,
			startX = 4f + 4f * Random.nextFloat(),
			startY = 2f - 4f * Random.nextFloat(),
			rotation = 30f * Random.nextFloat(),
			startSize = 4f + 4f * Random.nextFloat(),
			startOpacity = 1f,
			velocityX = 15f,
			velocityY = -18f,
			growth = 0f,
			growFactor = 1.04.pow(30).toFloat(),
			fade = 0.6f,
			discreteFadePeriod = 0f,
			shiftGrowFactor = 0f
		)

		fun paralysis(combatant: CombatantState, sprite: BcSprite) = CustomParticle(
			sprite = sprite,
			combatant = combatant,
			startX = 32f * Random.nextFloat() - 16f,
			startY = 32f * Random.nextFloat() - 12f,
			rotation = 360f * Random.nextFloat(),
			startSize = 4f + 12f * Random.nextFloat(),
			startOpacity = 1f,
			velocityX = 0f,
			velocityY = 0f,
			growth = 0f,
			growFactor = 1f,
			fade = 1f,
			discreteFadePeriod = 0.035f,
			shiftGrowFactor = 0f
		)
	}
}
