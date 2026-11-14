package mardek.content

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import mardek.content.sprite.BcSprite
import mardek.content.sprite.KimSprite
import mardek.content.ui.TitleScreenContent
import java.io.Serializable

/**
 * A thin wrapper around [Content] that additionally contains some sprites that are needed for exporting,
 * and for the [TitleScreenContent].
 * This is the 'root' class of the editor, although it is not used by the game at runtime.
 */
@BitStruct(backwardCompatible = true)
class ContentWrapper(

	/**
	 * The primary [Content] that will be used by the game
	 */
	@BitField(id = 0)
	val content: Content,

	/**
	 * See [TitleScreenContent.neutralMusicNote]
	 */
	@BitField(id = 1)
	val neutralMusicNote: BcSprite,

	/**
	 * See [TitleScreenContent.musicNoteShadow]
	 */
	@BitField(id = 2)
	val musicNoteShadow: BcSprite,

	/**
	 * See [TitleScreenContent.playMusicIcon]
	 */
	@BitField(id = 3)
	val playMusicIcon: BcSprite,

	/**
	 * See [TitleScreenContent.pauseMusicIcon]
	 */
	@BitField(id = 4)
	val pauseMusicIcon: BcSprite,

	/**
	 * See [TitleScreenContent.playMusicHoveredIcon]
	 */
	@BitField(id = 5)
	val playMusicHoveredIcon: BcSprite,

	/**
	 * See [TitleScreenContent.pauseMusicHoveredIcon]
	 */
	@BitField(id = 6)
	val pauseMusicHoveredIcon: BcSprite,

	/**
	 * The application (taskbar) icon of the game. This is a PhoenixPinion by default, but can be changed.
	 */
	@BitField(id = 7)
	val applicationIcon: KimSprite,

	/**
	 * The cursor that is used to represent `MardekCursor.Inventory`.
	 *
	 * This is the cursor that is normally used while the inventory is open.
	 */
	@BitField(id = 8)
	val inventoryCursor: KimSprite,

	/**
	 * The cursor that is used to represent `MardekCursor.Pointer`.
	 *
	 * This is the default cursor (outside the inventory).
	 */
	@BitField(id = 9)
	val pointerCursor: KimSprite,

	/**
	 * The cursor that is used to represent `MardekCursor.Grab`.
	 *
	 * This cursor is used while the player is holding an item (while the inventory is open).
	 */
	@BitField(id = 10)
	val grabCursor: KimSprite,
) : Serializable
