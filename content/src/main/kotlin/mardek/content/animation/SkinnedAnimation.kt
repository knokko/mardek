package mardek.content.animation

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField

@BitStruct(backwardCompatible = true)
class SkinnedAnimation(
	@BitField(id = 0)
	@IntegerField(expectUniform = true, minValue = -1, maxValue = 8000)
	val defineSpriteFlashID: Int,

	@BitField(id = 1)
	val skins: HashMap<String, AnimationFrames>,
) {

	@Suppress("unused")
	private constructor() : this(0, HashMap())

	override fun toString() = "SkinnedAnimation(flash sprite ID = $defineSpriteFlashID, #skins=${skins.size})"

	fun hasSpecial(special: SpecialAnimationNode) = skins.values.any { it.hasSpecialNode(special) }
}
