package mardek.content.animation

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceFieldTarget

/**
 * Represents an animation that can be (de)serialized independently. This is basically an instance of `AnimationFrames`,
 * plus the sprites and inner animations used by that `AnimationFrames`.
 *
 * This independent deserialization is important for the lazy loading of combatant animations, since we must be able to
 * render any animation, regardless of which other animations are deserialized already. Lazy loading of combatant
 * animations is important to reduce the start-up time of the game.
 */
@BitStruct(backwardCompatible = true)
class StandaloneAnimation(

	/**
	 * The animation
	 */
	@BitField(id = 0)
	val frames: AnimationFrames,

	/**
	 * All sprites that this animation can use. This field should only be used during exporting.
	 */
	@BitField(id = 1)
	@Suppress("unused")
	@ReferenceFieldTarget(label = "animation sprites")
	val innerSprites: Array<AnimationSprite>,

	/**
	 * All child animations that this animation can use. This field should only be used during exporting.
	 */
	@BitField(id = 2)
	@Suppress("unused")
	@ReferenceFieldTarget(label = "skinned animations")
	private val innerAnimations: Array<SkinnedAnimation>,
) {
	@Suppress("unused")
	private constructor() : this(AnimationFrames(), emptyArray(), emptyArray())
}
