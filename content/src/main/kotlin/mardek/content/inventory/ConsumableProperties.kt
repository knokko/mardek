package mardek.content.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.characters.CharacterState
import mardek.content.characters.PlayableCharacter
import mardek.content.particle.ParticleEffect
import mardek.content.stats.PossibleStatusEffect
import mardek.content.stats.StatModifierRange
import kotlin.math.min
import kotlin.random.Random

/**
 * The properties that only consumable items have.
 */
@BitStruct(backwardCompatible = true)
class ConsumableProperties(

	/**
	 * The particle effect that will be spawned at the consumer when the consumable is used in combat.
	 */
	@BitField(id = 0, optional = true)
	@ReferenceField(stable = false, label = "particles")
	val particleEffect: ParticleEffect?,

	/**
	 * The 'blink color' that the consumer will temporarily get when the consumable is used in combat.
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = true)
	val blinkColor: Int,

	/**
	 * - When true, this consumable will restore all health & mana of the consumer.
	 * - When false, the amount of restored health & mana are determined by `restoreHealth` and `restoreMana`.
	 */
	@BitField(id = 2)
	val isFullCure: Boolean,

	/**
	 * The amount of health that the consumer will regain
	 */
	@BitField(id = 3)
	@IntegerField(expectUniform = false, minValue = 0)
	val restoreHealth: Int,

	/**
	 * The amount of mana that the consumer will regain
	 */
	@BitField(id = 4)
	@IntegerField(expectUniform = false, minValue = 0)
	val restoreMana: Int,

	/**
	 * When this is `0`, the consumable can *not* be used on fainted/KO'd characters. When this is non-zero, the
	 * consumable can be used on fainted characters to restore `revive * target.maxHealth` health. For instance,
	 * the `revive` of the PhoenixDown is `0.5`, and the revive of the PhoenixPinion is `1.0`.
	 */
	@BitField(id = 5)
	@FloatField(expectMultipleOf = 0.5)
	val revive: Float,

	/**
	 * The status effects that the consumer may/will get upon consuming this item. This is used by items like
	 * Speed Juice, which has 100% chance to give Haste.
	 */
	@BitField(id = 6)
	val addStatusEffects: ArrayList<PossibleStatusEffect>,

	/**
	 * The status effects that can/will be removed from the consumer upon consuming this item. This is used by items
	 * like Antidote, which has 100% chance to remove Poison.
	 */
	@BitField(id = 7)
	val removeStatusEffects: ArrayList<PossibleStatusEffect>,

	/**
	 * When true, all negative status effects will be removed from the consumer. This is used by the Remedy.
	 */
	@BitField(id = 8)
	val removeNegativeStatusEffects: Boolean,

	/**
	 * The stat modifiers that the consumer can/will get. This is used by items like Power Drink, which increase the
	 * strength of the consumer for the remainder of the battle.
	 */
	@BitField(id = 9)
	val statModifiers: ArrayList<StatModifierRange>,

	/**
	 * When non-null, this consumable will deal damage to the consumer. This is used by items like Liquid Lightning.
	 */
	@BitField(id = 10, optional = true)
	val damage: ConsumableDamage?,
) {

	@Suppress("unused")
	private constructor() : this(
			null, 0, false, 0, 0, 0f, ArrayList(0),
			ArrayList(0), false, ArrayList(0), null
	)

	/**
	 * Checks whether this item is classified as positive (beneficial to the consumer).
	 * - When a player selects a positive consumable, the target selection will initially target an ally.
	 * - When a player selects a negative consumable, the target selection will initially target an enemy.
	 */
	fun isPositive() = damage == null && addStatusEffects.all { it.effect.isPositive }

	/**
	 * Attempts to consume this item *outside* combat.
	 * - If this method returns `false`, this item can *not* be consumed by `target`, so it should *stay* in the
	 * inventory.
	 * - If this method returns `true`, this item was consumed, so it should be removed from the inventory.
	 */
	fun consumeOutsideBattle(target: PlayableCharacter, targetState: CharacterState): Boolean {
		val maxHealth = targetState.determineMaxHealth(target.baseStats, targetState.activeStatusEffects)
		val maxMana = targetState.determineMaxMana(target.baseStats, targetState.activeStatusEffects)

		var didSomething = false
		if (this.isFullCure) {
			if (targetState.currentHealth < maxHealth) {
				targetState.currentHealth = maxHealth
				didSomething = true
			}
			if (targetState.currentMana < maxMana) {
				targetState.currentMana = maxMana
				didSomething = true
			}
		}

		if (this.restoreHealth > 0 && targetState.currentHealth < maxHealth) {
			targetState.currentHealth = min(targetState.currentHealth + this.restoreHealth, maxHealth)
			didSomething = true
		}

		if (this.restoreMana > 0 && targetState.currentMana < maxMana) {
			targetState.currentMana = min(targetState.currentMana + this.restoreMana, maxMana)
			didSomething = true
		}

		val rng = Random.Default
		for (addEffect in this.addStatusEffects) {
			if (addEffect.effect.disappearsAfterCombat) continue
			if (targetState.activeStatusEffects.contains(addEffect.effect)) continue
			if (!addEffect.effect.isPositive) continue

			didSomething = true
			if (rng.nextInt(100) < addEffect.chance) {
				targetState.activeStatusEffects.add(addEffect.effect)
			}
		}

		for (removeEffect in this.removeStatusEffects) {
			if (!targetState.activeStatusEffects.contains(removeEffect.effect)) continue

			didSomething = true
			if (rng.nextInt(100) < removeEffect.chance) {
				targetState.activeStatusEffects.remove(removeEffect.effect)
			}
		}

		if (this.removeNegativeStatusEffects && targetState.activeStatusEffects.removeIf { !it.isPositive }) {
			didSomething = true
		}

		return didSomething
	}
}
