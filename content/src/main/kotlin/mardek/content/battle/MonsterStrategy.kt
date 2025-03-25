package mardek.content.battle

import com.github.knokko.bitser.BitEnum
import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.stats.Element
import mardek.content.stats.StatusEffect
import mardek.content.inventory.Item
import mardek.content.skill.ActiveSkill
import mardek.content.stats.ElementalResistance
import java.util.*
import kotlin.collections.ArrayList

@BitStruct(backwardCompatible = true)
class StrategyPool(
	@BitField(id = 0)
	val criteria: StrategyCriteria,

	@BitField(id = 1)
	val entries: ArrayList<StrategyEntry>,
) {

	@Suppress("unused")
	private constructor() : this(StrategyCriteria.NONE, ArrayList(0))
}

@BitStruct(backwardCompatible = true)
class StrategyEntry(
	@BitField(id = 0, optional = true)
	@ReferenceField(stable = false, label = "skills")
	val skill: ActiveSkill?,

	@BitField(id = 1, optional = true)
	@ReferenceField(stable = false, label = "items")
	val item: Item?,

	@BitField(id = 2)
	val target: StrategyTarget,

	@BitField(id = 3)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 100)
	val chance: Int,
) {
	init {
		if (skill != null && item != null) throw IllegalArgumentException("Skill ($skill) or item ($item) must be null")
		if (item != null && item.consumable == null) throw IllegalArgumentException("Item ($item) must be consumable")
	}

	@Suppress("unused")
	private constructor() : this(null, null, StrategyTarget.Self, 0)

	override fun toString() = "$chance% ${skill?.name ?: item?.flashName ?: "Attack"}"

	override fun equals(other: Any?) = other is StrategyEntry && this.skill === other.skill &&
			this.item === other.item && this.target == other.target && this.chance == other.chance

	override fun hashCode() = 13 * Objects.hashCode(skill) - 31 * Objects.hashCode(item) + 97 * target.hashCode() - chance
}

@BitEnum(mode = BitEnum.Mode.Ordinal)
enum class StrategyTarget(val raw: String) {
	AnyPlayer("ANY_PC"),
	AllPlayers("ALL_p"),
	Self("SELF"),
	AnyAlly("ANY_ALLY"),
	AllAllies("ALL_e")
}

@BitStruct(backwardCompatible = true)
class StrategyCriteria(
	@BitField(id = 0, optional = true)
	@IntegerField(expectUniform = false, minValue = 1)
	val maxUses: Int? = null,

	@BitField(id = 1)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 100)
	val hpPercentageAtMost: Int = 100,

	@BitField(id = 2)
	@IntegerField(expectUniform = false, minValue = 0, maxValue = 100)
	val hpPercentageAtLeast: Int = 0,

	@BitField(id = 3, optional = true)
	@ReferenceField(stable = false, label = "status effects")
	val targetHasEffect: StatusEffect? = null,

	@BitField(id = 4, optional = true)
	@ReferenceField(stable = false, label = "status effects")
	val targetMissesEffect: StatusEffect? = null,

	@BitField(id = 5, optional = true)
	val resistanceAtMost: ElementalResistance? = null,

	@BitField(id = 6, optional = true)
	val resistanceAtLeast: ElementalResistance? = null,

	@BitField(id = 7, optional = true)
	@ReferenceField(stable = false, label = "elements")
	val myElement: Element? = null,

	@BitField(id = 8)
	@IntegerField(expectUniform = false, minValue = 0)
	val freeAllySlots: Int = 0,

	@BitField(id = 9)
	val targetFainted: Boolean = false,

	@BitField(id = 10)
	val canUseOnOddTurns: Boolean = true,

	@BitField(id = 11)
	val canUseOnEvenTurns: Boolean = true,

	@BitField(id = 12)
	val canRepeat: Boolean = true,
) {

	// TODO Maybe let Bitser handle this
	override fun equals(other: Any?) = other is StrategyCriteria && this.maxUses == other.maxUses &&
			this.hpPercentageAtMost == other.hpPercentageAtMost &&
			this.hpPercentageAtLeast == other.hpPercentageAtLeast &&
			this.targetHasEffect === other.targetHasEffect &&
			this.targetMissesEffect === other.targetMissesEffect &&
			this.resistanceAtMost == other.resistanceAtMost &&
			this.resistanceAtLeast == other.resistanceAtLeast &&
			this.canUseOnOddTurns == other.canUseOnOddTurns &&
			this.canUseOnEvenTurns == other.canUseOnEvenTurns &&
			this.canRepeat == other.canRepeat &&
			this.myElement === other.myElement &&
			this.freeAllySlots == other.freeAllySlots &&
			this.targetFainted == other.targetFainted

	override fun hashCode() = (maxUses ?: 0) + 5 * hpPercentageAtMost - 13 * hpPercentageAtLeast +
			31 * Objects.hashCode(targetHasEffect) - 251 * Objects.hashCode(targetMissesEffect) +
			37 * Objects.hashCode(resistanceAtMost) + 41 * Objects.hashCode(resistanceAtLeast) -
			47 * canUseOnOddTurns.hashCode() + 93 * canUseOnEvenTurns.hashCode() - 113 * canRepeat.hashCode() +
			147 * Objects.hashCode(myElement) - 151 * freeAllySlots + 179 * targetFainted.hashCode()

	override fun toString(): String {
		val builder = StringBuilder("Criteria(")
		if (maxUses != NONE.maxUses) builder.append("maxUses == $maxUses,")
		if (hpPercentageAtMost != NONE.hpPercentageAtMost) builder.append("hp% <= $hpPercentageAtMost,")
		if (hpPercentageAtLeast != NONE.hpPercentageAtLeast) builder.append("hp% >= $hpPercentageAtLeast,")
		if (targetHasEffect != null) builder.append("target has ${targetHasEffect.niceName},")
		if (targetMissesEffect != null) builder.append("target misses ${targetMissesEffect.niceName},")
		if (resistanceAtMost != null) builder.append("target ${resistanceAtMost.element} resistance <= ${resistanceAtMost.modifier},")
		if (resistanceAtLeast != null) builder.append("target ${resistanceAtLeast.element} resistance >= ${resistanceAtLeast.modifier}")
		if (canUseOnOddTurns != NONE.canUseOnOddTurns) builder.append("oddTurn == $canUseOnOddTurns,")
		if (canUseOnEvenTurns != NONE.canUseOnEvenTurns) builder.append("evenTurn == $canUseOnEvenTurns,")
		if (canRepeat != NONE.canRepeat) builder.append("canRepeat == $canRepeat,")
		if (myElement != NONE.myElement) builder.append("myElement == $myElement,")
		if (freeAllySlots != NONE.freeAllySlots) builder.append("#freeAllySlots >= $freeAllySlots,")
		if (targetFainted != NONE.targetFainted) builder.append("target fainted,")
		return if (builder.endsWith(",")) "${builder.substring(0 until builder.lastIndex)})"
		else "NONE"
	}

	private fun <T> merge(defaultValue: T, myValue: T, otherValue: T): T {
		if (myValue == defaultValue) return otherValue
		if (otherValue == defaultValue) return myValue
		throw IllegalArgumentException("Merge conflict: default=$defaultValue, my=$myValue, other=$otherValue")
	}

	fun merge(other: StrategyCriteria) = StrategyCriteria(
		maxUses = merge(NONE.maxUses, this.maxUses, other.maxUses),
		hpPercentageAtMost = merge(NONE.hpPercentageAtMost, this.hpPercentageAtMost, other.hpPercentageAtMost),
		hpPercentageAtLeast = merge(NONE.hpPercentageAtLeast, this.hpPercentageAtLeast, other.hpPercentageAtLeast),
		targetHasEffect = merge(NONE.targetHasEffect, this.targetHasEffect, other.targetHasEffect),
		targetMissesEffect = merge(NONE.targetMissesEffect, this.targetMissesEffect, other.targetMissesEffect),
		resistanceAtMost = merge(NONE.resistanceAtMost, this.resistanceAtMost, other.resistanceAtMost),
		resistanceAtLeast = merge(NONE.resistanceAtLeast, this.resistanceAtLeast, other.resistanceAtLeast),
		canUseOnOddTurns = merge(NONE.canUseOnOddTurns, this.canUseOnOddTurns, other.canUseOnOddTurns),
		canUseOnEvenTurns = merge(NONE.canUseOnEvenTurns, this.canUseOnEvenTurns, other.canUseOnEvenTurns),
		canRepeat = merge(NONE.canRepeat, this.canRepeat, other.canRepeat),
		myElement = merge(NONE.myElement, this.myElement, other.myElement),
		freeAllySlots = merge(NONE.freeAllySlots, this.freeAllySlots, other.freeAllySlots),
		targetFainted = merge(NONE.targetFainted, this.targetFainted, other.targetFainted)
	)

	companion object {
		val NONE = StrategyCriteria()
	}
}
