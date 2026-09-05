package mardek.content.stats

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import java.io.Serializable

/**
 * Increases the [stat] of a combatant by a fixed amount ([adder])
 */
@BitStruct(backwardCompatible = true)
class StatModifier(

	/**
	 * The [CombatStat] of the combatant to be increased (or decreased)
	 */
	@BitField(id = 0)
	val stat: CombatStat,

	/**
	 * The amount by which the [stat] of the combatant will be increased (or negative to decrease it)
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = false, digitSize = 2)
	val adder: Int,
) : Serializable {

	override fun toString() = "$stat+$adder"
}
