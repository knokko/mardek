package mardek.content.animation

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FunctionContext
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.serialize.BitPostInit
import com.github.knokko.bitser.serialize.Bitser
import mardek.content.BITSER

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
	val animations: HashMap<String, StandaloneAnimation>,

	/**
	 * The ID of the Flash DefineSpriteTag from which this skeleton was imported, or 0 if this skeleton wasn't
	 * imported from Flash. (For now, all skeletons are imported from Flash though...)
	 */
	@BitField(id = 1)
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
	@BitField(id = 2)
	@IntegerField(expectUniform = true, minValue = 1, maxValue = 8)
	val magicScale: Int,

) : BitPostInit {
	private val rawAnimations = HashMap<String, ByteArray>()

	constructor() : this(emptyArray(), HashMap(), -1, 1)

	@Suppress("unused")
	@BitField(id = 0)
	@NestedFieldSetting(path = "v", writeAsBytes = true)
	private fun serializeAnimations(context: FunctionContext): HashMap<String, ByteArray> {
		val serializedAnimations = HashMap(rawAnimations)
		for ((name, animation) in animations) {
			serializedAnimations[name] = context.bitser.serializeToBytes(
				animation, Bitser.BACKWARD_COMPATIBLE
			)
		}
		return serializedAnimations
	}

	/**
	 * Gets the animation with the given `name`. If the animation is used for the first time, it will be deserialized
	 * first. An exception will be thrown if no animation with the given name exists.
	 */
	operator fun get(name: String): AnimationFrames {
		var animation = animations[name]
		if (animation != null) return animation.frames

		val startTime = System.nanoTime()
		animation = BITSER.deserializeFromBytes(
			StandaloneAnimation::class.java, rawAnimations[name]!!,
			Bitser.BACKWARD_COMPATIBLE
		)
		println("Loading animation took ${(System.nanoTime() - startTime) / 1000_000}ms")
		animations[name] = animation
		return animation.frames
	}

	override fun postInit(context: BitPostInit.Context) {
		// TODO Try to push this logic into bitser
		@Suppress("UNCHECKED_CAST")
		this.rawAnimations.putAll(context.functionValues[CombatantSkeleton::class.java]!![0] as Map<String, ByteArray>)
	}
}
