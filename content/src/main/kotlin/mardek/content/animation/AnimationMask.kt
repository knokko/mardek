package mardek.content.animation

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.BitPostInit
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import kotlin.time.Duration

/**
 * Represents an alpha *mask* of an animation: the `mask` of an `AnimationNode`. It can be used to clip the animation
 * of its node.
 */
@BitStruct(backwardCompatible = true)
class AnimationMask(

	/**
	 * The animation mask frames, or an empty array when this node doesn't have a mask. The number of frames is almost
	 * always 0, sometimes 1, but rarely more than 1. Still, we need to support multiple frames for these rare cases.
	 */
	@BitField(id = 0)
	@NestedFieldSetting(path = "", sizeField = IntegerField(
		minValue = 0, expectUniform = false, commonValues = [0]
	))
	val frames: Array<AnimationMaskFrame>
) : BitPostInit, Iterable<AnimationMaskFrame> {

	/**
	 * The sum of the duration of all mask frames (0 when there are no frames)
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
