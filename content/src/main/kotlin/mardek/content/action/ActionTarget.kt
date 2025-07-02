package mardek.content.action

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.characters.PlayableCharacter

/**
 * Represents the *target* of an *action*, which is usually a player character.
 */
@BitStruct(backwardCompatible = true)
sealed class ActionTarget {
	companion object {

		@JvmStatic
		@Suppress("unused")
		val BITSER_HIERARCHY = arrayOf(
			ActionTargetPartyMember::class.java,
			ActionTargetPlayer::class.java,
			ActionTargetWholeParty::class.java,
			ActionTargetDialogueObject::class.java,
		)
	}
}

/**
 * Targets the party member with the given index (e.g. 0 is Mardek, 1 and 2 are Deugan and Emela in chapter 2)
 */
@BitStruct(backwardCompatible = true)
class ActionTargetPartyMember(

	@BitField(id = 0)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 3)
	val index: Int
) : ActionTarget() {

	internal constructor() : this(0)

	override fun toString() = "PartyMember($index)"

	override fun equals(other: Any?) = other is ActionTargetPartyMember && this.index == other.index

	override fun hashCode() = index
}

/**
 * Targets a fixed player character, regardless of whether this character is currently in the player's party.
 */
@BitStruct(backwardCompatible = true)
class ActionTargetPlayer(

	@BitField(id = 0)
	@ReferenceField(stable = false, label = "playable characters")
	val player: PlayableCharacter
) : ActionTarget() {

	@Suppress("unused")
	private constructor() : this(PlayableCharacter())
}

/**
 * Targets the whole party. This is often used in walking actions.
 */
@BitStruct(backwardCompatible = true)
class ActionTargetWholeParty : ActionTarget()

/**
 * This target can only be used as `speaker` in dialogue actions, which means that the 'speaker' is an object without
 * portrait.
 */
@BitStruct(backwardCompatible = true)
class ActionTargetDialogueObject(
	/**
	 * The display name of the object above the dialogue box, e.g. "Save Crystal"
	 */
	@BitField(id = 0)
	val displayName: String
) : ActionTarget() {

	@Suppress("unused")
	private constructor() : this("")
}
