package mardek.content.skill

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceFieldTarget
import mardek.content.sprite.KimSprite

/**
 * The part of a [mardek.content.stats.CharacterClass] that determines which [ActiveSkill]s the character
 * can learn. Since this is a separate class, it allows different characters to have the same skill class, but a
 * different character class (e.g. child Mardek and child Deugan)
 */
@BitStruct(backwardCompatible = true)
class SkillClass(

	/**
	 * The 'key' of this class, which is only used during importing, and for some unit tests
	 */
	@BitField(id = 0)
	val key: String,

	/**
	 * The display name of this skill class, which is shown in e.g. the "Skills" tab of the in-game menu.
	 */
	@BitField(id = 1)
	val name: String,

	/**
	 * The description of this skill class, which is shown in e.g. the "Skills" tab of the in-game menu.
	 */
	@BitField(id = 2)
	val description: String,

	/**
	 * The active skills that characters with this skill class can use and learn
	 */
	@BitField(id = 3)
	@ReferenceFieldTarget(label = "skills")
	val actions: ArrayList<ActiveSkill>,

	/**
	 * The icon of this skill class, which is shown in the "Skills" tab of the in-game menu, as well as in
	 * combat.
	 */
	@BitField(id = 4)
	val icon: KimSprite,
) {

	constructor() : this("", "", "", ArrayList(0), KimSprite())

	override fun toString() = name
}
