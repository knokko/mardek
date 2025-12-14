package mardek.content.world

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceField

/**
 * Represents an edge on a world map. While on the world map, the player can walk over such edges to travel from one
 * `WorldMapNode` to another.
 */
@BitStruct(backwardCompatible = true)
class WorldMapEdge(

	/**
	 * The 'first' node connected by this edge. Note that edges are symmetric, so it doesn't matter which node is the
	 * 'first' node, and which is the 'second' node.
	 */
	@BitField(id = 0)
	@ReferenceField(stable = false, label = "world map nodes")
	val node1: WorldMapNode,

	/**
	 * The node to which `node1` gets connected by this edge.
	 */
	@BitField(id = 1)
	@ReferenceField(stable = false, label = "world map nodes")
	val node2: WorldMapNode,
) {

	@Suppress("unused")
	private constructor() : this(WorldMapNode(), WorldMapNode())
}
