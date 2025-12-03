package mardek.content.animation

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.SimpleLazyBits

/**
 * Represents the skeleton of a combatant. This contains all the animations of the combatant, as well as some 'flat'
 * nodes that are independent of the animation.
 */
@BitStruct(backwardCompatible = true)
class CombatantSkeleton(

	/**
	 * The 'flat' nodes that are present during all animations
	 */
	@BitField(id = 0)
	val flatNodes: Array<AnimationNode>,

	/**
	 * This field should only be used during exporting! Instead, you should use the `get(animationName)` method instead
	 * (and preferably via operator overloading).
	 */
	@BitField(id = 1)
	val animations: HashMap<String, SimpleLazyBits<StandaloneAnimation>>,

	/**
	 * The ID of the Flash DefineSpriteTag from which this skeleton was imported, or 0 if this skeleton wasn't
	 * imported from Flash. (For now, all skeletons are imported from Flash though...)
	 */
	@BitField(id = 2)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 9000)
	val defineSpriteID: Int,

	/**
	 * The 'magic scale' of this skeleton. When the textures of this skeleton are imported from Flash, their width and
	 * height are multiplied by `magicScale`. This is needed because the flash textures are SVGs, which are converted
	 * to PNGs because this engine cannot handle SVGs. Using a larger `magicScale` will give a higher-quality texture,
	 * but also requires more disk space and (video) memory.
	 *
	 * We need to remember this magic scale because the renderer needs it to interpret some transformations correctly.
	 */
	@BitField(id = 3)
	@IntegerField(expectUniform = true, minValue = 1, maxValue = 8)
	val magicScale: Int,
) {
	constructor() : this(emptyArray(), HashMap(), -1, 1)

	/**
	 * Gets the animation with the given `name`. If the animation is used for the first time, it will be deserialized
	 * first. An exception will be thrown if no animation with the given name exists.
	 */
	operator fun get(name: String) = animations[name]!!.get()!!.frames
}
