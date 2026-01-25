package mardek.content.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceFieldTarget

/**
 * This class captures all decorations, objects, characters, etc... that an `Area` can have.
 */
@BitStruct(backwardCompatible = true)
class AreaObjects(

	/**
	 * The characters that spawn in this area when the player enters this area. Most characters have different sprites,
	 * depending on which direction they face.
	 */
	@BitField(id = 0)
	@ReferenceFieldTarget(label = "area characters")
	val characters: ArrayList<AreaCharacter>,

	/**
	 * The decorations that spawn in this area when the player enters this area. Players can walk through some
	 * decorations, and interact with some of them.
	 */
	@BitField(id = 1)
	val decorations: ArrayList<AreaDecoration>,

	/**
	 * The doors of this area. Players can interact with doors to go to the destination of that door.
	 */
	@BitField(id = 2)
	@ReferenceFieldTarget(label = "doors")
	val doors: ArrayList<AreaDoor>,

	/**
	 * The portals of this area. Portals are basically doors, except that players activate them by walking on them
	 * rather than interacting with them.
	 */
	@BitField(id = 3)
	val portals: ArrayList<AreaPortal>,

	/**
	 * The shops in this area
	 */
	@BitField(id = 4)
	val shops: ArrayList<AreaShop>,

	/**
	 * The switch gates in this area. Switch gates will block the player when the corresponding *switch orb*
	 * is disabled.
	 */
	@BitField(id = 5)
	val switchGates: ArrayList<AreaSwitchGate>,

	/**
	 * The switch orbs in this area. Players can interact with switch orbs to toggle the switch gates and switch
	 * platforms.
	 */
	@BitField(id = 6)
	val switchOrbs: ArrayList<AreaSwitchOrb>,

	/**
	 * The switch platforms in this area. Switch platforms are bridges over normally-inaccessible tiles, which allow
	 * the player to walk over the underlying tile *if* the corresponding *switch orb* is disabled.
	 */
	@BitField(id = 7)
	val switchPlatforms: ArrayList<AreaSwitchPlatform>,

	/**
	 * The 'talk triggers' in this area. Each talk trigger is linked to a character, and interacting with the talk
	 * trigger causes the player to interact with that character.
	 */
	@BitField(id = 8)
	val talkTriggers: ArrayList<AreaTalkTrigger>,

	/**
	 * When a player steps on a transition tile, the player will go to the *destination* of the transition.
	 * Transitions are basically portals, except that they are displayed as 'oscillating' blue arrows rather than
	 * portal textures.
	 */
	@BitField(id = 9)
	val transitions: ArrayList<AreaTransition>,

	/**
	 * The 'walk triggers' in this area. Walk triggers should have an action sequence, which will be activated when
	 * the player steps on the (typically invisible) trigger.
	 */
	@BitField(id = 10)
	@ReferenceFieldTarget(label = "area triggers")
	val walkTriggers: ArrayList<AreaTrigger>,
) {
	constructor() : this(
		ArrayList(), ArrayList(), ArrayList(), ArrayList(),
		ArrayList(), ArrayList(), ArrayList(), ArrayList(),
		ArrayList(), ArrayList(), ArrayList(),
	)
}
