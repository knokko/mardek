package mardek.state.ingame.worldmap

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.world.WorldMapNode
import kotlin.time.Duration

/**
 * This class is used in [WorldMapState.nextNode], and simply tells the destination node + the arrival time.
 */
@BitStruct(backwardCompatible = true)
class NextWorldMapNode(

	/**
	 * The destination node that the player will reach at [arrivalTime]
	 */
	@BitField(id = 0)
	@ReferenceField(stable = true, label = "world map nodes")
	val destination: WorldMapNode,

	/**
	 * The time at which the player started walking towards [destination]
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	val startTime: Duration,

	/**
	 * The time at which the player will reach [destination]
	 */
	@BitField(id = 2)
	@IntegerField(expectUniform = false, minValue = 0)
	val arrivalTime: Duration,
) {

	@Suppress("unused")
	private constructor() : this(WorldMapNode(), Duration.ZERO, Duration.ZERO)
}
