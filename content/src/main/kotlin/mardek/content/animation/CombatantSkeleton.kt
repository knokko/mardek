package mardek.content.animation

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FunctionContext
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.serialize.BitPostInit
import com.github.knokko.bitser.serialize.Bitser
import mardek.content.BITSER

@BitStruct(backwardCompatible = true)
class CombatantSkeleton(
	@BitField(id = 0)
	val flatNodes: Array<AnimationNode>,

	val animations: HashMap<String, StandaloneAnimation>,

	@BitField(id = 1)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 9000)
	val defineSpriteID: Int,

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
