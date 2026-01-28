package mardek.content.world

import com.github.knokko.bitser.BitPostInit
import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.ReferenceFieldTarget
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.content.area.Area
import mardek.content.area.Direction
import mardek.content.story.FixedTimelineVariable
import java.util.UUID

/**
 * Represents a node on a world map. Each node belongs to an area, and the player can enter the area by pressing the
 * interact key (E or X) while standing on the node.
 */
@BitStruct(backwardCompatible = true)
class WorldMapNode(

	/**
	 * The unique ID of this node, which is used for (de)serialization
	 */
	@BitField(id = 0)
	@StableReferenceFieldId
	val id: UUID,

	/**
	 * The X-coordinate of this node, on the `sprite` of its `WorldMap`.
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	val x: Int,

	/**
	 * The Y-coordinate of this node, on the `sprite` of its `WorldMap`.
	 */
	@BitField(id = 2)
	@IntegerField(expectUniform = false, minValue = 0)
	val y: Int,

	/**
	 * The positions where the player can enter the area. Some areas have only 1 entrance (e.g. Goznor),
	 * whereas other areas have multiple (e.g. Soothwood has both an east entrance and a west entrance). The selected
	 * entrance depends on the direction that the player came from, take Soothwood as an example:
	 * - If the player goes from Goznor to Soothwood, the east entrance is used.
	 * - If the player goes from Crash site to Soothwood, the west entrance is used.
	 * - If the player just exited the Soothwood, or warped out, the nearest entrance will be used.
	 */
	@BitField(id = 3)
	@ReferenceFieldTarget(label = "world map entrances")
	val entrances: Array<Entrance>,
) : BitPostInit {

	/**
	 * Whether this area was discovered, and can thus be seen on the world map.
	 */
	@BitField(id = 4)
	@ReferenceFieldTarget(label = "timeline variables")
	val wasDiscovered = FixedTimelineVariable<Unit>()

	init {
		if (entrances.isNotEmpty()) {
			wasDiscovered.debugName = "${entrances[0].area.properties.displayName} was discovered"
		}
	}

	constructor() : this(UUID(0, 0), 0, 0, emptyArray())

	override fun toString() = if (entrances.isNotEmpty()) "dummy" else entrances[0].area.properties.displayName

	override fun postInit(context: BitPostInit.Context) {
		wasDiscovered.debugName = "${entrances[0].area.properties.displayName} was discovered"
	}

	/**
	 * Represents a position where the player can enter the area. Some areas have only 1 entrance (e.g. Goznor),
	 * whereas other areas have multiple (e.g. Soothwood has both an east entrance and a west entrance).
	 */
	@BitStruct(backwardCompatible = true)
	class Entrance(

		/**
		 * The unique ID of this entrance, which is used for (de)serialization.
		 */
		@BitField(id = 0)
		@StableReferenceFieldId
		val id: UUID,

		/**
		 * The area that the player will enter
		 */
		@BitField(id = 1)
		@ReferenceField(stable = false, label = "areas")
		val area: Area,

		/**
		 * The X-coordinate where the player will enter the area
		 */
		@BitField(id = 2)
		@IntegerField(expectUniform = false)
		val x: Int,

		/**
		 * The Y-coordinate where the player will enter the area
		 */
		@BitField(id = 3)
		@IntegerField(expectUniform = false)
		val y: Int,

		/**
		 * The direction that the player will face upon entering the area
		 */
		@BitField(id = 4)
		val direction: Direction,
	) {

		constructor() : this(UUID.randomUUID(), Area(), 0, 0, Direction.Up)

		override fun toString() = "${area.properties.rawName}($x, $y)"
	}
}
