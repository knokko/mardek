package mardek.content.battle

import com.github.knokko.bitser.BitEnum
import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.content.BITSER
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

	@BitField(id = 2)
	@Suppress("unused")
	@StableReferenceFieldId
	private val id = UUID.randomUUID()

	@Suppress("unused")
	private constructor() : this(StrategyCriteria.NONE, ArrayList(0))

	override fun toString() = "StrategyPool(if $criteria $entries)"
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

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)
}

@BitEnum(mode = BitEnum.Mode.Ordinal)
enum class StrategyTarget(val raw: String) {
	AnyEnemy("ANY_PC"),
	AllEnemies("ALL_p"),
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
	val myHpPercentageAtMost: Int = 100,

	@BitField(id = 2)
	@IntegerField(expectUniform = false, minValue = 0, maxValue = 100)
	val myHpPercentageAtLeast: Int = 0,

	@BitField(id = 3)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 100)
	val targetHpPercentageAtMost: Int = 100,

	@BitField(id = 4)
	@IntegerField(expectUniform = false, minValue = 0, maxValue = 100)
	val targetHpPercentageAtLeast: Int = 0,

	@BitField(id = 5, optional = true)
	@ReferenceField(stable = false, label = "status effects")
	val targetHasEffect: StatusEffect? = null,

	@BitField(id = 6, optional = true)
	@ReferenceField(stable = false, label = "status effects")
	val targetMissesEffect: StatusEffect? = null,

	@BitField(id = 7, optional = true)
	val resistanceAtMost: ElementalResistance? = null,

	@BitField(id = 8, optional = true)
	val resistanceAtLeast: ElementalResistance? = null,

	@BitField(id = 9, optional = true)
	@ReferenceField(stable = false, label = "elements")
	val myElement: Element? = null,

	@BitField(id = 10)
	@IntegerField(expectUniform = false, minValue = 0)
	val freeAllySlots: Int = 0,

	@BitField(id = 11)
	val targetFainted: Boolean = false,

	@BitField(id = 12)
	val canUseOnOddTurns: Boolean = true,

	@BitField(id = 13)
	val canUseOnEvenTurns: Boolean = true,

	@BitField(id = 14)
	val canRepeat: Boolean = true,
) {

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)

	override fun toString(): String {
		val builder = StringBuilder("Criteria(")
		if (maxUses != NONE.maxUses) builder.append("maxUses == $maxUses,")
		if (myHpPercentageAtMost != NONE.myHpPercentageAtMost) builder.append("my hp% <= $myHpPercentageAtMost,")
		if (myHpPercentageAtLeast != NONE.myHpPercentageAtLeast) builder.append("my hp% >= $myHpPercentageAtLeast,")
		if (targetHpPercentageAtMost != NONE.targetHpPercentageAtMost) builder.append("target hp% <= $targetHpPercentageAtMost,")
		if (targetHpPercentageAtLeast != NONE.targetHpPercentageAtLeast) builder.append("target hp% >= $targetHpPercentageAtLeast,")
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
		myHpPercentageAtMost = merge(NONE.myHpPercentageAtMost, this.myHpPercentageAtMost, other.myHpPercentageAtMost),
		myHpPercentageAtLeast = merge(NONE.myHpPercentageAtLeast, this.myHpPercentageAtLeast, other.myHpPercentageAtLeast),
		targetHpPercentageAtMost = merge(NONE.targetHpPercentageAtMost, this.targetHpPercentageAtMost, other.targetHpPercentageAtMost),
		targetHpPercentageAtLeast = merge(NONE.targetHpPercentageAtLeast, this.targetHpPercentageAtLeast, other.targetHpPercentageAtLeast),
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
