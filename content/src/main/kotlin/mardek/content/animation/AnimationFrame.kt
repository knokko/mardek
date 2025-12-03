package mardek.content.animation

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import kotlin.time.Duration

/**
 * Represents 1 frame in an animation of e.g. a monster. A frame is basically a list of independent parts/nodes.
 */
@BitStruct(backwardCompatible = true)
class AnimationFrame(

	/**
	 * The duration of the frame, which will be 1/30 seconds for any frame imported from flash (currently, all frames
	 * are imported from flash)
	 */
	@BitField(id = 0)
	@IntegerField(expectUniform = false, minValue = 1, digitSize = 4, commonValues = [66666666])
	val duration: Duration,

	/**
	 * The *nodes* of the frame. Each node will be rendered independently.
	 */
	@BitField(id = 1)
	@NestedFieldSetting(path = "", sizeField = IntegerField(
		minValue = 0, expectUniform = false, digitSize = 2, commonValues = [1]
	))
	val nodes: Array<AnimationNode>,
): Iterable<AnimationNode> {

	@Suppress("unused")
	private constructor() : this(Duration.ZERO, emptyArray())

	override fun iterator() = nodes.iterator()

	/**
	 * Checks whether this frame has at least 1 (child) node whose special is `special`. This method is nice for
	 * unit-testing that the import succeeded.
	 */
	fun hasSpecialNode(special: SpecialAnimationNode) = nodes.any { it.hasSpecial(special) }
}
