package mardek.state.ingame.worldmap

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.world.WorldMapNode
import kotlin.time.Duration

/**
 * This struct is used for [WorldMapState.exiting]. It is just a tuple `(entrance, exitAt)`.
 */
@BitStruct(backwardCompatible = true)
class ExitingWorldMap(

	/**
	 * The entrance through which the player will enter the area, once the `currentTime` reaches `exitAt`.
	 */
	@BitField(id = 0)
	@ReferenceField(stable = true, label = "world map entrances")
	val entrance: WorldMapNode.Entrance,

	/**
	 * The player will enter the area when [WorldMapState.currentTime] is at least `exitAt`.
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = false)
	val exitAt: Duration,
) {

	@Suppress("unused")
	private constructor() : this(WorldMapNode.Entrance(), Duration.ZERO)
}
