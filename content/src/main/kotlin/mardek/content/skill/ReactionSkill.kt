package mardek.content.skill

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.stats.*
import java.util.UUID

/**
 * Represents a reaction skill that players can toggle in the "Skills" tab. These skills have one or more 'passive'
 * effects (e.g. `Block` removes 1 point from all incoming melee damage). Unlike [PassiveSkill]s, these skills only
 * work if the player passes the reaction bar challenge.
 */
@BitStruct(backwardCompatible = true)
class ReactionSkill(
	name: String,
	description: String,
	element: Element,
	masteryPoints: Int,
	id: UUID,

	/**
	 * The type of this reaction skill, which determines when this skill can be applied. For instance,
	 * `ReactionSkillType.MeleeAttack` only works when the player performs a melee attack.
	 */
	@BitField(id = 0)
	val type: ReactionSkillType,

	/**
	 * The number of points that it costs to enable this skill in the "Skills" tab. The number of points that players
	 * can spend depends on their level.
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	val enablePoints: Int,

	/**
	 * For most skills, this is `null`, which means that any player can enable the skill. When this is non-null, only
	 * players with this skill class can enable/use the skill. This is used by e.g. Smite+ that only Vehrn can use.
	 */
	@BitField(id = 2, optional = true)
	@ReferenceField(stable = false, label = "skill classes")
	val skillClass: SkillClass?,

	/**
	 * When this skill is enabled and the reaction challenge is passed, this amount is added to the damage. When this
	 * is negative, the damage will be decreased.
	 * - This is typically positive for `MeleeAttack` or `RangedAttack` skills
	 * - This is typically negative for `MeleeDefense` or `RangedDefense` skills
	 */
	@BitField(id = 3)
	@IntegerField(expectUniform = false)
	val addFlatDamage: Int,

	/**
	 * When this skill is enabled and the reaction challenge is passed, the damage is multiplied by
	 * `1 + addDamageFraction`. When multiple reaction skills are enabled, the damage is multiplied by
	 * `1 + skills.sumOf { it.addDamageFraction }`
	 */
	@BitField(id = 4)
	@FloatField(expectMultipleOf = 0.1)
	val addDamageFraction: Float,

	/**
	 * When this skill is enabled and the reaction challenge is passed, the chance to get a critical hit is increased
	 * by `addCritChance`%.
	 */
	@BitField(id = 5)
	@IntegerField(expectUniform = false, minValue = 0, maxValue = 100)
	val addCritChance: Int,

	/**
	 * When this skill is enabled and the reaction challenge is passed, the hit chance is increased by
	 * `addAccuracy`%.
	 */
	@BitField(id = 6)
	@IntegerField(expectUniform = false)
	val addAccuracy: Int,

	/**
	 * When this skill is enabled and the reaction challenge is passed, the attacker regains `drainHp * finalDamage`
	 * HP. Only `MeleeAttack` and `RangedAttack` skills should have `drainHp != 0f`; draining HP doesn't make sense
	 * for defensive reaction skills.
	 */
	@BitField(id = 7)
	@FloatField(expectMultipleOf = 0.1)
	val drainHp: Float,

	/**
	 * The meaning of this property depends on whether this reaction skill is offensive or defensive:
	 *
	 * ### Offensive reaction skill meaning
	 * When this skill is enabled and the reaction challenge is passed, the **attacker** regains
	 * `absorbMp * finalDamage` MP.
	 *
	 * ### Defensive reaction skill meaning
	 * When this skill is enabled and the reaction challenge is passed, the **target** regains `absorbMp * manaCost`
	 * MP.
	 */
	@BitField(id = 8)
	@FloatField(expectMultipleOf = 0.1)
	val absorbMp: Float,

	/**
	 * When this skill is enabled and the reaction challenge is passed, these damage bonuses (or resistances) will be
	 * applied to attacks with the right element.
	 */
	@BitField(id = 9)
	val elementalBonuses: ArrayList<ElementalDamageBonus>,

	/**
	 * When this skill is enabled and the reaction challenge is passed, the target may get these status effects. This
	 * list should only be non-empty for attack skills; it doesn't make any sense for defensive reaction skills.
	 */
	@BitField(id = 10)
	val addStatusEffects: ArrayList<PossibleStatusEffect>,

	/**
	 * When this skill is enabled and the reaction challenge is passed, these status effects may be removed from the
	 * target. This list should only be non-empty for attack skills; it doesn't make any sense for defensive
	 * reaction skills.
	 */
	@BitField(id = 11)
	val removeStatusEffects: ArrayList<PossibleStatusEffect>,

	/**
	 * When this skill is enabled and the reaction challenge is passed, these damage bonuses will be applied to attacks
	 * against enemies with the right creature type. This list should only be non-empty for attack skills;
	 * it doesn't make any sense for defensive reaction skills.
	 */
	@BitField(id = 12)
	val effectiveAgainst: ArrayList<CreatureTypeBonus>,

	/**
	 * Some damage bonus for the Smite+ skill. TODO CHAP2 Figure this out
	 */
	@BitField(id = 13)
	val smitePlus: Boolean,

	/**
	 * When this skill is enabled and the reaction challenge is passed, basic attacks will use the element of the
	 * attacker rather than the element of the weapon. This should only be `true` for skills of type `MeleeAttack`;
	 * it wouldn't make sense on any other skills.
	 */
	@BitField(id = 14)
	val soulStrike: Boolean,

	/**
	 * When this skill is enabled and the reaction challenge is passed, the target will survive the attack with at
	 * least 1 HP, unless the target only had 1 HP to begin with. This should only be `true` for defensive reaction
	 * skills; it wouldn't make any sense on offensive skills.
	 */
	@BitField(id = 15)
	val survivor: Boolean,
): Skill(name, description, element, masteryPoints, id) {

	@Suppress("unused")
	private constructor() : this(
		"", "", Element(), 0, UUID.randomUUID(),
		ReactionSkillType.RangedDefense, 0, null, 0,
		0f, 0, 0, 0f, 0f,
		ArrayList(0), ArrayList(0),
		ArrayList(0), ArrayList(0),
		false, false, false,
	)

	/**
	 * Gets the sum of all bonuses (or resistances, which are negative) against the given element
	 */
	fun getElementalBonus(element: Element) = elementalBonuses.sumOf {
		if (it.element === element) it.modifier.toDouble() else 0.0
	}.toFloat()

	/**
	 * Gets the sum of all bonuses against the given creature type
	 */
	fun getCreatureTypeBonus(creatureType: CreatureType) = effectiveAgainst.sumOf {
		if (it.type === creatureType) it.modifier.toDouble() else 0.0
	}.toFloat()
}
