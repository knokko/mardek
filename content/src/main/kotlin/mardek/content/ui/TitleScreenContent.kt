package mardek.content.ui

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import mardek.content.audio.AudioContent
import mardek.content.sprite.BcSprite

/**
 * This is a minified version of [mardek.content.Content] that is needed by the title screen. This content must be
 * loaded right when the game launches, whereas the real `Content` is only needed after loading a save (or starting
 * a new save).
 */
@BitStruct(backwardCompatible = true)
class TitleScreenContent(

	/**
	 * The title screen background image
	 */
	@BitField(id = 0)
	val background: BcSprite,

	/**
	 * The arrow head sprite, which is used in the Music Player
	 */
	@BitField(id = 1)
	val arrowHead: BcSprite,

	/**
	 * The crystal pointer sprite (used by the Music Player)
	 */
	@BitField(id = 2)
	val crystalPointer: BcSprite,

	/**
	 * The basic font used by the title screen
	 */
	@BitField(id = 3)
	val basicFont: Font,

	/**
	 * The fat font used by the title screen
	 */
	@BitField(id = 4)
	val fatFont: Font,

	/**
	 * The full-uppercase font used by the title screen
	 */
	@BitField(id = 5)
	val largeFont: Font,

	/**
	 * The 'boring' (Myriad) font
	 */
	@BitField(id = 6)
	val boringFont: Font,

	/**
	 * The subset of the [AudioContent] that is relevant for the title screen,
	 * currently just the music tracks and music categories.
	 */
	@BitField(id = 7)
	val audio: AudioContent,

	/**
	 * The yellow/brown music note icon/image that represents music tracks from all music categories,
	 * and is shown in the Music Player.
	 */
	@BitField(id = 8)
	val neutralMusicNote: BcSprite,

	/**
	 * A sprite containing the shadow of a music note
	 */
	@BitField(id = 9)
	val musicNoteShadow: BcSprite,

	/**
	 * The icon in the Music Player to play/unpause the current music track
	 */
	@BitField(id = 10)
	val playMusicIcon: BcSprite,

	/**
	 * The icon in the Music Player to pause the current music track
	 */
	@BitField(id = 11)
	val pauseMusicIcon: BcSprite,

	/**
	 * [playMusicIcon] while the mouse is hovering over it
	 */
	@BitField(id = 12)
	val playMusicHoveredIcon: BcSprite,

	/**
	 * [pauseMusicIcon] while the mouse is hovering over it
	 */
	@BitField(id = 13)
	val pauseMusicHoveredIcon: BcSprite,
) {

	@Suppress("unused")
	private constructor() : this(
		BcSprite(), BcSprite(), BcSprite(),
		Font(), Font(), Font(), Font(),
		AudioContent(), BcSprite(), BcSprite(),
		BcSprite(), BcSprite(),
		BcSprite(), BcSprite(),
	)

	companion object {

		/**
		 * Creates a dummy [TitleScreenContent] instance for unit test purposes
		 */
		fun dummy() = TitleScreenContent()
	}
}
