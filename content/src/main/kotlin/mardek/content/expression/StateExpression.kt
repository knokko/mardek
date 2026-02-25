package mardek.content.expression

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.BITSER
import mardek.content.inventory.Item
import mardek.content.story.FixedTimelineVariable
import mardek.content.story.TimelineVariable

/**
 * An expression that, given the `CampaignState`, evaluates to a [ExpressionValue]. The simplest subclass is
 * [ConstantStateExpression], which always evaluates to the same value, regardless of the state.
 */
sealed class StateExpression<T> {

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)

	companion object {

		@Suppress("unused")
		private val BITSER_HIERARCHY = arrayOf(
			ConstantStateExpression::class.java,
			GlobalStateExpression::class.java,
			VariableStateExpression::class.java,

			ExpressionOrDefaultStateExpression::class.java,
			SwitchCaseStateExpression::class.java,
			IfElseStateExpression::class.java,

			NegateStateCondition::class.java,
			AndStateCondition::class.java,
			DefinedVariableStateCondition::class.java,

			ItemCountStateCondition::class.java,
		)
	}
}

/**
 * The simplest subclass of [StateExpression]: instances of this class will always evaluate to the same value:
 * their `fixedValue`.
 */
@BitStruct(backwardCompatible = true)
class ConstantStateExpression<T>(

	/**
	 * The [ExpressionValue] to which this expression will always evaluate.
	 */
	@BitField(id = 0)
	@ClassField(root = ExpressionValue::class)
	val fixedValue: ExpressionValue<T>
) : StateExpression<T>() {

	@Suppress("UNCHECKED_CAST")
	internal constructor() : this(ExpressionBooleanValue(false) as ExpressionValue<T>)

	override fun toString() = fixedValue.toString()
}

/**
 * A subclass of [StateExpression] that propagates to the `expression` of a [GlobalExpression], which is
 * convenient for 'code' reuse.
 */
@BitStruct(backwardCompatible = true)
class GlobalStateExpression<T>(

	/**
	 * The [GlobalExpression] whose expression will be used
	 */
	@BitField(id = 0)
	@ReferenceField(stable = false, label = "global expressions")
	val global: GlobalExpression<T>
) : StateExpression<T>() {

	@Suppress("unused")
	private constructor() : this(GlobalExpression())

	override fun toString() = "GlobalTimelineExpression(${global.name})"
}

/**
 * A subclass of [StateExpression] that evaluates to the current value of [variable]
 */
@BitStruct(backwardCompatible = true)
class VariableStateExpression<T>(

	/**
	 * This expression will evaluate to the value of this variable
	 */
	@BitField(id = 0)
	@ReferenceField(stable = false, label = "timeline variables")
	val variable: TimelineVariable<T>
) : StateExpression<T?>() {

	@Suppress("unused")
	private constructor() : this(FixedTimelineVariable())

	override fun toString() = variable.toString()
}

/**
 * A subclass of [StateExpression] that evaluates to whatever `expression` evaluates, unless it evaluates to null.
 * When it evaluates to null, this expression evaluates to `ifNull` instead.
 */
@BitStruct(backwardCompatible = true)
class ExpressionOrDefaultStateExpression<T>(

	/**
	 * This state expression will evaluate to whatever [expression] evaluates, unless it evaluates to `null`.
	 */
	@BitField(id = 0)
	@ClassField(root = StateExpression::class)
	val expression: StateExpression<out T?>,

	/**
	 * This state expression will evaluate to the value of [ifNull] when [expression] evaluates to `null`
	 */
	@BitField(id = 1)
	@ClassField(root = StateExpression::class)
	val ifNull: StateExpression<T>,
) : StateExpression<T>() {

	@Suppress("unused")
	private constructor() : this(ConstantStateExpression(), ConstantStateExpression())

	override fun toString() = "(($expression) ?: ($ifNull))"
}

/**
 * A subclass of [StateExpression] that acts like a switch-case statement:
 * 1. It evaluates [input]
 * 2. When this evaluation matches the `inputToMatch` of one of these [cases],
 *    this expression evaluates to the `outputWhenInputMatches` of that case.
 *    (If multiple cases match, the first one is used.)
 * 3. When none of the cases matches, this expression evaluates to [defaultOutput]
 */
@BitStruct(backwardCompatible = true)
class SwitchCaseStateExpression<I, O>(

	/**
	 * The input expression that should be compared with the `inputToMatch` of each [Case]
	 */
	@BitField(id = 0)
	@ClassField(root = StateExpression::class)
	val input: StateExpression<I>,

	/**
	 * The cases
	 */
	@BitField(id = 1)
	val cases: Array<Case<I, out O>>,

	/**
	 * The output expression when none of the [cases] matches [input]
	 */
	@BitField(id = 2)
	@ClassField(root = StateExpression::class)
	val defaultOutput: StateExpression<out O>,
) : StateExpression<O>() {

	@Suppress("unused")
	private constructor() : this(
		ConstantStateExpression<I>(),
		emptyArray<Case<I, out O>>(),
		ConstantStateExpression<O>(),
	)

	override fun toString() = "SwitchTimelineExpression(input=$input, cases=$cases, default=$defaultOutput)"

	/**
	 * Represents a single case of a [SwitchCaseStateExpression]
	 */
	@BitStruct(backwardCompatible = true)
	class Case<I, O>(

		/**
		 * The input value to match. This case is taken when the `input` evaluates to the same value as [inputToMatch]
		 */
		@BitField(id = 0)
		@ClassField(root = StateExpression::class)
		val inputToMatch: StateExpression<I>,

		/**
		 * The output expression when the `input` evaluates to the same value as [inputToMatch]
		 */
		@BitField(id = 1)
		@ClassField(root = StateExpression::class)
		val outputWhenInputMatches: StateExpression<O>,
	) {

		@Suppress("unused")
		private constructor() : this(
			ConstantStateExpression<I>(),
			ConstantStateExpression<O>()
		)

		override fun toString() = "$inputToMatch -> $outputWhenInputMatches"
	}
}

/**
 * A state expression that evaluates to [ifTrue] if [condition] evaluates to true, and evaluates to [ifFalse]
 * otherwise.
 */
@BitStruct(backwardCompatible = true)
class IfElseStateExpression<T>(

	/**
	 * The condition that determines whether this expression evaluates to [ifTrue], or to [ifFalse]
	 */
	@BitField(id = 0)
	@ClassField(root = StateExpression::class)
	val condition: StateExpression<Boolean>,

	/**
	 * The expression to which this expression evaluates if [condition] evaluates to `true`
	 */
	@BitField(id = 1)
	@ClassField(root = StateExpression::class)
	val ifTrue: StateExpression<T>,

	/**
	 * The expression to which this expression evaluates if [condition] evaluates to `false`
	 */
	@BitField(id = 2)
	@ClassField(root = StateExpression::class)
	val ifFalse: StateExpression<T>,
) : StateExpression<T>() {

	@Suppress("unused", "UNCHECKED_CAST")
	private constructor() : this(
		ConstantStateExpression(ExpressionBooleanValue(false)),
		ConstantStateExpression(ExpressionUnitValue()) as StateExpression<T>,
		ConstantStateExpression(ExpressionUnitValue()) as StateExpression<T>,
	)

	override fun toString() = "if($condition) { $ifTrue } else { $ifFalse }"
}


/**
 * A state expression that evaluates to `true` if and only if [operand] evaluates to **false**.
 */
@BitStruct(backwardCompatible = true)
class NegateStateCondition(

	/**
	 * This expression will evaluate to the negation of [operand]
	 */
	@BitField(id = 0)
	@ClassField(root = StateExpression::class)
	val operand: StateExpression<Boolean>,
) : StateExpression<Boolean>() {

	@Suppress("unused")
	private constructor() : this(ConstantStateExpression(ExpressionBooleanValue(false)))

	override fun toString() = "!($operand)"
}

/**
 * A state expression that evaluates to `true` if and only if all [operands] evaluate to `true`.
 */
@BitStruct(backwardCompatible = true)
class AndStateCondition(

	/**
	 * The operands that should all evaluate to `true`
	 */
	@BitField(id = 0)
	@ClassField(root = StateExpression::class)
	val operands: Array<StateExpression<Boolean>>

) : StateExpression<Boolean>() {

	@Suppress("unused")
	private constructor() : this(emptyArray())

	override fun toString() = "(${operands.joinToString(" && ")})"
}

/**
 * A state expression that evaluates to `true` if and only if [variable] currently has a value (in other words,
 * at least 1 active `TimelineAssignment` assigns [variable]).
 */
@BitStruct(backwardCompatible = true)
class DefinedVariableStateCondition(

	/**
	 * This expression evaluates to `true` if and only if this variable evaluates to something non-null.
	 */
	@BitField(id = 0)
	@ReferenceField(stable = false, label = "timeline variables")
	val variable: TimelineVariable<*>,
) : StateExpression<Boolean>() {

	@Suppress("unused")
	private constructor() : this(FixedTimelineVariable<Unit>())

	override fun toString() = "($variable is defined)"
}

/**
 * A state expression that evaluates to `true` if the party has at least [minAmount] and at most [maxAmount]
 * occurrences of [item] in their inventories.
 *
 * The occurrences of the item in the inventory and equipment each active party member will be summed up.
 */
@BitStruct(backwardCompatible = true)
class ItemCountStateCondition(

	/**
	 * The item to be counted
	 */
	@BitField(id = 0)
	@ReferenceField(stable = false, label = "items")
	val item: Item,

	/**
	 * The minimum number of occurrences/instances of [item] that the party must have in their inventories. If you don't
	 * want to require a minimum amount, you can simply use `minAmount = 0`, since that is always `true`.
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	val minAmount: Int,

	/**
	 * The maximum number of occurrences/instances of [item] that the party can have in their inventories. If you don't
	 * want to require a maximum amount, you can use `maxAmount = null`.
	 */
	@BitField(id = 2, optional = true)
	@IntegerField(expectUniform = false, minValue = 0)
	val maxAmount: Int?,
) : StateExpression<Boolean>() {

	@Suppress("unused")
	private constructor() : this(Item(), 0, 0)

	override fun toString() = "(has $minAmount to $maxAmount occurrences of $item)"
}
