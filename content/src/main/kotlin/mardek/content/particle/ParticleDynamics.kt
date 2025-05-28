package mardek.content.particle

import com.github.knokko.bitser.BitStruct
import java.lang.Math.pow
import kotlin.math.ln
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.DurationUnit

@BitStruct(backwardCompatible = true)
class ParticleDynamics(
	/**
	 * This value is added to the X velocity of each particle, every second (continuously)
	 */
	val accelerationX: Float,

	/**
	 * This value is added to the Y velocity for each particle update, every second (continuously)
	 */
	val accelerationY: Float,

	/**
	 * Every second, the particle velocity is increased by `initialDirection * radialAcceleration` (continuously)
	 */
	val radialAcceleration: Float,

	/**
	 * The X velocity of each emitted particle update will be multiplied by `velocityMultiplierX`, every second
	 * (continuously)
	 */
	val velocityMultiplierX: Float,

	/**
	 * The Y velocity of each emitted particle update will be multiplied by `velocityMultiplierY`, every second
	 * (continuously)
	 */
	val velocityMultiplierY: Float,

	/**
	 * The particle spin, in degrees per second: the rotation of each particle will be increased by `spin` degrees
	 * every second (continuously): the rotation of a particle after `t` seconds is computed as
	 * `initialRotation + t * spin`.
	 */
	val spin: Float,
) {
	/**
	 * See math.txt
	 */
	fun computeX(initialX: Float, initialVelocityX: Float, timeSinceSpawn: Duration): Float {
		val t = timeSinceSpawn.toDouble(DurationUnit.SECONDS).toFloat()
		if (velocityMultiplierX == 1f) return initialX + t * initialVelocityX + t * t * 0.5f * accelerationX

		val steadyVelocity = accelerationX / (1f - velocityMultiplierX)
		return initialX + t * steadyVelocity + (initialVelocityX - steadyVelocity) *
				velocityMultiplierX.pow(t) / ln(velocityMultiplierX)
	}
}
