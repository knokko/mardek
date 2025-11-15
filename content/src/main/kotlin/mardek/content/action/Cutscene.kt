package mardek.content.action

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.content.animation.AnimationFrames
import java.util.UUID

/**
 * A cutscene, for instance the chapter 1 intro cutscene
 */
@BitStruct(backwardCompatible = true)
class Cutscene(
	/**
	 * The name of the cutscene, which is useful for debugging and editing
	 */
	@BitField(id = 0)
	val name: String,

	/**
	 * The animation frames of this cutscene
	 */
	@BitField(id = 1)
	val frames: AnimationFrames,

	/**
	 * The 'magic scale' of this cutscene. When the textures of this cutscene are imported from Flash, their width
	 * and height are multiplied by `magicScale`. This is needed because the flash textures are SVGs, which are
	 * converted to PNGs because this engine cannot handle SVGs. Using a larger `magicScale` will give a higher-quality
	 * texture, but also requires more disk space and (video) memory.
	 *
	 * We need to remember this magic scale because the renderer needs it to interpret some transformations correctly.
	 */
	@BitField(id = 2)
	@IntegerField(expectUniform = false, minValue = 1)
	val magicScale: Int,

	/**
	 * The name of the music track that should be played during the cutscene.
	 */
	@BitField(id = 3)
	val musicTrack: String,

	/**
	 * Some cutscenes (currently just the chapter 1 intro) have subtitles below the cutscene.
	 */
	@BitField(id = 4)
	val subtitles: Array<TextEntry>,

	/**
	 * The unique ID of the cutscene, which is used for (de)serialization
	 */
	@BitField(id = 5)
	@StableReferenceFieldId
	val id: UUID,
) {
	internal constructor() : this(
		"", AnimationFrames(), 0,
		"", emptyArray(), UUID.randomUUID(),
	)

	/**
	 * Represents a text/subtitle under a cutscene
	 */
	@BitStruct(backwardCompatible = true)
	class TextEntry(

		/**
		 * The first frame where this subtitle should be rendered. The renderer will keep rendering it until the next
		 * `TextEntry` overwrites it.
		 */
		@BitField(id = 0)
		@IntegerField(expectUniform = false, minValue = 1)
		val frame: Int,

		/**
		 * The text of this subtitle entry
		 */
		@BitField(id = 1)
		val text: String,
	) {

		@Suppress("unused")
		private constructor() : this(0, "")
	}
}
