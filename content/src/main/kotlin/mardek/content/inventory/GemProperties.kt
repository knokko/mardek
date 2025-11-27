package mardek.content.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.IntegerField
import mardek.content.stats.PossibleStatusEffect

/**
 * The item properties that only gemstones have. They basically determine the power & element of Legions gemsplosion
 * skill.
 */
@BitStruct(backwardCompatible = true)
class GemProperties(

	/**
	 * The power of the gemstone. The gemsplosion damage will be proportional to `power - MDEF`.
	 */
	@BitField(id = 0)
	@IntegerField(expectUniform = false, minValue = 1)
	val power: Int,

	/**
	 * The gemsplosion particle effect
	 */
	@BitField(id = 1)
	val particleEffect: String, // TODO CHAP3 Turn into reference

	/**
	 * The status effects that the target may get after getting hit by gemsplosion
	 */
	@BitField(id = 2)
	val inflictStatusEffects: ArrayList<PossibleStatusEffect>,

	/**
	 * How much HP gemsplosion will drain from the target (used by Blood Opal). The HP of the attacker will be increased
	 * by `drainHp * damageDealt`.
	 */
	@BitField(id = 3)
	@FloatField(expectMultipleOf = 0.25)
	val drainHp: Float,
) {

	@Suppress("unused")
	private constructor() : this(0, "", ArrayList(0), 0f)

	override fun toString() = "Gem($power, $drainHp)"
}
