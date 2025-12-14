package mardek.content.story

import com.github.knokko.bitser.BitPostInit
import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceFieldTarget
import mardek.content.characters.PlayableCharacter

/**
 * Represents a 'variable' whose value is always *derived* from the state of the story/[Timeline]s.
 *
 * Since the value is always derived, it is not stored anywhere in the game state, which is convenient for keeping
 * save files compatible between game updates.
 *
 * The values of timeline variables are 'assigned' by [TimelineNode.variables], when that timeline node is *active*,
 * which depends on the story/timeline state.
 */
sealed class TimelineVariable<T> {

	companion object {

		@JvmStatic
		@Suppress("unused")
		val BITSER_HIERARCHY = arrayOf(
			FixedTimelineVariable::class.java,
			CustomTimelineVariable::class.java,
		)
	}
}

/**
 * A timeline variable that has a special meaning for the engine, for instance:
 * - Every playable character has an `isAvailable` variable, which determines whether the player can put that character
 * in the party.
 * - The content has a `blockItemStorage` variable that determines whether the item storage is currently blocked
 * (e.g. in Dragon's Lair).
 */
@BitStruct(backwardCompatible = true)
class FixedTimelineVariable<T>() : TimelineVariable<T>() {

	internal var debugName = ""

	override fun toString() = "FixedTimelineVariable($debugName)"
}

/**
 * A named timeline variable without special meaning for the engine. Custom timeline variables can be used in
 * [TimelineExpression]s and assigned by [TimelineNode]s (just like fixed timeline variables).
 */
@BitStruct(backwardCompatible = true)
class CustomTimelineVariable<T>(

	/**
	 * The name of the variable, which is only used for debugging/editing.
	 */
	@BitField(id = 0)
	val name: String,
) : TimelineVariable<T>() {

	internal constructor() : this("")

	override fun toString() = "CustomTimelineVariable($name)"
}

/**
 * This class holds the [FixedTimelineVariable]s that are *not* tied to anything else.
 *
 * Note that this class does *not* contain *all* fixed variables:
 * for instance each quest has its own [Quest.isActive] variable.
 */
@BitStruct(backwardCompatible = true)
class FixedTimelineVariables : BitPostInit {

	/**
	 * The current chapter number, which should be 1, 2, or 3 in vanilla MARDEK.
	 */
	@BitField(id = 0)
	@ReferenceFieldTarget(label = "timeline variables")
	val chapter = FixedTimelineVariable<Int>()

	/**
	 * The party members that are currently *forced* in each party slot. For instance, if `forcedPartyMembers[0]]` is
	 * Mardek, then Mardek is the 'first' party member, and the player can *not* put any other playable character in the
	 * first slot.
	 */
	@BitField(id = 1)
	@ReferenceFieldTarget(label = "timeline variables")
	val forcedPartyMembers = Array(4) { FixedTimelineVariable<PlayableCharacter?>() }

	/**
	 * Whether the item storage should be blocked. If this variable is assigned (to `TimelineUnitValue()`), the item
	 * storage can *not* be accessed from any save crystal.
	 *
	 * This is used e.g. in Dragon's Lair to prevent the player from putting the ridiculously strong armor of Hero
	 * Mardek & Hero Deugan in the item storage, and taking them from the item storage as Child Mardek & Child Deugan.
	 */
	@BitField(id = 2)
	@ReferenceFieldTarget(label = "timeline variables")
	val blockItemStorage = FixedTimelineVariable<Unit>()

	/**
	 * Whether the music of random battles should be blocked. If this variable is assigned (to `TimelineUnitValue()`),
	 * the music will *not* be changed during random battles. This is used e.g. in Dragon's Lair, where the Mighty
	 * Heroes music keeps playing when the player encounters a random battle.
	 */
	@BitField(id = 3)
	@ReferenceFieldTarget(label = "timeline variables")
	val blockRandomBattleMusic = FixedTimelineVariable<Unit>()

	init {
		assignDebugNames()
	}

	private fun assignDebugNames() {
		for (field in FixedTimelineVariables::class.java.declaredFields.filter {
			it.type == FixedTimelineVariable::class.java
		}) {
			val variable = field.get(this) as FixedTimelineVariable<*>
			variable.debugName = field.name
		}
		for ((index, variable) in forcedPartyMembers.withIndex()) {
			variable.debugName = "forcedPartyMembers[$index]"
		}
	}

	override fun postInit(context: BitPostInit.Context) {
		assignDebugNames()
	}
}
