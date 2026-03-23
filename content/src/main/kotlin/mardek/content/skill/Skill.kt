package mardek.content.skill

import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.content.stats.Element
import java.util.*

/**
 * Represents a skill that playable characters can use or toggle. This is an abstract class with 3 subclasses:
 * - [ActiveSkill]s are the skills that characters can cast/use directory
 * - [PassiveSkill]s are the skills that characters can toggle, and have some passive effect while the skill is enabled
 * - [ReactionSkill]s are similar to passive skills, but their effects are only applied when the player passes the
 * reaction challenge
 */
abstract class Skill(

	/**
	 * The display name of the skill, which is shown in the UI
	 */
	@BitField(id = 0)
	val name: String,

	/**
	 * The description of the skill, which is shown in the UI
	 */
	@BitField(id = 1)
	val description: String,

	/**
	 * The element of the skill. For [ActiveSkill]s, this determines the type of damage that the skill does. For
	 * passive/reaction skills, the element is not important, but is still shown in the UI.
	 */
	@BitField(id = 2)
	@ReferenceField(stable = false, label = "elements")
	val element: Element,

	/**
	 * The number of points needed to *master* the skill.
	 *
	 * - Before a playable character masters a skill, the skill can only be used/toggled when the character equips an
	 * item that has the skill.
	 * - But, once the character *master*s the skill, it can be used/toggled even after that item is unequipped.
	 *
	 * Initially, the character has 0 points for each skill, which means that it can only be used when the right item is
	 * equipped.
	 * - When the skill is an [ActiveSkill], the character gains a point each time the skill is cast/used
	 * - When the skill is a [ReactionSkill], the character gains a point each time the skill is triggered. For instance,
	 * che character will get a point for each `MeleeAttack` when it performs a melee attack, and passes the reaction bar
	 * challenge.
	 * - When the skill is a [PassiveSkill], the character gains a point after winning any battle.
	 */
	@BitField(id = 3)
	@IntegerField(expectUniform = false, minValue = -1, commonValues=[0, 20])
	val masteryPoints: Int,

	/**
	 * The unique ID of this skill, which is used for (de)serialization
	 */
	@BitField(id = 4)
	@StableReferenceFieldId
	val id: UUID,
) {

	override fun toString() = name

	override fun equals(other: Any?) = other is Skill && this.id == other.id

	override fun hashCode() = id.hashCode()
}
