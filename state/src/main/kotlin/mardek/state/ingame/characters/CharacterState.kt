package mardek.state.ingame.characters

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import mardek.assets.combat.StatusEffect
import mardek.assets.inventory.Item
import mardek.assets.skill.Skill
import mardek.state.ingame.inventory.ItemStack

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
