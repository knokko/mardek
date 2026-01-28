package mardek.state.ingame.worldmap

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.area.Area

/**
 * Represents the position where the player exited an area (either through an exit, or by warping out),
 * to enter the world map.
 */
@BitStruct(backwardCompatible = true)
class AreaExitPoint(

	/**
	 * The area that the player left
	 */
	@BitField(id = 0)
	@ReferenceField(stable = true, label = "areas")
	val area: Area,

	/**
	 * The X-coordinate of the player when it left the area
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = false)
	val x: Int,

	/**
	 * The Y-coordinate of the player when it left the area
	 */
	@BitField(id = 2)
	@IntegerField(expectUniform = false)
	val y: Int,
) {
	internal constructor() : this(Area(), 0, 0)
}