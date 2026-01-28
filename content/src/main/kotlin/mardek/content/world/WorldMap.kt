package mardek.content.world

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField
import com.github.knokko.bitser.field.ReferenceFieldTarget
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.content.sprite.BcSprite
import mardek.content.story.ConstantTimelineExpression
import mardek.content.story.TimelineExpression
import mardek.content.story.TimelineOptionalStringValue
import java.util.UUID

/**
 * Represents a world map. Vanilla MARDEK has just 1 world map: Belfan. However, we are going to need multiple once the
 * party starts space-travelling.
 */
@BitStruct(backwardCompatible = true)
class WorldMap(

	/**
	 * The unique ID of this world map, which is used for (de)serialization
	 */
	@BitField(id = 0)
	@StableReferenceFieldId
	val id: UUID,

	/**
	 * The name of this world map, which is only used for editing and debugging
	 */
	@BitField(id = 1)
	val name: String,

	/**
	 * The (background) sprite/image of this world map
	 */
	@BitField(id = 2)
	val sprite: BcSprite,

	/**
	 * The name of the music track that should be played while the player is on this world map
	 */
	@BitField(id = 3)
	@ClassField(root = TimelineExpression::class)
	val music: TimelineExpression<String?>,

	/**
	 * All the nodes (areas) on this world map
	 */
	@BitField(id = 4)
	@ReferenceFieldTarget(label = "world map nodes")
	val nodes: Array<WorldMapNode>,

	/**
	 * All the edges (paths between nodes) on this world map
	 */
	@BitField(id = 5)
	val edges: Array<WorldMapEdge>,
) {

	constructor() : this(
		UUID.randomUUID(), "", BcSprite(),
		ConstantTimelineExpression(TimelineOptionalStringValue(null)),
		arrayOf(), arrayOf(),
	)
}
