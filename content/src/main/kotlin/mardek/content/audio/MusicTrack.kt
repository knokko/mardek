package mardek.content.audio

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.StableReferenceFieldId
import java.io.Serializable
import java.util.UUID
import kotlin.time.Duration

/**
 * Represents a single music track (e.g. MightyHeroes)
 */
@BitStruct(backwardCompatible = true)
class MusicTrack(

	/**
	 * The unique ID of this track, which is used for (de)serialization purposes
	 */
	@BitField(id = 0)
	@StableReferenceFieldId
	val id: UUID,

	/**
	 * The nice/display name of this music track, which is shown in the Music Player.
	 */
	@BitField(id = 1)
	val displayName: String,

	/**
	 * The file name of this music track, within `resources/music`, but without extension.
	 * For instance, if this is "MightyHeroes", the corresponding file would be `resources/music/MightyHeroes.ogg.zstd`.
	 */
	@BitField(id = 2)
	val fileName: String,

	@BitField(id = 3)
	@IntegerField(minValue = 0, expectUniform = false)
	val loopAfter: Duration,

	/**
	 * The category to which this music track belongs (e.g. Area Music).
	 *
	 * This field is currently only used in the Music Player.
	 */
	@BitField(id = 4)
	@ReferenceField(stable = false, label = "music categories")
	val category: MusicCategory,
) : Serializable {

	internal constructor() : this(
		UUID(0, 0),
		"", "", Duration.ZERO, MusicCategory(),
	)

	override fun toString() = "MusicTrack($fileName)"
}
