package mardek.assets.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.assets.skill.ActiveSkill
import java.util.*

@BitStruct(backwardCompatible = true)
class CounterAttack(
	@BitField(id = 0)
	@ReferenceField(stable = false, label = "skills")
	val action: ActiveSkill,

	@BitField(id = 1)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 100)
	val chance: Int,

	@BitField(id = 2)
	val target: StrategyTarget,
) {

	@Suppress("unused")
	private constructor() : this(ActiveSkill(), 0, StrategyTarget.Self)

	override fun toString() = "Counter($chance% $action at $target)"

	override fun equals(other: Any?) = other is CounterAttack && this.action === other.action &&
			this.chance == other.chance && this.target == other.target

	override fun hashCode() = Objects.hashCode(action) + 127 * chance - 927 * target.hashCode()
}
