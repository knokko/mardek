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
	 * The characters that spawn in this area when the player enters this area. Characters have different sprites,
	 * depending on which direction they face.
	 */
	@BitField(id = 0)
	val characters: ArrayList<AreaCharacter>,

	// TODO CHAP1 Code reuse: create abstract StaticAreaObject class with (x, y) that most objects extend
	/**
	 * The decorations that spawn in this area when the player enters this area. Players can walk through all
	 * decorations, and interact with some of them.
	 */
	@BitField(id = 1)
	val decorations: ArrayList<AreaDecoration>,

	/**
	 * The doors of this area. Players can interact with doors to go to the destination of that door.
	 */
	@BitField(id = 2)
	val doors: ArrayList<AreaDoor>,

	/**
	 * The objects that spawn in this area when the player enters this area. Unlike decorations, players can
	 * **not** move through objects. And unlike characters, the sprite/animation of objects does *not* depend on
	 * their direction.
	 */
	@BitField(id = 3)
	val objects: ArrayList<AreaObject>,

	/**
	 * The portals of this area. Portals are basically doors, except that players activate them by walking on them
	 * rather than interacting with them.
	 */
	@BitField(id = 4)
	val portals: ArrayList<AreaPortal>,

	/**
	 * The shops in this area
	 */
	@BitField(id = 5)
	val shops: ArrayList<AreaShop>,

	/**
	 * The switch gates in this area. Switch gates will block the player when the corresponding *switch orb*
	 * is disabled.
	 */
	@BitField(id = 6)
	val switchGates: ArrayList<AreaSwitchGate>,

	/**
	 * The switch orbs in this area. Players can interact with switch orbs to toggle the switch gates and switch
	 * platforms.
	 */
	@BitField(id = 7)
	val switchOrbs: ArrayList<AreaSwitchOrb>,

	/**
	 * The switch platforms in this area. Switch platforms are bridges over normally-inaccessible tiles, which allow
	 * the player to walk over the underlying tile *if* the corresponding *switch orb* is disabled.
	 */
	@BitField(id = 8)
	val switchPlatforms: ArrayList<AreaSwitchPlatform>,

	/**
	 * The 'talk triggers' in this area. Each talk trigger is linked to a character, and interacting with the talk
	 * trigger causes the player to interact with that character.
	 */
	@BitField(id = 9)
	val talkTriggers: ArrayList<AreaTalkTrigger>,

	/**
	 * When a player steps on a transition tile, the player will go to the *destination* of the transition.
	 * Transitions are basically portals, except that they are displayed as 'oscillating' blue arrows rather than
	 * portal textures.
	 */
	@BitField(id = 10)
	val transitions: ArrayList<AreaTransition>,

	/**
	 * The 'walk triggers' in this area. Walk triggers should have an action sequence, which will be activated when
	 * the player steps on the (typically invisible) trigger.
	 */
	@BitField(id = 11)
	@ReferenceFieldTarget(label = "area triggers")
	val walkTriggers: ArrayList<AreaTrigger>,
) {
	internal constructor() : this(
		ArrayList(), ArrayList(), ArrayList(), ArrayList(), ArrayList(), ArrayList(),
		ArrayList(), ArrayList(), ArrayList(), ArrayList(), ArrayList(), ArrayList()
	)
}
