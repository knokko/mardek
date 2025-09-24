package mardek.content.animation

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.serialize.BitPostInit
import kotlin.time.Duration

@BitStruct(backwardCompatible = true)
class AnimationFrames(
	@BitField(id = 0)
	val frames: Array<AnimationFrame>,
): BitPostInit, Iterable<AnimationFrame> {

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

	fun hasSpecialNode(special: SpecialAnimationNode): Boolean = frames.any { it.hasSpecialNode(special) }
}
