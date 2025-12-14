package mardek.content.world

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceFieldTarget
import mardek.content.sprite.BcSprite

/**
 * Represents a world map. Vanilla MARDEK has just 1 world map: Belfan. However, we are going to need multiple once the
 * party starts space-travelling.
 */
@BitStruct(backwardCompatible = true)
class WorldMap(

	/**
	 * The name of this world map, which is only used for editing and debugging
	 */
	@BitField(id = 0)
	val name: String,

	/**
	 * The (background) sprite/image of this world map
	 */
	@BitField(id = 1)
	val sprite: BcSprite,

	/**
	 * All the nodes (areas) on this world map
	 */
	@BitField(id = 2)
	@ReferenceFieldTarget(label = "world map nodes")
	val nodes: Array<WorldMapNode>,

	/**
	 * All the edges (paths between nodes) on this world map
	 */
	@BitField(id = 3)
	val edges: Array<WorldMapEdge>,
) {

	@Suppress("unused")
	private constructor() : this("", BcSprite(), arrayOf(), arrayOf())
}
