package mardek.content.stats

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.ReferenceFieldTarget

/**
 * All the 'stats'-related content of the game: elements, status effects, creature types, etc...
 */
@BitStruct(backwardCompatible = true)
class StatsContent(

	/**
	 * All the elements (fire, water, etc...)
	 */
	@BitField(id = 0)
	@ReferenceFieldTarget(label = "elements")
	val elements: ArrayList<Element>,

	/**
	 * All the status effects (poison, numb, shield, etc...)
	 */
	@BitField(id = 1)
	@ReferenceFieldTarget(label = "status effects")
	val statusEffects: ArrayList<StatusEffect>,

	/**
	 * All the creature types that monsters and playable characters can have. Creature types are used for the
	 * `QUARRY: X` skills, but have little other impact.
	 */
	@BitField(id = 2)
	@ReferenceFieldTarget(label = "creature types")
	val creatureTypes: ArrayList<CreatureType>,

	/**
	 * All the 'classes' that playable characters can have. The class of a character determines the active skills which
	 * the character can learn, and determines the types of armor that the character can wear.
	 */
	@BitField(id = 3)
	@ReferenceFieldTarget(label = "character classes")
	val classes: ArrayList<CharacterClass>,

	/**
	 * The type of damage that weapons without an element will deal. This should be `PHYSICAL`/`NONE`
	 */
	@BitField(id = 4)
	@ReferenceField(stable = false, label = "elements")
	var defaultWeaponElement: Element,
) {

	constructor() : this(
		ArrayList(0), ArrayList(0),
		ArrayList(0), ArrayList(0),
		Element(),
	)
}
