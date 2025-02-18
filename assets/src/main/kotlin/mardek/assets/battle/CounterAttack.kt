package mardek.assets.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.assets.skill.ActiveSkill

@BitStruct(backwardCompatible = false)
class CounterAttack(
	@BitField(ordering = 0)
	@ReferenceField(stable = false, label = "skills")
	val action: ActiveSkill,

	@BitField(ordering = 1)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 100)
	val chance: Int,

	@BitField(ordering = 2)
	val target: StrategyTarget,
) {

	@Suppress("unused")
	private constructor() : this(ActiveSkill(), 0, StrategyTarget.Self)
}
