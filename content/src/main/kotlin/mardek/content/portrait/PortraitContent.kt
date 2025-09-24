package mardek.content.portrait

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceFieldTarget
import mardek.content.animation.AnimationSprite
import mardek.content.animation.SkinnedAnimation

@BitStruct(backwardCompatible = true)
class PortraitContent {

	@BitField(id = 0)
	@ReferenceFieldTarget(label = "portrait info")
	val info = ArrayList<PortraitInfo>()

	@BitField(id = 1)
	lateinit var animations: SkinnedAnimation

	@BitField(id = 2)
	@ReferenceFieldTarget(label = "animation sprites")
	val animationSprites = ArrayList<AnimationSprite>()

	@BitField(id = 3)
	@ReferenceFieldTarget(label = "skinned animations")
	val skinnedAnimations = ArrayList<SkinnedAnimation>()

	@BitField(id = 4)
	@IntegerField(expectUniform = false, minValue = 1)
	var magicScale = 0
}
