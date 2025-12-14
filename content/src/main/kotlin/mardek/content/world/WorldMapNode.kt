package mardek.content.world

import com.github.knokko.bitser.BitPostInit
import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.ReferenceFieldTarget
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.content.area.Area
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
	 * The area that the player can enter by pressing the interact key while standing on this ndoe.
	 */
	@BitField(id = 1)
	@ReferenceField(stable = false, label = "areas")
	val area: Area,

	/**
	 * The X-coordinate of this node, on the `sprite` of its `WorldMap`.
	 */
	@BitField(id = 2)
	@IntegerField(expectUniform = false, minValue = 0)
	val x: Int,

	/**
	 * The Y-coordinate of this node, on the `sprite` of its `WorldMap`.
	 */
	@BitField(id = 3)
	@IntegerField(expectUniform = false, minValue = 0)
	val y: Int,
) : BitPostInit {

	/**
	 * Whether this area was discovered, and can thus be seen on the world map.
	 */
	@BitField(id = 4)
	@ReferenceFieldTarget(label = "timeline variables")
	val wasDiscovered = FixedTimelineVariable<Unit>()

	init {
		wasDiscovered.debugName = "${area.properties.displayName} was discovered"
	}

	internal constructor() : this(UUID(0, 0), Area(), 0, 0)

	override fun postInit(context: BitPostInit.Context) {
		wasDiscovered.debugName = "${area.properties.displayName} was discovered"
	}
}
