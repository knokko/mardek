package mardek.content.animation

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import kotlin.time.Duration

@BitStruct(backwardCompatible = true)
class AnimationFrame(
	@BitField(id = 0)
	@IntegerField(expectUniform = true, minValue = 1)
	val duration: Duration,

	@BitField(id = 1)
	val nodes: Array<AnimationNode>,
): Iterable<AnimationNode> {

	@Suppress("unused")
	private constructor() : this(Duration.ZERO, emptyArray())

	override fun iterator() = nodes.iterator()

	fun hasSpecialNode(special: SpecialAnimationNode) = nodes.any { it.hasSpecial(special) }
}
