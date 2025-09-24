package mardek.content.animation

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import kotlin.time.Duration

@BitStruct(backwardCompatible = true)
class AnimationMaskFrame(

	@BitField(id = 0)
	val sprite: AnimationSprite,

	@BitField(id = 1)
	val matrix: AnimationMatrix,

	@BitField(id = 2)
	@IntegerField(expectUniform = true, minValue = 1)
	val duration: Duration,
) {
	@Suppress("unused")
	private constructor() : this(AnimationSprite(), AnimationMatrix(), Duration.ZERO)
}
