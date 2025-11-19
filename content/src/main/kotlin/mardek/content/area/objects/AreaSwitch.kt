package mardek.content.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceField

/**
 * Represents a switch gate, orb, or platform. This class is convenient, since each subclass needs the same fields.
 */
sealed class AreaSwitch(

	/**
	 * The 'color' of this gate, orb, or platform. Toggling a switch orb with color X will toggle all switch gates
	 * and switch platforms with color X, in all areas with the same `dungeon` as the current area.
	 */
	@BitField(id = 0)
	@ReferenceField(stable = false, label = "switch colors")
	val color: SwitchColor,

	x: Int,
	y: Int,
) : StaticAreaObject(x, y) {

	override fun toString() = "${this::class.java.simpleName.substring(4)}($color, x=$x, y=$y)"
}

/**
 * Represents a switch orb. Players can interact with switch orbs to toggle switch gates and switch platforms with the
 * same color as the switch orb.
 */
@BitStruct(backwardCompatible = true)
class AreaSwitchOrb(color: SwitchColor, x: Int, y: Int): AreaSwitch(color, x, y) {

	@Suppress("unused")
	private constructor() : this(SwitchColor(), 0, 0)
}

/**
 * Represents a switch gate. Switch gates will prevent the player from walking on its tile if and only if the
 * corresponding switch orb is disabled.
 */
@BitStruct(backwardCompatible = true)
class AreaSwitchGate(color: SwitchColor, x: Int, y: Int): AreaSwitch(color, x, y) {

	@Suppress("unused")
	private constructor() : this(SwitchColor(), 0, 0)
}

/**
 * Represents a switch platform. Switch platforms are built on inaccessible tiles, but a switch platform will allow
 * the player to walk over that tile if and only if the corresponding switch orb is disabled.
 */
@BitStruct(backwardCompatible = true)
class AreaSwitchPlatform(color: SwitchColor, x: Int, y: Int): AreaSwitch(color, x, y) {

	@Suppress("unused")
	private constructor() : this(SwitchColor(), 0, 0)
}
