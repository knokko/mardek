package mardek.content.animation

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.serialize.BitPostInit
import kotlin.time.Duration

@BitStruct(backwardCompatible = true)
class AnimationMask(

	/**
	 * The animation frames, or an empty array when this node doesn't have a mask
	 */
	@BitField(id = 0)
	val frames: Array<AnimationMaskFrame>
) : BitPostInit, Iterable<AnimationMaskFrame> {

	/**
	 * The sum of the duration of all frames (0 when there are no frames)
	 */
	var duration = Duration.ZERO
		private set

	internal constructor() : this(emptyArray())

	init {
		for (frame in frames) duration += frame.duration
	}

	override fun postInit(context: BitPostInit.Context) {
		duration = Duration.ZERO
		for (frame in frames) duration += frame.duration
	}

	override fun iterator() = frames.iterator()
}
