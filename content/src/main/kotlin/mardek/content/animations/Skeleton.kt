package mardek.content.animations

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FunctionContext
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceFieldTarget
import com.github.knokko.bitser.serialize.BitPostInit
import com.github.knokko.bitser.serialize.Bitser

@BitStruct(backwardCompatible = true)
class Skeleton(
	@BitField(id = 0)
	@ReferenceFieldTarget(label = "skeleton parts")
	val parts: Array<SkeletonPart>,

	private val animations: HashMap<String, Animation>
) : BitPostInit {

	private lateinit var bitser: Bitser
	private val rawAnimations = HashMap<String, ByteArray>()

	constructor() : this(emptyArray(), HashMap())

	@Suppress("unused")
	@BitField(id = 0)
	@NestedFieldSetting(path = "v", writeAsBytes = true)
	private fun serializeAnimations(context: FunctionContext): HashMap<String, ByteArray> {
		val serializedAnimations = HashMap(rawAnimations)
		for ((name, animation) in animations) {
			serializedAnimations[name] = context.bitser.serializeToBytes(animation, Bitser.BACKWARD_COMPATIBLE, this)
		}
		return serializedAnimations
	}

	fun getAnimation(name: String): Animation {
		var animation = animations[name]
		if (animation != null) return animation
		val startTime = System.nanoTime()
		animation = bitser.deserializeFromBytes(
			Animation::class.java, rawAnimations[name]!!, Bitser.BACKWARD_COMPATIBLE, this
		)
		println("Loading animation took ${(System.nanoTime() - startTime) / 1000_000}ms")
		animations[name] = animation
		return animation
	}

	override fun postInit(context: BitPostInit.Context) {
		this.bitser = context.bitser
		@Suppress("UNCHECKED_CAST")
		this.rawAnimations.putAll(context.functionValues[Skeleton::class.java]!![0] as Map<String, ByteArray>)
	}
}
