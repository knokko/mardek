package mardek.assets.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField

@BitStruct(backwardCompatible = false)
class AreaFlags(
	/**
	 * Whether the player is allowed to use the Warp spell
	 */
	@BitField(ordering = 0)
	val canWarp: Boolean, // TODO Special case is start of chapter 3

	/**
	 * Hm... I'm not quite sure what this does... this flag is true in all kinds of places, and 162 / 258 areas
	 */
	@BitField(ordering = 1)
	val hasClearMap: Boolean,

	/**
	 * Whether the camera should be fixed rather than following the player? It's true in the Aeropolis theatre and
	 * the Earth Temple puzzle.
	 */
	@BitField(ordering = 2)
	val noMovingCamera: Boolean,

	/**
	 * Whether most of the party should be hidden? This is true in the Aeropolis theatre and the Earth Temple puzzle.
	 */
	@BitField(ordering = 3)
	val hideParty: Boolean,

	/**
	 * This flag is true when you're in the Astral Tunnel. It means that you can't switch to party members,
	 * despite already having unlocked them.
	 */
	@BitField(ordering = 4)
	val noSwitch: Boolean,

	/**
	 * This flag is true when you're in the Astral Tunnel. It means that you can't discover anything on the area map.
	 */
	@BitField(ordering = 5)
	val noMap: Boolean,

	/**
	 * This flag is true in the miasmal citadel maps. It probably means that we should display the miasma effect.
	 */
	@BitField(ordering = 6)
	val miasma: Boolean,

	/**
	 * This flag is true in Dragon's Lair. It probably means that the player can't access the item storage via save
	 * crystals.
	 */
	@BitField(ordering = 7)
	val noStorage: Boolean,
) {

	internal constructor() : this(
		false, false, false, false,
		false, false, false, false
	)

	override fun toString() = "AreaFlags(canWarp=$canWarp, clearMap=$hasClearMap, noMovingCamera=$noMovingCamera, " +
			"hideParty=$hideParty, noSwitch=$noSwitch, noMap=$noMap, miasma=$miasma, noStorage=$noStorage"

	override fun equals(other: Any?) = other is AreaFlags && canWarp == other.canWarp &&
			hasClearMap == other.hasClearMap && noMovingCamera == other.noMovingCamera &&
			hideParty == other.hideParty && noSwitch == other.noSwitch && noMap == other.noMap &&
			miasma == other.miasma && noStorage == other.noStorage

	override fun hashCode() = canWarp.hashCode() + 2 * hasClearMap.hashCode() + 4 * noMovingCamera.hashCode() +
			8 * hideParty.hashCode() + 16 * noSwitch.hashCode() + 32 * noMap.hashCode() + 64 * miasma.hashCode() +
			128 * noStorage.hashCode()
}