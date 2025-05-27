package mardek.content.particle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.IntegerField
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@BitStruct(backwardCompatible = true)
class ParticleQuake(

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
