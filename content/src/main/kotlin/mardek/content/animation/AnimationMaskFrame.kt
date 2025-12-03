package mardek.content.animation

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import kotlin.time.Duration

/**
 * Represents a frame of an `AnimationMask`. Note that most animation masks have just 1 frame.
 */
@BitStruct(backwardCompatible = true)
class AnimationMaskFrame(

	/**
	 * The sprite of this mask frame: it should be a single-channel image that defines the alpha mask of the masked
	 * animation.
	 */
	@BitField(id = 0)
	@ReferenceField(stable = false, label = "animation sprites")
	val sprite: AnimationSprite,

	/**
	 * The transformation that should be applied to `sprite`
	 */
	@BitField(id = 1)
	val matrix: AnimationMatrix,

	/**
	 * The duration of the frame, which will always be 1/30 seconds for frames that are imported from flash. (Currently,
	 * all frames are imported from flash.)
	 */
	@BitField(id = 2)
	@IntegerField(expectUniform = false, minValue = 1, digitSize = 4, commonValues = [66666666])
	val duration: Duration,
) {
	@Suppress("unused")
	private constructor() : this(AnimationSprite(), AnimationMatrix(), Duration.ZERO)
}
