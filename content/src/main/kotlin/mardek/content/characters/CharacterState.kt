package mardek.content.characters

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.inventory.EquipmentSlot
import mardek.content.inventory.Item
import mardek.content.inventory.ItemStack
import mardek.content.skill.ActiveSkill
import mardek.content.skill.PassiveSkill
import mardek.content.skill.Skill
import mardek.content.stats.CombatStat
import mardek.content.stats.StatModifier
import mardek.content.stats.StatusEffect
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Represents the state of a `PlayableCharacter`. This class is part of `mardek.content` rather than `mardek.state`
 * because we need it to define default states of playable characters in the content.
 */
@BitStruct(backwardCompatible = true)
class CharacterState {

	/**
	 * The current health of the player, *outside of combat*, which should be somewhere between 1 and the maximum
	 * health. Note that the battle state is responsible for tracking the in-battle health, which is transferred
	 * after the battle ends.
	 */
	@BitField(id = 0)
	@IntegerField(expectUniform = false, minValue = 1)
	var currentHealth = 0

	/**
	 * The current mana of the player, *outside of combat*, which should be somewhere between 0 and the maximum mana.
	 * Note that the battle state is responsible for tracking the in-battle mana, which is transferred after the battle
	 * ends.
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	var currentMana = 0

	/**
	 * The current level of the player. TODO CHAP1 Add experience field
	 */
	@BitField(id = 2)
	@IntegerField(expectUniform = false, minValue = 1)
	var currentLevel = 0

	/**
	 * The status effects that the player currently has, *outside of combat*. This should only contain *persistent*
	 * status effects. Note that the battle state is responsible for tracking the in-battle status effects. Only the
	 * *persistent* status effects are transferred after the battle ends.
	 */
	@BitField(id = 3)
	@ReferenceField(stable = true, label = "status effects")
	val activeStatusEffects = HashSet<StatusEffect>()

	/**
	 * The current equipment of the player. It maps each equipment slot of the player to the item equipped in that slot.
	 * When an equipment slot is empty, it won't be present as key in this map.
	 */
	@BitField(id = 4)
	@NestedFieldSetting(path = "k", fieldName = "EQUIPMENT_KEY_PROPERTIES")
	@NestedFieldSetting(path = "v", fieldName = "EQUIPMENT_VALUE_PROPERTIES")
	val equipment = HashMap<EquipmentSlot, Item>()

	/**
	 * The current inventory of the player, where the item in slot (x, y) is stored at index `x + 8 * y`.
	 */
	@BitField(id = 5)
	@NestedFieldSetting(path = "c", optional = true)
	val inventory = Array<ItemStack?>(64) { null }

	/**
	 * This map tracks which skills this player is mastering. This map contains each skill for which the player has at
	 * least 1 mastery point, and maps such skills to the exact number of mastery points for that skill. When
	 * `characterState.skillMastery.get(skill) >= skill.masteryPoints`, the player has mastered the skill.
	 */
	@BitField(id = 6)
	@NestedFieldSetting(path = "k", fieldName = "SKILL_MASTERY_KEY")
	@NestedFieldSetting(path = "v", fieldName = "SKILL_MASTERY_VALUE")
	val skillMastery = HashMap<Skill, Int>()

	/**
	 * The set of reaction & passive skills that are currently toggled (enabled) for this player. Reaction skills and
	 * passive skills don't do anything if they are not toggled.
	 */
	@BitField(id = 7)
	@ReferenceField(stable = true, label = "skills")
	val toggledSkills = HashSet<Skill>()

	/**
	 * If this player recently took damage while walking inside an area (e.g. due to being poisoned), this field will
	 * store some information needed to render the damage 'blink'.
	 */
	var lastWalkDamage: WalkDamage? = null

	/**
	 * This method should be called after a `CharacterState` is initialized. It will find all items that the
	 * given playable character is no longer allowed to equipment, and send them to the item storage.
	 */
	fun initialize(character: PlayableCharacter, itemStorage: ArrayList<ItemStack?>) {
		val iterator = equipment.iterator()
		while (iterator.hasNext()) {
			val (slot, equippedItem) = iterator.next()
			if (!character.characterClass.equipmentSlots.contains(slot) ||
				!slot.isAllowed(equippedItem, character)
			) {
				iterator.remove()
				itemStorage.add(ItemStack(equippedItem, 1))
			}
		}
	}

	/**
	 * Computes the value that this character has for the given `stat`. For instance, if `stat == CombatStat.Attack`,
	 * this method computes the current ATK of the character.
	 * - `base` should be the `baseStats` of the corresponding `PlayableCharacter`
	 * - `statusEffects` should be `this.activeStatusEffects` (outside combat) or `combatantState.statusEffects`
	 * (in combat)
	 *
	 * This method takes the current equipment, toggled skills, and given status effects into account.
	 */
	fun computeStatValue(base: List<StatModifier>, statusEffects: Set<StatusEffect>, stat: CombatStat): Int {
		var total = 0
		for (modifier in base) {
			if (modifier.stat == stat) total += modifier.adder
		}
		for (item in equipment.values) total += item.getModifier(stat)
		for (skill in toggledSkills) {
			if (skill is PassiveSkill) total += skill.getModifier(stat)
		}
		for (effect in statusEffects) {
			total += effect.getModifier(stat)
		}

		return total
	}

	/**
	 * Determines the maximum health that this character should have, based on the given `base` stats and status
	 * effects, as well as the current equipment and toggled skills.
	 * - Outside combat, `base` should simply be the `baseStats` of the corresponding `PlayableCharacter`.
	 * Inside combat, `base` should additionally contain any relevant `statModifiers` of the corresponding
	 * `CombatantState.
	 * * - `statusEffects` should be `this.activeStatusEffects` (outside combat) or `combatantState.statusEffects`
	 * (in combat)
	 */
	fun determineMaxHealth(base: List<StatModifier>, statusEffects: Set<StatusEffect>): Int {
		val vit = computeStatValue(base, statusEffects, CombatStat.Vitality)
		val extra = computeStatValue(base, statusEffects, CombatStat.MaxHealth)
		var hpModifier = 0f
		for (skill in toggledSkills) {
			if (skill is PassiveSkill) hpModifier += skill.hpModifier
		}
		return determineMaxHealth(currentLevel, vit, hpModifier, extra)
	}

	/**
	 * Determines the maximum mana that this character should have, based on the given `base` stats and status
	 * effects, as well as the current equipment and toggled skills.
	 * - Outside combat, `base` should simply be the `baseStats` of the corresponding `PlayableCharacter`.
	 * Inside combat, `base` should additionally contain any relevant `statModifiers` of the corresponding
	 * `CombatantState.
	 * * - `statusEffects` should be `this.activeStatusEffects` (outside combat) or `combatantState.statusEffects`
	 * (in combat)
	 */
	fun determineMaxMana(base: List<StatModifier>, statusEffects: Set<StatusEffect>): Int {
		val spr = computeStatValue(base, statusEffects, CombatStat.Spirit)
		val extra = computeStatValue(base, statusEffects, CombatStat.MaxMana)
		var mpModifier = 0f
		for (skill in toggledSkills) {
			if (skill is PassiveSkill) mpModifier += skill.mpModifier
		}
		return determineMaxMana(currentLevel, spr, mpModifier, extra)
	}

	/**
	 * Determines the number of points that this character can use to toggle reaction skills and passive skill.
	 * Toggling each skill costs `skill.enablePoints` points.
	 */
	fun determineSkillEnablePoints() = 3 + (currentLevel / 2) + (currentLevel % 2)

	/**
	 * Determines the **auto** `StatusEffect`s that this character should have, based on its current equipment and
	 * the toggled skills.
	 */
	fun determineAutoEffects(): Set<StatusEffect> {
		val effects = HashSet<StatusEffect>()
		for (item in equipment.values) {
			val equipment = item.equipment ?: continue
			effects.addAll(equipment.autoEffects)
		}
		for (skill in toggledSkills) {
			if (skill !is PassiveSkill) continue
			effects.addAll(skill.autoEffects)
		}
		return effects
	}

	/**
	 * Check whether this character should be allowed to cast `skill`. Casting skills is allowed when either:
	 * - this character has *mastered* the skill, or when
	 * - the equipment of this character allows the skill to be cast
	 */
	fun canCastSkill(skill: ActiveSkill): Boolean {
		val mastery = skillMastery[skill]
		if (mastery != null && mastery >= skill.masteryPoints) return true
		return equipment.values.any { it.equipment != null && it.equipment.skills.contains(skill) }
	}

	/**
	 * Counts how many times this character has `item` in its inventory and equipment.
	 */
	fun countItemOccurrences(item: Item): Int {
		var count = 0
		for (candidate in equipment.values) {
			if (candidate === item) count += 1
		}

		for (stack in inventory) {
			if (stack != null && stack.item === item) count += stack.amount
		}

		return count
	}

	/**
	 * Attempts to add `stackToGive` to the inventory of this character. Returns `true` if it was added, and `false`
	 * if the item could not be added due to a lack of inventory space.
	 */
	fun giveItemStack(stackToGive: ItemStack): Boolean {
		for ((index, existingStack) in inventory.withIndex()) {
			if (existingStack != null && existingStack.item === stackToGive.item) {
				inventory[index] = ItemStack(
					existingStack.item, existingStack.amount + stackToGive.amount
				)
				return true
			}
		}

		for ((index, existingStack) in inventory.withIndex()) {
			if (existingStack == null) {
				inventory[index] = stackToGive
				return true
			}
		}

		return false
	}

	/**
	 * Attempts to remove 1 instance of `item` from the inventory of this character, and returns true on success.
	 * Note that the `equipment` will *not* be touched.
	 */
	fun removeItem(item: Item): Boolean {
		for ((index, stack) in inventory.withIndex()) {
			if (stack != null && stack.item === item) {
				if (stack.amount > 1) {
					inventory[index] = ItemStack(item, stack.amount - 1)
				} else {
					inventory[index] = null
				}
				return true
			}
		}

		return false
	}

	companion object {

		@Suppress("unused")
		@ReferenceField(stable = true, label = "skills")
		private const val SKILL_MASTERY_KEY = false

		@Suppress("unused")
		@IntegerField(expectUniform = false, minValue = 1)
		private const val SKILL_MASTERY_VALUE = false

		@Suppress("unused")
		@ReferenceField(stable = true, label = "equipment slots")
		private const val EQUIPMENT_KEY_PROPERTIES = false

		@Suppress("unused")
		@ReferenceField(stable = true, label = "items")
		private const val EQUIPMENT_VALUE_PROPERTIES = false

		/**
		 * Determines the maximum health of a playable character, based on its `level` and `vitality`, as well as a
		 * modifier (e.g. skills like +50% health). Finally, the result is increased by `extra`.
		 */
		fun determineMaxHealth(
			level: Int, vitality: Int, modifier: Float, extra: Int
		) = max(1, ((1f + modifier) * (3f * vitality + 2f * vitality * level + extra)).toInt())

		/**
		 * Determines the maximum mana of a playable character, based on its `level` and `spirit`, as well as a
		 * modifier (e.g. skills like +50% mana). Finally, the result is increased by `extra`.
		 */
		fun determineMaxMana(
			level: Int, spirit: Int, modifier: Float, extra: Int
		) = max(0, ((1f + modifier) * (spirit * 17f / 6f + spirit * level / 6f + extra)).toInt())
	}

	/**
	 * Represents an instant in time where a playable character took damage outside combat (e.g. walking through an area
	 * while poisoned). This is needed to render the green poison damage 'blink'.
	 */
	class WalkDamage(
		/**
		 * The blink color, which would be green for poison.
		 */
		val color: Int
	) {

		/**
		 * The approximate time at which the character took damage: the result of `System.nanoTime()` at that instant
		 * in time.
		 */
		val time = System.nanoTime()
	}
}