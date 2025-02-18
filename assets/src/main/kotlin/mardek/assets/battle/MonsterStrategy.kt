package mardek.assets.battle

import com.github.knokko.bitser.BitEnum
import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.assets.skill.ActiveSkill

@BitStruct(backwardCompatible = false)
class StrategyPool(
	@BitField(ordering = 0)
	val criteria: StrategyCriteria,

	@BitField(ordering = 1)
	val entries: ArrayList<StrategyEntry>,
) {

	@Suppress("unused")
	private constructor() : this(StrategyCriteria.NONE, ArrayList(0))
}

@BitStruct(backwardCompatible = false)
class StrategyEntry(
	@BitField(ordering = 0, optional = true)
	@ReferenceField(stable = false, label = "skills")
	val skill: ActiveSkill?,

	@BitField(ordering = 1)
	val target: StrategyTarget,

	@BitField(ordering = 2)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 100)
	val chance: Int,
) {
	@Suppress("unused")
	private constructor() : this(null, StrategyTarget.Self, 0)

	override fun toString() = "$chance% ${skill?.name ?: "Attack"}"
}

@BitEnum(mode = BitEnum.Mode.VariableIntOrdinal)
enum class StrategyTarget(val raw: String) {
	AnyPlayer("ANY_PC"),
	AllPlayers("ALL_p"),
	Self("SELF")
}

@BitStruct(backwardCompatible = false)
class StrategyCriteria(
	@BitField(ordering = 0, optional = true)
	@IntegerField(expectUniform = false, minValue = 1)
	val maxUses: Int? = null,

	@BitField(ordering = 1)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 100)
	val hpPercentageAtMost: Int = 100,

	@BitField(ordering = 2)
	@IntegerField(expectUniform = false, minValue = 0, maxValue = 100)
	val hpPercentageAtLeast: Int = 0,
) {

	// TODO Maybe let Bitser handle this
	override fun equals(other: Any?) = other is StrategyCriteria && this.maxUses == other.maxUses &&
			this.hpPercentageAtMost == other.hpPercentageAtMost &&
			this.hpPercentageAtLeast == other.hpPercentageAtLeast

	override fun hashCode() = (maxUses ?: 0) + 5 * hpPercentageAtMost - 13 * hpPercentageAtLeast

	companion object {
		val NONE = StrategyCriteria()
	}
}
