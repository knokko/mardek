package mardek.content.animation

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.serialize.BitPostInit
import kotlin.time.Duration

/**
 * Represents a list of `AnimationFrame`s that should be played in order.
 */
@BitStruct(backwardCompatible = true)
class AnimationFrames(
	@BitField(id = 0)
	val frames: Array<AnimationFrame>,
): BitPostInit, Iterable<AnimationFrame> {

	/**
	 * The sum of the duration of all the frames
	 */
	var duration = Duration.ZERO
		private set

	constructor() : this(emptyArray())

	init {
		for (frame in frames) duration += frame.duration
	}

	override fun postInit(context: BitPostInit.Context) {
		if (frames.isEmpty()) throw RuntimeException("No frames?")
		duration = Duration.ZERO
		for (frame in frames) duration += frame.duration
	}

	override fun iterator() = frames.iterator()

	/**
	 * Checks whether any of the frames has at least 1 (child) node whose special is `special`. This method is nice for
	 * unit-testing that the import succeeded.
	 */
	fun hasSpecialNode(special: SpecialAnimationNode): Boolean = frames.any { it.hasSpecialNode(special) }
}
