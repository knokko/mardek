package mardek.state.ingame.characters

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import mardek.assets.combat.CombatStat
import mardek.assets.combat.StatModifier
import mardek.assets.combat.StatusEffect
import mardek.assets.inventory.Item
import mardek.assets.skill.PassiveSkill
import mardek.assets.skill.Skill
import mardek.assets.inventory.ItemStack
import kotlin.math.roundToInt

@BitStruct(backwardCompatible = false)
class CharacterState {

	@BitField(ordering = 0)
	@IntegerField(expectUniform = false, minValue = 0)
	var currentHealth = 0

	@BitField(ordering = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	var currentMana = 0

	@BitField(ordering = 2)
	@IntegerField(expectUniform = false, minValue = 1)
	var currentLevel = 0

	@BitField(ordering = 3)
	@ReferenceField(stable = true, label = "status effects")
	val activeStatusEffects = HashSet<StatusEffect>()

	@BitField(ordering = 4)
	@ReferenceField(stable = true, label = "items")
	@NestedFieldSetting(path = "c", optional = true)
	val equipment = Array<Item?>(6) { null }

	@BitField(ordering = 5)
	@NestedFieldSetting(path = "c", optional = true)
	val inventory = Array<ItemStack?>(64) { null }

	@BitField(ordering = 6)
	@NestedFieldSetting(path = "k", fieldName = "SKILL_MASTERY_KEY")
	@NestedFieldSetting(path = "v", fieldName = "SKILL_MASTERY_VALUE")
	val skillMastery = HashMap<Skill, Int>()

	@BitField(ordering = 7)
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
		return ((1f + hpModifier) * (3 * vit + 2 * vit * currentLevel + extra)).roundToInt()
	}

	fun determineMaxMana(base: List<StatModifier>, allStats: List<CombatStat>): Int {
		val spr = determineValue(base, allStats.find { it.flashName == "SPR" }!!)
		val extra = determineValue(base, allStats.find { it.flashName == "mp" }!!)
		var mpModifier = 0f
		for (skill in toggledSkills) {
			if (skill is PassiveSkill) mpModifier += skill.mpModifier
		}
		return ((1f + mpModifier) * (spr * 17 / 6 + spr * currentLevel / 6 + extra)).roundToInt()
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
	}
}
