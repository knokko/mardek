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

/**
 * Represents a list of `StrategyEntry`s (typically basic attacks or active skills) with the same criteria.
 *
 * See the doc comments of `Monster.strategies` for more information.
 */
@BitStruct(backwardCompatible = true)
class StrategyPool(

	/**
	 * The criteria of this pool. Monsters usually select the first strategy pool whose criteria are satisfied.
	 */
	@BitField(id = 0)
	val criteria: StrategyCriteria,

	/**
	 * The entries (typically basic attacks or skills) that can be chosen when this strategy pool is selected.
	 */
	@BitField(id = 1)
	val entries: ArrayList<StrategyEntry>,

	/**
	 * The unique ID of this strategy pool, which is used for (de)serialization.
	 */
	@BitField(id = 2)
	@Suppress("unused")
	@StableReferenceFieldId
	val id: UUID,
) {

	@Suppress("unused")
	private constructor() : this(
		StrategyCriteria.NONE,
		ArrayList(0),
		UUID.randomUUID(),
	)

	override fun toString() = "StrategyPool(if $criteria $entries)"
}

/**
 * Represents an entry of a `StrategyPool`. Each `StrategyEntry` represents a move (e.g. attack) that a monster could
 * do. When a strategy pool is selected, one of its entries will be chosen.
 */
@BitStruct(backwardCompatible = true)
class StrategyEntry(

	/**
	 * When non-null, the monster will cast/use this skill if this `StrategyEntry` is chosen.
	 *
	 * When `skill` is non-null, `item` must be null. When both `skill` and `item` are null, this entry represents a
	 * basic attack.
	 */
	@BitField(id = 0, optional = true)
	@ReferenceField(stable = false, label = "skills")
	val skill: ActiveSkill?,

	/**
	 * When non-null, the monster will use this (consumable) item if this `StrategyEntry` is chosen.
	 *
	 * When `item` is non-null, `skill` must be null. When both `skill` and `item` are null, this entry represents a
	 * 	 * basic attack.
	 */
	@BitField(id = 1, optional = true)
	@ReferenceField(stable = false, label = "items")
	val item: Item?,

	/**
	 * The type of target that the monster will choose.
	 */
	@BitField(id = 2)
	val target: StrategyTarget,

	/**
	 * The chance (in percentage) of this entry. See the doc comments of `Monster.strategies` for the details of the
	 * odds that this entry is chosen.
	 */
	@BitField(id = 3)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 100, commonValues = [100, 30])
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

/**
 * The target selection behavior for a `StrategyEntry`. Note that this is from the perspective of the monster, so
 * e.g. `AnyEnemy` means an enemy of the monster, which is normally a player.
 */
@BitEnum(mode = BitEnum.Mode.Ordinal)
enum class StrategyTarget(val raw: String) {

	/**
	 * A single enemy of the monster will be targeted
	 */
	AnyEnemy("ANY_PC"),

	/**
	 * All enemies of the monster will be targeted. This can only be used for multi-target skills.
	 */
	AllEnemies("ALL_p"),

	/**
	 * The monster will target itself
	 */
	Self("SELF"),

	/**
	 * A single ally of the monster will be targeted, possibly the monster itself
	 */
	AnyAlly("ANY_ALLY"),

	/**
	 * All allies of the monster will be targeted, including the monster itself
	 */
	AllAllies("ALL_e")
}

/**
 * The possible criteria of a `StrategyPool`. Strategy pools can only be selected if all their criteria are satisfied.
 */
@BitStruct(backwardCompatible = true)
class StrategyCriteria(

	/**
	 * The maximum number of times the monster can select this strategy pool (per combatant, per battle). When null,
	 * there is no maximum.
	 */
	@BitField(id = 0, optional = true)
	@IntegerField(expectUniform = false, minValue = 1)
	val maxUses: Int? = null,

	/**
	 * The strategy pool can only be selected if the monster has at most this percentage of its maximum HP.
	 *
	 * This is typically used in combination with `maxUses`, to cast a certain skill once when the monster
	 * HP drops below a certain threshold.
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 100)
	val myHpPercentageAtMost: Int = 100,

	/**
	 * The strategy pool can only be selected if the monster has at least this percentage of its maximum HP.
	 *
	 * When a strategy pool of a monster has a criteria with e.g. `myHpPercentageAtLeast == 70`, that monster
	 * typically has another pool with a strategy criteria with `myHpPercentageAtMost == 69`. This is not required
	 * though. TODO CHAP2 Check for potential off-by-one errors where myHpPercentageAtMost == myHpPercentageAtLeast
	 */
	@BitField(id = 2)
	@IntegerField(expectUniform = false, minValue = 0, maxValue = 100)
	val myHpPercentageAtLeast: Int = 0,

	/**
	 * The strategy pool can only be selected if at least one potential target has at most this percentage of its
	 * maximum HP. Furthermore, if the strategy pool is selected and the target type is single-target, only targets
	 * with at most this percentage of their maximum HP can be selected.
	 */
	@BitField(id = 3)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 100)
	val targetHpPercentageAtMost: Int = 100,

	/**
	 * The strategy pool can only be selected if at least one potential target has at least this percentage of its
	 * maximum HP. Furthermore, if the strategy pool is selected and the target type is single-target, only targets
	 * with at least this percentage of their maximum HP can be selected.
	 */
	@BitField(id = 4)
	@IntegerField(expectUniform = false, minValue = 0, maxValue = 100)
	val targetHpPercentageAtLeast: Int = 0,

	/**
	 * When non-null, the strategy pool can only be selected if at least one possible target has this status effect.
	 * Furthermore, if the strategy pool is selected and the target type is single-target, only targets that have this
	 * status effect can be selected.
	 */
	@BitField(id = 5, optional = true)
	@ReferenceField(stable = false, label = "status effects")
	val targetHasEffect: StatusEffect? = null,

	/**
	 * When non-null, the strategy pool can only be selected if at least one possible target does *not* have this
	 * status effect. Furthermore, if the strategy pool is selected and the target type is single-target, only targets
	 * that do *not* have this status effect can be selected.
	 */
	@BitField(id = 6, optional = true)
	@ReferenceField(stable = false, label = "status effects")
	val targetMissesEffect: StatusEffect? = null,

	/**
	 * When non-null, the strategy pool can only be selected if at least one possible target has an elemental resistance
	 * against `resistanceAtMost.element` of at most `resistanceAtMost.modifier`. Furthermore, if the strategy pool is
	 * selected and the target type is single-target, only targets with an elemental resistance against
	 * `resistanceAtMost.element` of at most `resistanceAtMost.modifier` can be selected.
	 */
	@BitField(id = 7, optional = true)
	val resistanceAtMost: ElementalResistance? = null,

	/**
	 * When non-null, the strategy pool can only be selected if at least one possible target has an elemental resistance
	 * against `resistanceAtMost.element` of at least `resistanceAtMost.modifier`. Furthermore, if the strategy pool is
	 * selected and the target type is single-target, only targets with an elemental resistance against
	 * `resistanceAtMost.element` of at least `resistanceAtMost.modifier` can be selected.
	 */
	@BitField(id = 8, optional = true)
	val resistanceAtLeast: ElementalResistance? = null,

	/**
	 * When non-null, the strategy pool can only be selected if the (current) element of the monster is `myElement`.
	 * This is only useful on monsters like Master Stone and Karnos that can change their own element.
	 */
	@BitField(id = 9, optional = true)
	@ReferenceField(stable = false, label = "elements")
	val myElement: Element? = null,

	/**
	 * When non-zero, the strategy pool can only be selected if there are at least `freeAllySlots` 'slots' available
	 * on the monsters team where it can spawn monsters. For instance, when `freeAllySlots == 1`, the strategy pool
	 * can only be selected if there are exactly 3 combatants on the team of the monster, since the maximum number of
	 * combatants per team is 4.
	 */
	@BitField(id = 10)
	@IntegerField(expectUniform = false, minValue = 0)
	val freeAllySlots: Int = 0,

	/**
	 * When true, the strategy pool can only be selected if at least one possible target is down/fainted/has 0 HP.
	 * Furthermore, when the strategy pool is selected, only fainted targets can be selected.
	 *
	 * This is used for Morics zombify skill.
	 */
	@BitField(id = 11)
	val targetFainted: Boolean = false,

	/**
	 * When **false**, the strategy pool can **not** be used on 'odd' turns: only on 'even' turns. For every monster,
	 * the `totalSpentTurns` is tracked. This counter is initially 0, and increased whenever the monster gets on turn
	 * (even when the turn is lost because the monster is e.g. asleep). When `canUseOnOddTurns` is false, the strategy
	 * pool can only be selected when `totalSpentTurns` is an even integer.
	 *
	 * This can be used to prevent a monster from casting two skills right after each other by giving both of them
	 * `canUseOnOddTurns = false`.
	 */
	@BitField(id = 12)
	val canUseOnOddTurns: Boolean = true,

	/**
	 * This is the opposite of `canUseOnOddTurns`: when `canUseOnEvenTurns` is false, the strategy pool can only be
	 * selected when `totalSpentTurns` is an odd integer.
	 */
	@BitField(id = 13)
	val canUseOnEvenTurns: Boolean = true,

	/**
	 * When false, the monster cannot select the strategy pool two turns in a row. This is sometimes used on very
	 * powerful moves.
	 */
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

	/**
	 * Merges/combines this strategy criteria with another strategy criteria, and returns the result.
	 *
	 * For instance, `StrategyCriteria(maxUses=5).merge(StrategyCriteria(myHpPercentageAtLeast=50))` would result in
	 * `StrategyCriteria(maxUses=5, myHpPercentageAtLeast=50)`.
	 */
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

		/**
		 * An instance of `StrategyCriteria` that does not have any criteria: `StrategyPool`s with this
		 * `criteria = StrategyCriteria.NONE` can always be selected.
		 */
		val NONE = StrategyCriteria()
	}
}
