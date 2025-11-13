package mardek.content.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import mardek.content.BITSER

/**
 * Special properties/flags that areas can have.
 */
@BitStruct(backwardCompatible = true)
class AreaFlags(
	/**
	 * Whether the player is allowed to use the Warp spell
	 */
	@BitField(id = 0)
	val canWarp: Boolean, // TODO CHAP2 Special case is start of chapter 3

	/**
	 * Whether this area is automatically discovered on the area map. This is typically true in civilized areas like
	 * Goznor, and typically false in dungeons like Soothwood.
	 */
	@BitField(id = 1)
	val hasClearMap: Boolean,

	/**
	 * Whether the camera should be fixed rather than following the player? It's true in the Aeropolis theatre and
	 * the Earth Temple puzzle.
	 */
	@BitField(id = 2)
	val noMovingCamera: Boolean,

	/**
	 * Whether most of the party should be hidden? This is true in the Aeropolis theatre and the Earth Temple puzzle.
	 */
	@BitField(id = 3)
	val hideParty: Boolean,

	/**
	 * This flag is true when you're in the Astral Tunnel. It means that you can't switch to party members,
	 * despite already having unlocked them.
	 */
	@BitField(id = 4)
	val noSwitch: Boolean,

	/**
	 * This flag is true when you're in the Astral Tunnel. It means that you can't discover anything on the area map.
	 */
	@BitField(id = 5)
	val noMap: Boolean,

	/**
	 * This flag is true in the miasmal citadel maps. It probably means that we should display the miasma effect.
	 */
	@BitField(id = 6)
	val miasma: Boolean,

	/**
	 * This flag is true in Dragon's Lair. It probably means that the player can't access the item storage via save
	 * crystals.
	 */
	@BitField(id = 7)
	val noStorage: Boolean,
) {

	internal constructor() : this(
		false, false, false, false,
		false, false, false, false
	)

	override fun toString() = "AreaFlags(canWarp=$canWarp, clearMap=$hasClearMap, noMovingCamera=$noMovingCamera, " +
			"hideParty=$hideParty, noSwitch=$noSwitch, noMap=$noMap, miasma=$miasma, noStorage=$noStorage"

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)
}