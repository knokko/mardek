package mardek.state.ingame.characters

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.stats.CombatStat
import mardek.content.stats.StatModifier
import mardek.content.stats.StatusEffect
import mardek.content.inventory.Item
import mardek.content.skill.PassiveSkill
import mardek.content.skill.Skill
import mardek.content.inventory.ItemStack
import kotlin.math.roundToInt

@BitStruct(backwardCompatible = true)
class CharacterState {

	@BitField(id = 0)
	@IntegerField(expectUniform = false, minValue = 0)
	var currentHealth = 0

	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	var currentMana = 0

	@BitField(id = 2)
	@IntegerField(expectUniform = false, minValue = 1)
	var currentLevel = 0

	@BitField(id = 3)
	@ReferenceField(stable = true, label = "status effects")
	val activeStatusEffects = HashSet<StatusEffect>()

	@BitField(id = 4)
	@ReferenceField(stable = true, label = "items")
	@NestedFieldSetting(path = "c", optional = true)
	val equipment = Array<Item?>(6) { null }

	@BitField(id = 5)
	@NestedFieldSetting(path = "c", optional = true)
	val inventory = Array<ItemStack?>(64) { null }

	@BitField(id = 6)
	@NestedFieldSetting(path = "k", fieldName = "SKILL_MASTERY_KEY")
	@NestedFieldSetting(path = "v", fieldName = "SKILL_MASTERY_VALUE")
	val skillMastery = HashMap<Skill, Int>()

	@BitField(id = 7)
	@ReferenceField(stable = true, label = "skills")
	val toggledSkills = HashSet<Skill>()

	fun determineValue(base: List<StatModifier>, stat: CombatStat): Int {
		var total = 0
		for (modifier in base) {
			if (modifier.stat == stat) total += modifier.adder
		}
		for (item in equipment) {
			if (item != null) total += item.getModifier(stat)
		}
		for (skill in toggledSkills) {
			if (skill is PassiveSkill) total += skill.getModifier(stat)
		}
		// TODO Allow status effects to modify DEF

		return total
	}

	fun determineMaxHealth(base: List<StatModifier>, allStats: List<CombatStat>): Int {
		val vit = determineValue(base, allStats.find { it.flashName == "VIT" }!!)
		val extra = determineValue(base, allStats.find { it.flashName == "hp" }!!)
		var hpModifier = 0f
		for (skill in toggledSkills) {
			if (skill is PassiveSkill) hpModifier += skill.hpModifier
		}
		return determineMaxHealth(currentLevel, vit, hpModifier, extra)
	}

	fun determineMaxMana(base: List<StatModifier>, allStats: List<CombatStat>): Int {
		val spr = determineValue(base, allStats.find { it.flashName == "SPR" }!!)
		val extra = determineValue(base, allStats.find { it.flashName == "mp" }!!)
		var mpModifier = 0f
		for (skill in toggledSkills) {
			if (skill is PassiveSkill) mpModifier += skill.mpModifier
		}
		return determineMaxMana(currentLevel, spr, mpModifier, extra)
	}

	fun determineSkillEnablePoints() = 3 + (currentLevel / 2) + (currentLevel % 2)

	fun determineAutoEffects(): Set<StatusEffect> {
		val effects = HashSet<StatusEffect>()
		for (item in equipment) {
			val equipment = item?.equipment ?: continue
			effects.addAll(equipment.autoEffects)
		}
		for (skill in toggledSkills) {
			if (skill !is PassiveSkill) continue
			effects.addAll(skill.autoEffects)
		}
		return effects
	}

	companion object {

		@Suppress("unused")
		@JvmStatic
		@ReferenceField(stable = true, label = "skills")
		private val SKILL_MASTERY_KEY = false

		@Suppress("unused")
		@JvmStatic
		@IntegerField(expectUniform = false, minValue = 1)
		val SKILL_MASTERY_VALUE = false

		fun determineMaxHealth(
			level: Int, vitality: Int, modifier: Float, extra: Int
		) = ((1f + modifier) * (3 * vitality + 2 * vitality * level + extra)).roundToInt()

		fun determineMaxMana(
			level: Int, spirit: Int, modifier: Float, extra: Int
		) = ((1f + modifier) * (spirit * 17 / 6 + spirit * level / 6 + extra)).roundToInt()
	}
}
