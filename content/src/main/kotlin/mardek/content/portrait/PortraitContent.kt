package mardek.content.portrait

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceFieldTarget
import mardek.content.animation.AnimationSprite
import mardek.content.animation.SkinnedAnimation

/**
 * The part/section of the `Content` that is used to render portraits
 */
@BitStruct(backwardCompatible = true)
class PortraitContent {

	/**
	 * All the portrait info data that is extracted from frame 1729 (_portrait). Every portrait has a [PortraitInfo].
	 * This portrait info, in combination with [animations], is needed to find and render the right 'child' animation
	 * of the root portraits animation.
	 */
	@BitField(id = 0)
	@ReferenceFieldTarget(label = "portrait info")
	val info = ArrayList<PortraitInfo>()

	/**
	 * The root animation that contains all portrait animations
	 */
	@BitField(id = 1)
	lateinit var animations: SkinnedAnimation

	/**
	 * All the sprites that are used by [animations]
	 */
	@BitField(id = 2)
	@ReferenceFieldTarget(label = "animation sprites")
	val animationSprites = ArrayList<AnimationSprite>()

	/**
	 * All the inner animations that are used by [animations]
	 */
	@BitField(id = 3)
	@ReferenceFieldTarget(label = "skinned animations")
	val skinnedAnimations = ArrayList<SkinnedAnimation>()

	/**
	 * The 'magic' scale that was the portrait importer used to import all the portraits. The `AnimationRenderer`
	 * needs this to determine the right orientation of each part of the portrait.
	 */
	@BitField(id = 4)
	@IntegerField(expectUniform = false, minValue = 1)
	var magicScale = 0
}
