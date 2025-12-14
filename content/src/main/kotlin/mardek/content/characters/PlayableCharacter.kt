package mardek.content.characters

import com.github.knokko.bitser.BitPostInit
import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.ReferenceFieldTarget
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.content.animation.CombatantAnimations
import mardek.content.portrait.PortraitInfo
import mardek.content.stats.CharacterClass
import mardek.content.stats.Element
import mardek.content.stats.StatModifier
import mardek.content.sprite.DirectionalSprites
import mardek.content.stats.CreatureType
import mardek.content.story.FixedTimelineVariable
import java.util.*
import kotlin.collections.ArrayList

/**
 * Represents a character that can be controlled by the player, e.g. Hero Mardek and Deugan.
 */
@BitStruct(backwardCompatible = true)
class PlayableCharacter(

	/**
	 * The (display) name of the character. Note that this is not necessarily unique. For instance, there are 3
	 * `PlayableCharacter`s whose `name` is "Deugan": Hero Deugan, child Deugan, and soldier Deugan. Use
	 * `characterClass.rawName` for a more... unique... name.
	 */
	@BitField(id = 0)
	val name: String,

	/**
	 * The 'class' of the playable character. This determines which weapons/armor the character can carry, and which
	 * action skills it can use. As a bonus, it also provides a unique name for the character.
	 */
	@BitField(id = 1)
	@ReferenceField(stable = false, label = "character classes")
	val characterClass: CharacterClass,

	/**
	 * The element of the playable character (e.g. `Light` for Mardek).
	 */
	@BitField(id = 2)
	@ReferenceField(stable = false, label = "elements")
	val element: Element,

	/**
	 * The base stats of the character. This list should have an entry for `Strength`, `Spirit`, `Vitality`, and
	 * `Agility`.
	 */
	@BitField(id = 3)
	val baseStats: ArrayList<StatModifier>,

	/**
	 * The sprites that will be rendered when this character is walking around in an area.
	 */
	@BitField(id = 4)
	val areaSprites: DirectionalSprites,

	/**
	 * The animations that will be rendered when this character is in combat. Note that one of its animations will be
	 * rendered at any point in time during a battle.
	 */
	@BitField(id = 5)
	val animations: CombatantAnimations,

	/**
	 * The creature type of the playable character, which is almost always HUMAN.
	 */
	@BitField(id = 6)
	val creatureType: CreatureType,

	/**
	 * The portrait of the playable character, which is used in dialogue, in the party tab, and in battle (when the
	 * character is on turn).
	 */
	@BitField(id = 7)
	@ReferenceField(stable = false, label = "portrait info")
	val portraitInfo: PortraitInfo,

	/**
	 * The unique ID of the playable character, which is used for (de)serialization.
	 */
	@BitField(id = 8)
	@StableReferenceFieldId
	val id: UUID,
) : BitPostInit {

	/**
	 * The `FixedTimelineVariable` that should be used to reference the state of this playable character
	 */
	@BitField(id = 9)
	@ReferenceFieldTarget(label = "timeline variables")
	val stateVariable = FixedTimelineVariable<CharacterState>()

	/**
	 * Whether this character can be chosen as party member. This variable is irrelevant when this playable character
	 * is currently *forced* to be a party member (see `FixedTimelineVariables.forcedPartyMembers`)
	 */
	@BitField(id = 10)
	@ReferenceFieldTarget(label = "timeline variables")
	val isAvailable = FixedTimelineVariable<Unit>()

	/**
	 * Whether the inventory of this character can be accessed in the item storage.
	 *
	 * This variable is irrelevant when `isAvailable` evaluates to true, since the inventories of available party
	 * members are always accessible.
	 *
	 * Likewise, this variable is irrelevant when the party member is forcibly in the party, since their inventories
	 * are also accessible.
	 */
	@BitField(id = 11)
	@ReferenceFieldTarget(label = "timeline variables")
	val isInventoryAvailable = FixedTimelineVariable<Unit>()

	init {
		assignVariableNames()
	}

	constructor() : this(
		"", CharacterClass(), Element(), ArrayList(0), DirectionalSprites(),
		CombatantAnimations(), CreatureType(), PortraitInfo(), UUID.randomUUID(),
	)

	override fun toString() = name

	override fun postInit(context: BitPostInit.Context) {
		assignVariableNames()
	}

	private fun assignVariableNames() {
		stateVariable.debugName = "state of $name"
		isAvailable.debugName = "$name is available"
		isInventoryAvailable.debugName = "inventory of $name is available"
	}
}
