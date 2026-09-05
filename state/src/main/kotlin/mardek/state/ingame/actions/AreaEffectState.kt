package mardek.state.ingame.actions

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import mardek.content.BITSER
import mardek.content.util.Time
import java.io.Serializable

/**
 * Represents the state of an [mardek.content.action.effect.AreaActionEffect] instance.
 *
 * Note that the coordinates are expressed in *tile pixel* coordinates rather than tile coordinates,
 * since this allows effects to be spawned e.g. in the middle of tiles.
 *
 * When a tile has *tile* coordinates (2, 3), the *tile pixel* coordinates of its top-left corner are
 * (2 * 16, 3 * 16) = (32, 48).
 */
@BitStruct(backwardCompatible = true)
class AreaEffectState(

	/**
	 * The value of [mardek.state.ingame.area.AreaState.currentTime] when this effect instance was spawned.
	 */
	@BitField(id = 0)
	val spawnTime: Time,

	/**
	 * The X-coordinate of the position, in tile pixels
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = false)
	var x: Int,

	/**
	 * The Y-coordinate of the position, in tile pixels
	 */
	@BitField(id = 2)
	@IntegerField(expectUniform = false)
	var y: Int,
) : Serializable {

	@Suppress("unused") // TODO BITSER remove
	private constructor() : this(Time.ZERO, 0, 0)

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)

	override fun toString() = "AreaEffectState($x, $y, spawned after $spawnTime)"
}
