package mardek.content.particle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.IntegerField

/**
 * Describes the quake effect of a particle effect (e.g. the Earthquake skill let's the screen shake/quake)
 */
@BitStruct(backwardCompatible = true)
class ParticleQuake(

	/**
	 * TODO CHAP3 Figure this out
	 */
	@BitField(id = 0)
	@IntegerField(expectUniform = false, minValue = 1)
	val strength: Int,

	/**
	 * The quake duration, in seconds
	 */
	@BitField(id = 1)
	@FloatField(expectMultipleOf = 1.0 / 30.0)
	val duration: Float,

	/**
	 * The decay per second
	 */
	@BitField(id = 2)
	@FloatField
	val decay: Float,
) {
	@Suppress("unused")
	private constructor() : this(0, 0f, 0f)
}
