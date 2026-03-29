package mardek.content.animation

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.ReferenceLazyBits
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.LazyReferences
import com.github.knokko.bitser.field.NestedFieldSetting

/**
 * Represents an animation that is possibly *skinned*. When a skinned animation is being rendered, the renderer will
 * keep track of the name of the current skin, and try to pick the animation that belongs to that skin.
 *
 * Note that not all instances of `SkinnedAnimation` have multiple skins.
 */
@BitStruct(backwardCompatible = true)
class SkinnedAnimation(

	/**
	 * The ID of the Flash DefineSpriteTag from which this `SkinnedAnimation` was imported, or -1 if it was not
	 * imported from Flash. (Currently, all animations are imported from Flash.)
	 */
	@BitField(id = 0)
	@IntegerField(expectUniform = true, minValue = -1, maxValue = 8000)
	val defineSpriteFlashID: Int,

	/**
	 * The skins of this animation.
	 */
	@BitField(id = 1)
	@NestedFieldSetting(path = "v", fieldName = "SKINS_VALUES_PROPERTIES")
	val skins: HashMap<String, ReferenceLazyBits<AnimationFrames>>,
) {

	@Suppress("unused")
	private constructor() : this(0, HashMap())

	override fun toString() = "SkinnedAnimation(flash sprite ID = $defineSpriteFlashID, #skins=${skins.size})"

	/**
	 * Checks whether any of the skins has at least 1 (child) node whose special is `special`. This method is nice for
	 * unit-testing that the import succeeded.
	 */
	fun hasSpecial(special: SpecialAnimationNode) = skins.values.any { it.get().hasSpecialNode(special) }

	companion object {

		@Suppress("unused")
		@LazyReferences(labels = ["animation sprites", "skinned animations"])
		private const val SKINS_VALUES_PROPERTIES = false
	}
}
