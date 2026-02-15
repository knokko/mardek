package mardek.content.action

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.BITSER
import mardek.content.area.objects.AreaCharacter
import mardek.content.characters.PlayableCharacter
import mardek.content.portrait.PortraitInfo
import mardek.content.stats.Element
import java.util.UUID

/**
 * Represents the *target* of an *action*, which is usually a player character.
 * - In a dialogue/talk action, the target is the speaker
 * - In a walk action, the target is the (playable) character that is supposed to walk
 */
@BitStruct(backwardCompatible = true)
sealed class ActionTarget {

	/**
	 * Gets the display name of this action target, which should be used when the target is used in a dialogue/talk
	 * action.
	 *
	 * This method returns `null` when this target is missing, or is not supposed to be used as dialogue target.
	 */
	abstract fun getDisplayName(defaultObject: ActionTargetData?, party: Array<PlayableCharacter?>): String?

	/**
	 * Gets the element of this action target, which should be used when the target is used in a dialogue/talk
	 * action.
	 *
	 * This method returns `null` when this target is missing, or does not have an element (e.g. save crystal).
	 */
	abstract fun getElement(defaultObject: ActionTargetData?, party: Array<PlayableCharacter?>): Element?

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)

	companion object {

		@JvmStatic
		@Suppress("unused")
		private val BITSER_HIERARCHY = arrayOf(
			ActionTargetPartyMember::class.java,
			ActionTargetPlayer::class.java,
			ActionTargetWholeParty::class.java,
			ActionTargetDialogueObject::class.java,
			ActionTargetAreaCharacter::class.java,
			ActionTargetDefaultDialogueObject::class.java,
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

	override fun getDisplayName(
		defaultObject: ActionTargetData?, party: Array<PlayableCharacter?>
	) = party[index]?.name

	override fun getElement(
		defaultObject: ActionTargetData?, party: Array<PlayableCharacter?>
	) = party[index]?.element
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

	override fun toString() = "Player($player)"

	override fun getDisplayName(defaultObject: ActionTargetData?, party: Array<PlayableCharacter?>) = player.name

	override fun getElement(defaultObject: ActionTargetData?, party: Array<PlayableCharacter?>) = player.element
}

/**
 * Targets the whole party. This is often used in walking actions.
 */
@BitStruct(backwardCompatible = true)
class ActionTargetWholeParty : ActionTarget() {
	override fun toString() = "WholeParty"

	override fun getDisplayName(
		defaultObject: ActionTargetData?, party: Array<PlayableCharacter?>
	) = "everyone"

	override fun getElement(defaultObject: ActionTargetData?, party: Array<PlayableCharacter?>) = null
}

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

	override fun toString() = "DialogueObject($displayName)"

	override fun getDisplayName(
		defaultObject: ActionTargetData?, party: Array<PlayableCharacter?>
	) = displayName

	override fun getElement(defaultObject: ActionTargetData?, party: Array<PlayableCharacter?>) = null
}

/**
 * Targets an `AreaCharacter`
 */
@BitStruct(backwardCompatible = true)
class ActionTargetAreaCharacter(
	/**
	 * The UUID of the character to target. Due to some cyclic reference issues, we can often not access the
	 * `AreaCharacter` before constructing the `ActionTargetAreaCharacter`. Instead, we pass its ID to the constructor,
	 * and use `ActionTargetAreaCharacter.resolve(content)` to resolve the ID...
	 */
	private val characterID: UUID,

	/**
	 * When this target is moved or rotated during an [ActionWalk] or [ActionRotate], this field determines whether the
	 * movement or rotation *persists* after the current `AreaActionsState` is finished.
	 *
	 * When this is `false`, the movement and rotations will be 'rolled back' after the `AreaActionsState` is over.
	 */
	@BitField(id = 1)
	val persistent: Boolean = true,
) : ActionTarget() {

	constructor(character: AreaCharacter) : this() {
		this.character = character
	}

	/**
	 * The character to target
	 */
	@BitField(id = 0)
	@ReferenceField(stable = false, label = "area characters")
	var character: AreaCharacter = AreaCharacter()
		private set

	internal constructor() : this(UUID(0, 0))

	override fun toString() = "AreaCharacter($character ($characterID))"

	override fun getDisplayName(
		defaultObject: ActionTargetData?, party: Array<PlayableCharacter?>
	) = character.name

	override fun getElement(
		defaultObject: ActionTargetData?, party: Array<PlayableCharacter?>
	) = character.element

	/**
	 * This method initializes `character` to the `AreaCharacter` whose ID is `AreaCharacter`. This method should only
	 * be needed by the importer.
	 */
	fun resolve(mapping: Map<UUID, AreaCharacter>) {
		this.character = mapping[characterID]!!
	}
}

/**
 * This is a special target that can only be used when the player is interacting with an area object or character.
 * This target will refer to that area object/character.
 */
@BitStruct(backwardCompatible = true)
class ActionTargetDefaultDialogueObject : ActionTarget() {

	override fun getDisplayName(
		defaultObject: ActionTargetData?, party: Array<PlayableCharacter?>
	) = defaultObject!!.displayName

	override fun getElement(
		defaultObject: ActionTargetData?, party: Array<PlayableCharacter?>
	) = defaultObject!!.element
}

/**
 * This class stores all the data needed by [ActionTargetDefaultDialogueObject]. An instance of this class is stored
 * in `AreaActionsState`.
 */
@BitStruct(backwardCompatible = true)
class ActionTargetData(
	/**
	 * The display name that will be returned by [ActionTargetDefaultDialogueObject.getDisplayName]
	 */
	@BitField(id = 0)
	val displayName: String,

	/**
	 * The element that will be returned by [ActionTargetDefaultDialogueObject.getElement]
	 */
	@BitField(id = 1, optional = true)
	@ReferenceField(stable = true, label = "elements")
	val element: Element?,

	/**
	 * The portrait of this character (if present)
	 */
	@BitField(id = 2, optional = true)
	@ReferenceField(stable = false, label = "portrait info")
	val portraitInfo: PortraitInfo?,
) {
	@Suppress("unused")
	private constructor() : this("", null, null)
}
