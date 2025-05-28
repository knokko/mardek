package mardek.content.particle

import com.github.knokko.bitser.BitStruct

@BitStruct(backwardCompatible = true)
class ParticleSize(
	/**
	 * The 'base' initial width of the emitted particles, which will be multiplied by a random number between
	 * `minSizeMultiplier` and `maxSizeMultiplier` to determine the initial particle width.
	 */
	val baseWidth: Float,

	/**
	 * The 'base' initial height of the emitted particles, which will be multiplied by a random number between
	 * `minSizeMultiplier` and `maxSizeMultiplier` (the same random number that was multiplied by `baseWidth`)
	 * to determine the initial particle height.
	 */
	val baseHeight: Float,

	/**
	 * Whenever a particle is spawned, its width and height will be multiplied by a random number between
	 * `minSizeMultiplier` and `maxSizeMultiplier`.
	 */
	val minSizeMultiplier: Float,
	val maxSizeMultiplier: Float,

	/**
	 * At any point in time, the current width of each particle is computed as
	 * `initialWidth * pow(growX, time since particle spawned)`
	 */
	val growX: Int,

	/**
	 * At any point in time, the current height of each particle is computed as
	 * `initialHeight * pow(growY, time since particle spawned)`
	 */
	val growY: Int,
) {
}
