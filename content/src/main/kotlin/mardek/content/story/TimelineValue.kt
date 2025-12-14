package mardek.content.story

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.BITSER
import mardek.content.animation.ColorTransform
import mardek.content.characters.CharacterState
import mardek.content.characters.PlayableCharacter

/**
 * Represents a value of a `TimelineVariable`. All possible values for `TimelineVariable`s (except `null`) must be a
 * subclass of `TimelineValue`.
 *
 * Each `TimelineValue<T>` wraps a value of type `T`.
 */
sealed class TimelineValue<T> {

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)

	/**
	 * Gets the value that is wrapped by this `TimelineValue`.
	 */
	abstract fun get(): T

	companion object {

		@Suppress("unused")
		private val BITSER_HIERARCHY = arrayOf(
			TimelineUnitValue::class.java,

			TimelineBooleanValue::class.java,
			TimelineIntValue::class.java,
			TimelineStringValue::class.java,
			TimelineOptionalStringValue::class.java,

			TimelineOptionalPlayerValue::class.java,
			TimelineCharacterStateValue::class.java,
			TimelineColorTransformValue::class.java,
			TimelineOptionalColorTransformValue::class.java,
		)
	}
}

/**
 * A `TimelineValue` without any content. This is used for 'flags' that are either present, or not.
 */
@BitStruct(backwardCompatible = true)
class TimelineUnitValue : TimelineValue<Unit>() {

	override fun get() = Unit
}

/**
 * A `TimelineValue` that wraps a boolean.
 */
@BitStruct(backwardCompatible = true)
class TimelineBooleanValue(

	/**
	 * The wrapped boolean value
	 */
	@BitField(id = 0)
	val value: Boolean
) : TimelineValue<Boolean>() {

	@Suppress("unused")
	private constructor() : this(false)

	override fun get() = value
}

/**
 * A `TimelineValue` that wraps an integer.
 */
@BitStruct(backwardCompatible = true)
class TimelineIntValue(

	/**
	 * The wrapped integer value
	 */
	@BitField(id = 0)
	@IntegerField(expectUniform = false)
	val value: Int
) : TimelineValue<Int>() {

	@Suppress("unused")
	private constructor() : this(0)

	override fun toString() = "TimelineInt($value)"

	override fun get() = value
}

/**
 * A `TimelineValue` that wraps a string
 */
@BitStruct(backwardCompatible = true)
class TimelineStringValue(

	/**
	 * The wrapped string value
	 */
	@BitField(id = 0)
	val value: String
) : TimelineValue<String>() {

	@Suppress("unused")
	private constructor() : this("")

	override fun toString() = "TimelineString($value)"

	override fun get() = value
}

/**
 * A `TimelineValue` that wraps a nullable string
 */
@BitStruct(backwardCompatible = true)
class TimelineOptionalStringValue(

	/**
	 * The wrapped string value, or `null`
	 */
	@BitField(id = 0, optional = true)
	val value: String?
) : TimelineValue<String?>() {

	@Suppress("unused")
	private constructor() : this("")

	override fun get() = value

	override fun toString() = "TimelineString?($value)"
}

/**
 * A `TimelineValue` that wraps a nullable `PlayableCharacter`.
 *
 * Such values are used for `FixedVariables.forcedPartyMembers`.
 */
@BitStruct(backwardCompatible = true)
class TimelineOptionalPlayerValue(

	/**
	 * The wrapped playable character, or `null`
	 */
	@BitField(id = 0, optional = true)
	@ReferenceField(stable = false, label = "playable characters")
	val value: PlayableCharacter?
) : TimelineValue<PlayableCharacter?>() {

	@Suppress("unused")
	private constructor() : this(PlayableCharacter())

	override fun get() = value
}

/**
 * A `TimelineValue` that wraps a `CharacterState`.
 *
 * Such values are used for `PlayableCharacter.stateVariable`.
 */
@BitStruct(backwardCompatible = true)
class TimelineCharacterStateValue(

	/**
	 * The wrapped character state
	 */
	@BitField(id = 0)
	val value: CharacterState,
) : TimelineValue<CharacterState>() {

	@Suppress("unused")
	private constructor() : this(CharacterState())

	override fun get() = value
}

/**
 * A `TimelineValue` that wraps a `ColorTransform`.
 *
 * Such values are used for area ambience.
 */
@BitStruct(backwardCompatible = true)
class TimelineColorTransformValue(

	/**
	 * The wrapped color transformation
	 */
	@BitField(id = 0)
	val value: ColorTransform
) : TimelineValue<ColorTransform>() {

	@Suppress("unused")
	private constructor() : this(ColorTransform())

	override fun toString() = value.toString()

	override fun get() = value
}

/**
 * A `TimelineValue` that wraps a nullable `ColorTransform.
 *
 * Such values are sometimes used for area ambiences.
 */
@BitStruct(backwardCompatible = true)
class TimelineOptionalColorTransformValue(

	/**
	 * The wrapped color transform, or `null`
	 */
	@BitField(id = 0, optional = true)
	val value: ColorTransform?
) : TimelineValue<ColorTransform?>() {

	@Suppress("unused")
	private constructor() : this(null)

	override fun get() = value
}
