package mardek.content.expression

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.BITSER
import mardek.content.action.ActionNode
import mardek.content.animation.ColorTransform
import mardek.content.characters.CharacterState
import mardek.content.characters.PlayableCharacter
import mardek.content.story.FixedTimelineVariables
import mardek.content.story.TimelineVariable

/**
 * Represents the value of a [StateExpression] or [TimelineVariable]. All possible values (except `null`) must be a
 * subclass of [ExpressionValue] .
 *
 * Each `ExpressionValue<T>` wraps a value of type `T`.
 */
sealed class ExpressionValue<T> {

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)

	/**
	 * Gets the value that is wrapped by this [ExpressionValue].
	 */
	abstract fun get(): T

	companion object {

		@Suppress("unused")
		private val BITSER_HIERARCHY = arrayOf(
			ExpressionUnitValue::class.java,

			ExpressionBooleanValue::class.java,
			ExpressionIntValue::class.java,
			ExpressionStringValue::class.java,
			ExpressionOptionalStringValue::class.java,

			ExpressionOptionalPlayerValue::class.java,
			ExpressionCharacterStateValue::class.java,
			ExpressionColorTransformValue::class.java,
			ExpressionOptionalColorTransformValue::class.java,
			ExpressionActionNodeValue::class.java,
		)
	}
}

/**
 * An [ExpressionValue] without any content. This is used for 'flags' that are either present, or not.
 */
@BitStruct(backwardCompatible = true)
class ExpressionUnitValue : ExpressionValue<Unit>() {

	override fun get() = Unit

	override fun toString() = "UnitValue"
}

/**
 * An [ExpressionValue] that wraps a boolean.
 */
@BitStruct(backwardCompatible = true)
class ExpressionBooleanValue(

	/**
	 * The wrapped boolean value
	 */
	@BitField(id = 0)
	val value: Boolean
) : ExpressionValue<Boolean>() {

	@Suppress("unused")
	private constructor() : this(false)

	override fun get() = value

	override fun toString() = "BooleanValue($value)"
}

/**
 * An [ExpressionValue] that wraps an integer.
 */
@BitStruct(backwardCompatible = true)
class ExpressionIntValue(

	/**
	 * The wrapped integer value
	 */
	@BitField(id = 0)
	@IntegerField(expectUniform = false)
	val value: Int
) : ExpressionValue<Int>() {

	@Suppress("unused")
	private constructor() : this(0)

	override fun get() = value

	override fun toString() = "IntValue($value)"
}

/**
 * An [ExpressionValue] that wraps a string
 */
@BitStruct(backwardCompatible = true)
class ExpressionStringValue(

	/**
	 * The wrapped string value
	 */
	@BitField(id = 0)
	val value: String
) : ExpressionValue<String>() {

	@Suppress("unused")
	private constructor() : this("")

	override fun get() = value

	override fun toString() = "StringValue($value)"
}

/**
 * An [ExpressionValue] that wraps a nullable string
 */
@BitStruct(backwardCompatible = true)
class ExpressionOptionalStringValue(

	/**
	 * The wrapped string value, or `null`
	 */
	@BitField(id = 0, optional = true)
	val value: String?
) : ExpressionValue<String?>() {

	@Suppress("unused")
	private constructor() : this("")

	override fun get() = value

	override fun toString() = "StringValue?($value)"
}

/**
 * An [ExpressionValue] that wraps a nullable `PlayableCharacter`.
 *
 * Such values are used for [FixedTimelineVariables.forcedPartyMembers]
 */
@BitStruct(backwardCompatible = true)
class ExpressionOptionalPlayerValue(

	/**
	 * The wrapped playable character, or `null`
	 */
	@BitField(id = 0, optional = true)
	@ReferenceField(stable = false, label = "playable characters")
	val value: PlayableCharacter?
) : ExpressionValue<PlayableCharacter?>() {

	@Suppress("unused")
	private constructor() : this(PlayableCharacter())

	override fun get() = value

	override fun toString() = "PlayerValue?($value)"
}

/**
 * An [ExpressionValue] that wraps a `CharacterState`.
 *
 * Such values are used for [PlayableCharacter.stateVariable].
 */
@BitStruct(backwardCompatible = true)
class ExpressionCharacterStateValue(

	/**
	 * The wrapped character state
	 */
	@BitField(id = 0)
	val value: CharacterState,
) : ExpressionValue<CharacterState>() {

	@Suppress("unused")
	private constructor() : this(CharacterState())

	override fun get() = value

	override fun toString() = "CharacterStateValue(...)"
}

/**
 * An [ExpressionValue] that wraps a `ColorTransform`.
 *
 * Such values are used for area ambience.
 */
@BitStruct(backwardCompatible = true)
class ExpressionColorTransformValue(

	/**
	 * The wrapped color transformation
	 */
	@BitField(id = 0)
	val value: ColorTransform
) : ExpressionValue<ColorTransform>() {

	@Suppress("unused")
	private constructor() : this(ColorTransform())

	override fun get() = value

	override fun toString() = "ColorTransformValue($value)"
}

/**
 * An [ExpressionValue] that wraps a nullable [ColorTransform].
 *
 * Such values are sometimes used for area ambiences.
 */
@BitStruct(backwardCompatible = true)
class ExpressionOptionalColorTransformValue(

	/**
	 * The wrapped color transform, or `null`
	 */
	@BitField(id = 0, optional = true)
	val value: ColorTransform?
) : ExpressionValue<ColorTransform?>() {

	@Suppress("unused")
	private constructor() : this(null)

	override fun get() = value

	override fun toString() = "ColorTransformValue?($value)"
}

/**
 * An [ExpressionValue] that wraps a nullable [ActionNode].
 *
 * Such values are used in `ExpressionActionNode`s, which are crucial for complex dialogues that depend on the
 * campaign state.
 */
@BitStruct(backwardCompatible = true)
class ExpressionActionNodeValue(

	/**
	 * The wrapped action node, or `null`
	 */
	@BitField(id = 0, optional = true)
	@ReferenceField(stable = false, label = "action nodes")
	val value: ActionNode?
) : ExpressionValue<ActionNode?>() {

	@Suppress("unused")
	private constructor() : this(null)

	override fun get() = value

	override fun toString() = "ActionNodeValue(...)"
}
