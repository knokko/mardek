package mardek.content.stats

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import mardek.content.sprite.KimSprite

/**
 * Represents the creature type/race of a combatant.
 *
 * This determines whether the creature can be healed (basically any creature type except UNDEAD).
 * Furthermore, some reaction skills (e.g. `Quarry: BEAST`) can be used to deal more damage against certain creature
 * types.
 */
@BitStruct(backwardCompatible = true)
class CreatureType(

	/**
	 * The name of the creature type, as imported from Flash. This is shown in the combatant info modal.
	 */
	@BitField(id = 0)
	val flashName: String,

	/**
	 * The icon of the creature type, which is shown in the turn order.
	 */
	@BitField(id = 1)
	val icon: KimSprite,

	/**
	 * Whether the creature type reverts healing (only true for UNDEAD)
	 */
	@BitField(id = 2)
	val revertsHealing: Boolean,

	/**
	 * The 'nice' name of the creature type, which is currently only used in the "People" section of the
	 * encyclopedia.
	 *
	 * Note that this can be the empty string if this creature type is never used in the "People" section of the
	 * encyclopedia.
	 */
	@BitField(id = 3)
	val niceName: String,
) {

	constructor() : this("", KimSprite(), false, "")

	override fun toString() = flashName
}
