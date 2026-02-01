package mardek.content.story

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.BITSER

/**
 * An expression that, given the `TimelineState`, evaluates to a `TimelineValue`. The simplest subclass is
 * `ConstantTimelineExpression`, which always evaluates to the same value, regardless of the state.
 */
sealed class TimelineExpression<T> {

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)

	companion object {

		@Suppress("unused")
		private val BITSER_HIERARCHY = arrayOf(
			ConstantTimelineExpression::class.java,
			GlobalTimelineExpression::class.java,
			VariableTimelineExpression::class.java,

			ExpressionOrDefaultTimelineExpression::class.java,
			SwitchCaseTimelineExpression::class.java,

			NegateTimelineCondition::class.java,
			AndTimelineCondition::class.java,
			DefinedVariableTimelineCondition::class.java,
		)
	}
}

/**
 * The simplest subclass of `TimelineExpression`: instances of this class will always evaluate to the same value:
 * their `fixedValue`.
 */
@BitStruct(backwardCompatible = true)
class ConstantTimelineExpression<T>(

	/**
	 * The `TimelineValue` to which this expression will always evaluate.
	 */
	@BitField(id = 0)
	@ClassField(root = TimelineValue::class)
	val fixedValue: TimelineValue<T>
) : TimelineExpression<T>() {

	@Suppress("UNCHECKED_CAST")
	internal constructor() : this(TimelineBooleanValue(false) as TimelineValue<T>)

	override fun toString() = fixedValue.toString()
}

/**
 * A subclass of `TimelineExpression` that propagates to the `expression` of a `GlobalExpression`, which is
 * convenient for 'code' reuse.
 */
@BitStruct(backwardCompatible = true)
class GlobalTimelineExpression<T>(

	/**
	 * The `GlobalExpression` whose expression will be used
	 */
	@BitField(id = 0)
	@ReferenceField(stable = false, label = "global expressions")
	val global: GlobalExpression<T>
) : TimelineExpression<T>() {

	@Suppress("unused")
	private constructor() : this(GlobalExpression())

	override fun toString() = "GlobalTimelineExpression(${global.name})"
}

/**
 * A subclass of `TimelineExpression` that evaluates to the current value of `variable`.
 */
@BitStruct(backwardCompatible = true)
class VariableTimelineExpression<T>(

	/**
	 * This expression will evaluate to the value of this variable
	 */
	@BitField(id = 0)
	@ReferenceField(stable = false, label = "timeline variables")
	val variable: TimelineVariable<T>
) : TimelineExpression<T?>() {

	@Suppress("unused")
	private constructor() : this(FixedTimelineVariable())

	override fun toString() = variable.toString()
}

/**
 * A subclass of `TimelineExpression` that evaluates to whatever `expression` evaluates, unless it evaluates to null.
 * When it evaluates to null, this expression evaluates to `ifNull` instead.
 */
@BitStruct(backwardCompatible = true)
class ExpressionOrDefaultTimelineExpression<T>(

	/**
	 * This timeline expression will evaluate to whatever `this.expression` evaluates, unless it evaluates to null.
	 */
	@BitField(id = 0)
	@ClassField(root = TimelineExpression::class)
	val expression: TimelineExpression<out T?>,

	/**
	 * This timeline expression will evaluate to the value of `ifNull` when `this.expression` evaluates to null
	 */
	@BitField(id = 1)
	@ClassField(root = TimelineExpression::class)
	val ifNull: TimelineExpression<T>,
) : TimelineExpression<T>() {

	@Suppress("unused")
	private constructor() : this(ConstantTimelineExpression(), ConstantTimelineExpression())

	override fun toString() = "(($expression) ?: ($ifNull))"
}

@BitStruct(backwardCompatible = true)
class SwitchCaseTimelineExpression<I, O>(

	/**
	 * The input expression that should be compared with the `inputToMatch` of each `Case`
	 */
	@BitField(id = 0)
	@ClassField(root = TimelineExpression::class)
	val input: TimelineExpression<I>,

	/**
	 * The cases
	 */
	@BitField(id = 1)
	val cases: Array<Case<I, out O>>,

	/**
	 * The output expression when none of the `cases` matches `input`
	 */
	@BitField(id = 2)
	@ClassField(root = TimelineExpression::class)
	val defaultOutput: TimelineExpression<out O>,
) : TimelineExpression<O>() {

	@Suppress("unused")
	private constructor() : this(
		ConstantTimelineExpression<I>(),
		emptyArray<Case<I, out O>>(),
		ConstantTimelineExpression<O>(),
	)

	override fun toString() = "SwitchTimelineExpression(input=$input, cases=$cases, default=$defaultOutput)"

	/**
	 * Represents a single case of a `SwitchCaseTimelineExpression
	 */
	@BitStruct(backwardCompatible = true)
	class Case<I, O>(

		/**
		 * The input value to match. This case is ignored when `input != inputToMatch`.
		 */
		@BitField(id = 0)
		@ClassField(root = TimelineExpression::class)
		val inputToMatch: TimelineExpression<I>,

		/**
		 * The output expression when `input` evaluates to the same value as `inputToMatch`.
		 */
		@BitField(id = 1)
		@ClassField(root = TimelineExpression::class)
		val outputWhenInputMatches: TimelineExpression<O>,
	) {

		@Suppress("unused")
		private constructor() : this(
			ConstantTimelineExpression<I>(),
			ConstantTimelineExpression<O>()
		)

		override fun toString() = "$inputToMatch -> $outputWhenInputMatches"
	}
}

/**
 * A timeline expression that evaluates to `true` if and only if `operand` evaluates to **false**.
 */
@BitStruct(backwardCompatible = true)
class NegateTimelineCondition(

	/**
	 * This expression will evaluate to the negation of `operand`
	 */
	@BitField(id = 0)
	@ClassField(root = TimelineExpression::class)
	val operand: TimelineExpression<Boolean>,
) : TimelineExpression<Boolean>() {

	@Suppress("unused")
	private constructor() : this(ConstantTimelineExpression(TimelineBooleanValue(false)))

	override fun toString() = "!($operand)"
}

/**
 * A timeline expression that evaluates to `true` if and only if all `operands` evaluate to `true`.
 */
@BitStruct(backwardCompatible = true)
class AndTimelineCondition(

	/**
	 * The operands that should all evaluate to `true`
	 */
	@BitField(id = 0)
	@ClassField(root = TimelineExpression::class)
	val operands: Array<TimelineExpression<Boolean>>

) : TimelineExpression<Boolean>() {

	@Suppress("unused")
	private constructor() : this(emptyArray())

	override fun toString() = "(${operands.joinToString(" && ")})"
}

/**
 * A timeline expression that evaluates to `true` if and only if `variable` currently has a value (in other words,
 * at least 1 active `TimelineAssignment` assigns `variable`).
 */
@BitStruct(backwardCompatible = true)
class DefinedVariableTimelineCondition(

	/**
	 * This expression evaluates to `true` if and only if this variable evaluates to something non-null.
	 */
	@BitField(id = 0)
	@ReferenceField(stable = false, label = "timeline variables")
	val variable: TimelineVariable<*>,
) : TimelineExpression<Boolean>() {

	@Suppress("unused")
	private constructor() : this(FixedTimelineVariable<Unit>())

	override fun toString() = "($variable is defined)"
}
