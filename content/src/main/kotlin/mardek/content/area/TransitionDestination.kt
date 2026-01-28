package mardek.content.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.BITSER
import mardek.content.world.WorldMap
import mardek.content.world.WorldMapNode

/**
 * Represents the destination of a door, portal, or area transition. It can either be a location in an area, or the
 * world map.
 */
sealed class TransitionDestination {

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)

	companion object {

		@Suppress("unused")
		private val BITSER_HIERARCHY = arrayOf(
			AreaTransitionDestination::class.java,
			WorldMapTransitionDestination::class.java,
		)
	}
}

@BitStruct(backwardCompatible = true)
class AreaTransitionDestination(

	/**
	 * The name of the destination area, which is only used during importing
	 */
	private val areaName: String,

	/**
	 * The X-coordinate of the destination tile.
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = false, digitSize = 2)
	val x: Int,

	/**
	 * The Y-coordinate of the destination tile
	 */
	@BitField(id = 2)
	@IntegerField(expectUniform = false, digitSize = 2)
	val y: Int,

	/**
	 * The direction that the player will face after being moved to this destination.
	 */
	@BitField(id = 3, optional = true)
	val direction: Direction?,
) : TransitionDestination() {

	/**
	 * The destination area
	 */
	@BitField(id = 0)
	@ReferenceField(stable = false, label = "areas")
	var area = Area()
		private set

	internal constructor() : this("", 0, 0, null)

	override fun toString() = "(${area.properties.displayName}, x=$x, y=$y, direction=$direction)"

	/**
	 * This method should only be used during importing
	 */
	fun resolve(areas: AreaContent) {
		this.area = areas.areas.find { it.properties.rawName.equals(areaName, ignoreCase = true) } ?:
				throw IllegalArgumentException("Missing area $areaName")
	}
}

/**
 * This transition destination is the world map
 */
@BitStruct(backwardCompatible = true)
class WorldMapTransitionDestination(
	/**
	 * The name of the area containing this transition, which is only used during importing
	 */
	private val myAreaName: String,
) : TransitionDestination() {

	/**
	 * The world map to which the player will go. For all vanilla destinations, this will be the Belfan map.
	 */
	@BitField(id = 0)
	@ReferenceField(stable = false, label = "world maps")
	var worldMap = WorldMap()
		private set

	/**
	 * The node of [worldMap] to which the player should go
	 */
	@BitField(id = 1)
	@ReferenceField(stable = false, label = "world map nodes")
	var node = WorldMapNode()
		private set

	@Suppress("unused")
	private constructor() : this("")

	override fun toString() = "(${worldMap.name})"

	/**
	 * This method should only be used by the importer
	 */
	fun resolve(areas: AreaContent, belfan: WorldMap) {
		val myArea = areas.areas.find { it.properties.rawName.equals(myAreaName, ignoreCase = true) } ?:
				throw IllegalArgumentException("Can't find $myAreaName")
		this.worldMap = belfan
		this.node = belfan.nodes.find { it.entrances[0].area.properties.displayName == myArea.properties.displayName } ?:
				throw IllegalArgumentException("Missing $myAreaName")
	}
}
