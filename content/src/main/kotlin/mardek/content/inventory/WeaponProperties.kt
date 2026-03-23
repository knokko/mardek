package mardek.content.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.audio.SoundEffect
import mardek.content.stats.PossibleStatusEffect
import mardek.content.stats.ElementalDamageBonus
import mardek.content.stats.CreatureTypeBonus

/**
 * The item properties that only weapons have
 */
@BitStruct(backwardCompatible = true)
class WeaponProperties(

	/**
	 * The base hit chance (%) for basic attacks when this item is used as weapon. Note that the final hit chance may
	 * also depend on other factors (e.g. blindness or enemy evasion).
	 */
	@BitField(id = 0)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 999)
	val hitChance: Int,

	/**
	 * The base critical hit chance (%) for melee attacks when this item is used as weapon. Note that the final
	 * critical hit chance may also depend on other factors (e.g. reaction skills that increase the critical hit chance)
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 0, maxValue = 100)
	val critChance: Int,

	/**
	 * Upon hitting a **basic attack** with this weapon that deals `X` damage to the target, the attacker will regain
	 * `hpDrain * X` health.
	 *
	 * This is a rather niche weapon property that e.g. the Blood Sword has.
	 */
	@BitField(id = 2)
	@FloatField(expectMultipleOf = 0.1)
	val hpDrain: Float,

	/**
	 * Upon hitting a **basic attack** with this weapon that deals `X` damage to the target, the attacker will regain
	 * `mpDrain * X` **mana**.
	 *
	 * This property is only used by wands (Emela) in vanilla MARDEK.
	 */
	@BitField(id = 3)
	@FloatField(expectMultipleOf = 0.1)
	val mpDrain: Float,

	/**
	 * The damage bonus of this weapon against enemies with a specific creature type.
	 *
	 * In vanilla MARDEK, this is used by e.g. the Silver Sword to deal more damage to undead creatures.
	 */
	@BitField(id = 4)
	val effectiveAgainstCreatureTypes: ArrayList<CreatureTypeBonus>,

	/**
	 * The damage bonus of this weapon against enemies with a specific element.
	 *
	 * In vanilla MARDEK, this is used by e.g. the Flame Tongue to deal even more damage to air creatures.
	 */
	@BitField(id = 5)
	val effectiveAgainstElements: ArrayList<ElementalDamageBonus>,

	/**
	 * When a melee attack using this weapon hits, these status effects may be inflicted on the target. This is used
	 * by e.g. the Cursed Blade to inflict Curse on the target.
	 */
	@BitField(id = 6)
	val addEffects: ArrayList<PossibleStatusEffect>,

	/**
	 * The melee attack hit sound when this weapon is used, or `null` to use the default hit sound
	 */
	@BitField(id = 7, optional = true)
	@ReferenceField(stable = false, label = "sound effects")
	val hitSound: SoundEffect?,
) {

	@Suppress("unused")
	private constructor() : this(
			0, 0, 0f, 0f,
		ArrayList(0),
		ArrayList(0),
		ArrayList(0), null,
	)
}
