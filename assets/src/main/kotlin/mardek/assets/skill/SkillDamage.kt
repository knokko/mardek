package mardek.assets.skill

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.IntegerField

@BitStruct(backwardCompatible = false)
class SkillDamage(
	/**
	 * Increases attack value by flat amount, independent of weapon
	 */
	@BitField(ordering = 0)
	@IntegerField(expectUniform = false, minValue = 0)
	val flatAttackValue: Int,

	/**
	 * Increases attack value by weapon attack value * modifier
	 */
	@BitField(ordering = 1)
	@FloatField(expectMultipleOf = 0.1)
	val weaponModifier: Float,

	/**
	 * Increases attack value by attacker level * modifier
	 */
	@BitField(ordering = 2)
	@IntegerField(expectUniform = false, minValue = 0)
	val levelModifier: Int,

	/**
	 * Whether damage should be divided by 2 when targeting multiple characters
	 */
	@BitField(ordering = 3)
	val splitDamage: Boolean,

	@BitField(ordering = 4)
	val ignoresDefense: Boolean,

	@BitField(ordering = 5)
	val ignoresShield: Boolean,

	/**
	 * Lay on hands, divine glory, and spirit blade are... different
	 */
	@BitField(ordering = 6)
	val spiritModifier: SkillSpiritModifier,

	/**
	 * Bonus damage modifier against particular elements (like water slash)
	 */
	@BitField(ordering = 7)
	val bonusAgainstElements: ArrayList<ElementalDamageBonus>,

	/**
	 * Add damage based on money (money attack)
	 */
	@BitField(ordering = 8)
	val moneyModifier: Float,

	/**
	 * Attack bonus and element based on equipped gem (gemplosion)
	 */
	@BitField(ordering = 9)
	val gemModifier: Float,

	/**
	 * Adds `(hp lost by attacker) * modifier` damage (revenge strike)
	 */
	@BitField(ordering = 10)
	val lostHealthModifier: Float,

	/**
	 * Status effect count modifier (coup de grace), I don't know details yet
	 */
	@BitField(ordering = 11)
	val statusEffectModifier: Float,

	/**
	 * Kill count modifier (sin strike), I don't know details yet
	 */
	@BitField(ordering = 12)
	val killCountModifier: Float,

	/**
	 * Hardcoded extra damage (1000 needles)
	 */
	@BitField(ordering = 13)
	@IntegerField(expectUniform = false, minValue = 0)
	val hardcodedDamage: Int,

	/**
	 * Adds `(current target HP) * modifier` extra damage (spirit nova)
	 */
	@BitField(ordering = 14)
	@FloatField(expectMultipleOf = 0.1)
	val remainingTargetHpModifier: Float,

	/**
	 * 'Damage' is a fraction of the 'damage' of the consumed potion (potion spray)
	 */
	@BitField(ordering = 15)
	@FloatField(expectMultipleOf = 0.25)
	val potionModifier: Float,

	/**
	 * Crescendo strike modifier, Zachs Crescendo strike has a modifier of 0.25
	 */
	@BitField(ordering = 16)
	@FloatField(expectMultipleOf = 0.05)
	val crescendoModifier: Float,

	/**
	 * Use weapon crit chance when this is null
	 */
	@BitField(ordering = 17, optional = true)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 100)
	val critChance: Int? = null,
) {
}
