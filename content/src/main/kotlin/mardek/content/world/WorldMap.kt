package mardek.content.world

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.ReferenceFieldTarget
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.content.audio.MusicTrack
import mardek.content.encyclopedia.EncyclopediaArea
import mardek.content.sprite.BcSprite
import mardek.content.expression.ConstantStateExpression
import mardek.content.expression.ExpressionMusicTrackValue
import mardek.content.expression.StateExpression
import java.io.Serializable
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
	 * The music track that should be played while the player is on this world map
	 */
	@BitField(id = 3)
	@ClassField(root = StateExpression::class)
	val music: StateExpression<MusicTrack?>,

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

	/**
	 * The encyclopedia entry that should be discovered when this world map is visited for the first time
	 */
	@BitField(id = 6)
	@ReferenceField(stable = false, label = "encyclopedia places")
	val encyclopediaEntry: EncyclopediaArea,
) : Serializable {

	constructor() : this(
		UUID.randomUUID(), "", BcSprite(),
		ConstantStateExpression(ExpressionMusicTrackValue(null)),
		arrayOf(), arrayOf(), EncyclopediaArea(),
	)
}
