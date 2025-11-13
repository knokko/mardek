package mardek.content.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.content.sprite.KimSprite
import java.util.*

/**
 * Represents a switch color: the color that switch orbs, switch gates, and switch platforms have. Switch orbs are
 * 'linked' to switch gates and switch platforms with the same *color* in the same dungeon. The switch color also
 * determines their sprites.
 */
@BitStruct(backwardCompatible = true)
class SwitchColor(

	/**
	 * The name of the switch color. This field is not used in-game, but is potentially useful for debugging and
	 * editing.
	 */
	@BitField(id = 0)
	val name: String,

	/**
	 * The sprite that switch orbs with this color have when they are off/disabled.
	 */
	@BitField(id = 1)
	val offSprite: KimSprite,

	/**
	 * The sprite that switch orbs with this color have when they are on/enabled.
	 */
	@BitField(id = 2)
	val onSprite: KimSprite,

	/**
	 * The sprite that switch gates with this color have when they are off/disabled.
	 */
	@BitField(id = 3)
	val gateSprite: KimSprite,

	/**
	 * The sprite that switch platforms with this color have when they are on/enabled.
	 */
	@BitField(id = 4)
	val platformSprite: KimSprite,

	/**
	 * The unique ID of this color, which is used for (de)serialization.
	 */
	@BitField(id = 5)
	@StableReferenceFieldId
	val id: UUID,
) {

	internal constructor() : this(
		"", KimSprite(), KimSprite(),
		KimSprite(), KimSprite(), UUID.randomUUID(),
	)
}
