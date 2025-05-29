package mardek.content.particle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import kotlin.math.ln
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.DurationUnit

@BitStruct(backwardCompatible = true)
class ParticleDynamics(
	/**
	 * This value is added to the X velocity of each particle, every second (continuously)
	 */
	@BitField(id = 0)
	@FloatField(expectMultipleOf = 0.1)
	val accelerationX: Float,

	/**
	 * This value is added to the Y velocity for each particle update, every second (continuously)
	 */
	@BitField(id = 1)
	@FloatField(expectMultipleOf = 0.1)
	val accelerationY: Float,

	/**
	 * The `accelerationX` of new particles is increased by `shiftAccelerationX` every second (continuously)
	 */
	@BitField(id = 2)
	@FloatField(expectMultipleOf = 0.1)
	val shiftAccelerationX: Float,

	/**
	 * The `accelerationY` of new particles is increased by `shiftAccelerationY` every second (continuously)
	 */
	@BitField(id = 3)
	@FloatField(expectMultipleOf = 0.1)
	val shiftAccelerationY: Float,

	/**
	 * Every second, the particle velocity is increased by `initialDirection * radialAcceleration`, every second
	 * (continuously)
	 */
	@BitField(id = 4)
	@FloatField(expectMultipleOf = 1.0)
	val radialAcceleration: Float,

	/**
	 * The X velocity of each emitted particle update will be multiplied by `velocityMultiplierX`, every second
	 * (continuously)
	 */
	@BitField(id = 5)
	@FloatField(expectMultipleOf = 0.01)
	val velocityMultiplierX: Float,

	/**
	 * The Y velocity of each emitted particle update will be multiplied by `velocityMultiplierY`, every second
	 * (continuously)
	 */
	@BitField(id = 6)
	@FloatField(expectMultipleOf = 0.01)
	val velocityMultiplierY: Float,

	/**
	 * The particle spin, in degrees per second: the rotation of each particle will be increased by `spin` degrees
	 * every second (continuously)
	 */
	@BitField(id = 7)
	@FloatField(expectMultipleOf = 1.0)
	val spin: Float,
) {
	internal constructor() : this(
		0f, 0f, 0f, 0f,
		0f, 0f, 0f, 0f
	)

	/**
	 * The recurrence formula for the velocity is
	 * ```
	 * v[t] = M*v[t-1] + A
	 * ```
	 * where `M` is `velocityMultiplierX`, and `A` is `accelerationX`.
	 * To compute the steady-state velocity `v*`, we solve
	 * ```
	 * v* = Mv* + A
	 * v* = A / (1-M)
	 * ```
	 * Using `v*`, the original formula can be rewritten to
	 * ```v(t) - v* = (v[0] - v*)M^t
	 * v(t) = v* + (v[0] - v*)M^t`
	 * ```
	 *
	 * To compute the (X) position, we need the primitive V(t) of v(t): which is
	 * ```
	 * V(t) = tv* + (v[0] - v*)M^t / ln(M) + C
	 * ```
	 * Finally
	 * ```
	 * x(t) = x(0) + V(t) - V(0) =
	 * x(0) + tv* + (v[0] - v*)M^t / ln(M) + C - 0v* - (v[0] - v*)M^0 / ln(M) - C =
	 * x(0) + tv* + (v[0] - v*)M^t / ln(M) - (v[0] - v*) / ln(M)
	 * ```
	 */
	fun computeX(initialX: Float, initialVelocityX: Float, timeSinceSpawn: Duration): Float {
		val t = timeSinceSpawn.toDouble(DurationUnit.SECONDS).toFloat()
		if (velocityMultiplierX == 1f) return initialX + t * initialVelocityX + t * t * 0.5f * accelerationX

		val steadyVelocity = accelerationX / (1f - velocityMultiplierX)
		return initialX + t * steadyVelocity + (initialVelocityX - steadyVelocity) *
				velocityMultiplierX.pow(t) / ln(velocityMultiplierX) -
				(initialVelocityX - steadyVelocity) / ln(velocityMultiplierX)
	}
}
