package mardek.content.action

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceFieldTarget
import mardek.content.animation.AnimationFrames
import mardek.content.animation.AnimationSprite
import mardek.content.animation.SkinnedAnimation
import kotlin.longArrayOf

/**
 * The payload of a [Cutscene]. To reduce the loading time, the payload is only deserialized when it is actually used.
 */
@BitStruct(backwardCompatible = true)
class CutscenePayload(
	/**
	 * The animation frames of this cutscene
	 */
	@BitField(id = 0)
	val frames: AnimationFrames,

	/**
	 * Some cutscenes (currently just the chapter 1 intro) have subtitles below the cutscene.
	 */
	@BitField(id = 1)
	val subtitles: Array<TextEntry>,

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
	 * The name of the music track that should be played during the cutscene, or null if no music should be played.
	 */
	@BitField(id = 3, optional = true)
	val musicTrack: String?,
) {
	/**
	 * All animation sprites that are used by this cutscene
	 */
	@BitField(id = 4)
	@ReferenceFieldTarget(label = "animation sprites")
	val sprites = ArrayList<AnimationSprite>()

	/**
	 * All inner animations that are used by this cutscene
	 */
	@BitField(id = 5)
	@Suppress("unused")
	@ReferenceFieldTarget(label = "skinned animations")
	val innerAnimations = ArrayList<SkinnedAnimation>()

	constructor() : this(AnimationFrames(), emptyArray(), 0, "")

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

		/**
		 * The subtitle index:
		 * - 0 means on the bottom-left of the screen
		 * - 1 means on the bottom-middle of the screen
		 * - 2 means on the bottom-right of the screen
		 */
		@BitField(id = 2)
		@IntegerField(expectUniform = true, minValue = 0, maxValue = 2, commonValues = [1])
		val index: Int,
	) {

		@Suppress("unused")
		private constructor() : this(0, "", 0)
	}
}
