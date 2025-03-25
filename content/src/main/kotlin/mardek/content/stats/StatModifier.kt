package mardek.content.stats

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField

@BitStruct(backwardCompatible = true)
class StatModifier(
	@BitField(id = 0)
	val stat: CombatStat,

	@BitField(id = 1)
	@IntegerField(expectUniform = false)
	val adder: Int,
) {

	@Suppress("unused")
	private constructor() : this(CombatStat.Attack, 0)

	override fun toString() = "$stat+$adder"
}
