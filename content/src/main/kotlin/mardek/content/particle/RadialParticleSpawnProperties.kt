package mardek.content.particle

class RadialParticleSpawnProperties(
	/**
	 * The minimum distance between the particle and the emitter when the particle is spawned.
	 */
	val minRadius: Float,

	/**
	 * The minimum distance between the particle and the emitter when the particle is spawned.
	 */
	val maxRadius: Float,

	/**
	 * When the number of spawned particles is larger than 1, this property determines whether particles should be
	 * evenly spaced/divided over 360 degrees when they are spawned. For instance, for 3 particles, they would be
	 * spaced 120 degrees apart.
	 */
	val evenlySpaced: Boolean,

	/**
	 * When `evenlySpaced` is true, this determines the angle/direction of the first particle. For instance, when
	 * `baseEvenAngle == 20 && evenlySpaced`, the first particle spawns at 20 degrees, the second at 140 degrees, and
	 * the third at 260 degrees.
	 */
	val baseEvenAngle: Float,

	/**
	 * The value of `baseEvenAngle` is basically increased by `baseEvenAngleShift` every second (continuously), so the
	 * `baseEvenAngle` after `t` seconds is `originalBaseEvenAngle + t * baseEvenAngleShift`.
	 */
	val baseEvenAngleShift: Float,

	/**
	 * Whether the particle should initially be rotated towards the direction in which it starts moving
	 */
	val rotateToMoveDirection: Boolean,

	/**
	 * The minimum initial velocity (units / second), towards the spawn direction of the particle
	 */
	val minVelocity: Float,

	/**
	 * The maximum initial velocity (units / second), towards the spawn direction of the particle
	 */
	val maxVelocity: Float,
) {
}
