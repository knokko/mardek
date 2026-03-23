package mardek.content.skill

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.stats.*
import java.util.UUID

/**
 * Represents a passive skill that players can toggle in the "Skills" tab. These skills have one or more 'passive'
 * effects (e.g. Insomnia grants immunity to the Sleep status effect). Unlike [ReactionSkill]s, these skills will
 * also work if the player fails the reaction bar challenge.
 */
@BitStruct(backwardCompatible = true)
class PassiveSkill(
	name: String,
	description: String,
	element: Element,
	masteryPoints: Int,
	id: UUID,

	/**
	 * The number of points that it costs to enable this skill in the "Skills" tab. The number of points that players
	 * can spend depends on their level.
	 */
	@BitField(id = 0)
	@IntegerField(expectUniform = false, minValue = 0)
	val enablePoints: Int,

	/**
	 * The HP modifier of this skill: when this skill is enabled, the maximum HP of the player is multiplied by
	 * `1 + hpModifier`.
	 */
	@BitField(id = 1)
	@FloatField(expectMultipleOf = 0.05)
	val hpModifier: Float,

	/**
	 * The MP modifier of this skill: when this skill is enabled, the maximum MP of the player is multiplied by
	 * `1 + mpModifier`.
	 */
	@BitField(id = 2)
	@FloatField(expectMultipleOf = 0.05)
	val mpModifier: Float,

	/**
	 * The stats that are increased when this skill is enabled. This is used by e.g. the `DEF+1` skill.
	 */
	@BitField(id = 3)
	val statModifiers: ArrayList<StatModifier>,

	/**
	 * The elemental resistances that are increased when this skill is enabled. This is used by e.g. `Resist FIRE`.
	 */
	@BitField(id = 4)
	val resistances: Resistances,

	/**
	 * The status effects that the player gets when this skill is enabled. The player will get these status effects at
	 * the start of every battle, and these cannot be removed until the battle is over. This is used by e.g. the
	 * `Auto-Regen` skill.
	 */
	@BitField(id = 5)
	@ReferenceField(stable = false, label = "status effects")
	val autoEffects: HashSet<StatusEffect>,

	/**
	 * When this skill is enabled and the players health falls below 20% for the first time (each battle), the player
	 * will get these status effects.
	 */
	@BitField(id = 6)
	@ReferenceField(stable = false, label = "status effects")
	val sosEffects: HashSet<StatusEffect>,

	/**
	 * When the player gains EXP while this skill is enabled, the player will get an additional
	 * `originalAmount * experienceModifier`.
	 */
	@BitField(id = 7)
	@FloatField(expectMultipleOf = 0.05)
	val experienceModifier: Float,

	/**
	 * When the player gains a skill mastery point while this skill is enabled, the player will get `masteryModifier`
	 * extra mastery points.
	 */
	@BitField(id = 8)
	@IntegerField(expectUniform = false)
	val masteryModifier: Int,

	/**
	 * When the player gets gold (from battle loot) while this skill is enabled, the amount of gold is increased by
	 * `originalAmount * goldModifier`.
	 */
	@BitField(id = 9)
	@IntegerField(expectUniform = false)
	val goldModifier: Int,

	/**
	 * When this skill is enabled, the drop chance of all potential loot (after the player wins a battle) is increased
	 * by `addLootChance` % points.
	 */
	@BitField(id = 10)
	@IntegerField(expectUniform = false)
	val addLootChance: Int,

	/**
	 * For most skills, this is `null`, which means that any player can enable the skill. When this is non-null, only
	 * players with this skill class can enable/use the skill. This is used by e.g. Dragon's Blood that only Sslenck
	 * can use.
	 */
	@BitField(id = 11, optional = true)
	@ReferenceField(stable = false, label = "skill classes")
	val skillClass: SkillClass?,
): Skill(name, description, element, masteryPoints, id) {

	@Suppress("unused")
	private constructor() : this(
		"", "", Element(), 0, UUID.randomUUID(), 0, 0f,
		0f, ArrayList(), Resistances(), HashSet(), HashSet(),
		0f, 0, 0, 0, null,
	)

	/**
	 * Gets the modifier for [stat]. This simply iterates over [statModifiers].
	 */
	fun getModifier(stat: CombatStat): Int {
		var total = 0
		for (modifier in statModifiers) {
			if (modifier.stat == stat) total += modifier.adder
		}
		return total
	}
}
