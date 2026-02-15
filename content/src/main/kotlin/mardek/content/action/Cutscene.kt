package mardek.content.action

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.SimpleLazyBits
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.content.audio.SoundEffect
import java.util.UUID
import kotlin.time.Duration

/**
 * A cutscene, for instance the chapter 1 intro cutscene
 */
@BitStruct(backwardCompatible = true)
class Cutscene(
	/**
	 * The unique ID of the cutscene, which is used for (de)serialization
	 */
	@BitField(id = 0)
	@StableReferenceFieldId
	val id: UUID,

	/**
	 * The name of the cutscene, which is useful for debugging and editing
	 */
	@BitField(id = 1)
	val name: String,

	/**
	 * The payload/content of this cutscene, which is *lazy* to reduce the loading time of the game
	 */
	@BitField(id = 2)
	val payload: SimpleLazyBits<CutscenePayload>,

	/**
	 * The sounds that should be played during this cutscene
	 */
	@BitField(id = 3)
	val sounds: Array<SoundEntry>,
) {

	internal constructor() : this(
		UUID(0, 0), "",
		SimpleLazyBits(CutscenePayload()), emptyArray(),
	)

	/**
	 * A sound effect that should be played after a fixed amount of time, since the start of the cutscene
	 */
	@BitStruct(backwardCompatible = true)
	class SoundEntry(

		/**
		 * The sound should be played at `cutsceneStartTime + delay`
		 */
		@BitField(id = 0)
		@IntegerField(expectUniform = false, minValue = 0)
		val delay: Duration,

		/**
		 * The sound effect to be played after the `delay`
		 */
		@BitField(id = 1)
		@ReferenceField(stable = false, label = "sound effects")
		val sound: SoundEffect,
	) {

		@Suppress("unused")
		private constructor() : this(Duration.ZERO, SoundEffect())
	}
}
