package mardek.content.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.content.animation.AnimationNode
import java.util.*

/**
 * Represents a background used by battles. The background will be drawn behind the combatants.
 */
@BitStruct(backwardCompatible = true)
class BattleBackground(

	/**
	 * The name of the background, as imported from Flash. It serves no in-game purpose, but is potentially useful
	 * for debugging and editing.
	 */
	@BitField(id = 0)
	val name: String,

	/**
	 * The content of the background. These are the actual animation nodes that will be drawn behind the combatants.
	 */
	@BitField(id = 1)
	val nodes: Array<AnimationNode>,

	/**
	 * The 'magic scale' of this background. When the textures of this background are imported from Flash, their width
	 * and height are multiplied by `magicScale`. This is needed because the flash textures are SVGs, which are
	 * converted to PNGs because this engine cannot handle SVGs. Using a larger `magicScale` will give a higher-quality
	 * texture, but also requires more disk space and (video) memory.
	 *
	 * We need to remember this magic scale because the renderer needs it to interpret some transformations correctly.
	 */
	@BitField(id = 2)
	@IntegerField(expectUniform = false, minValue = 1)
	val magicScale: Int,

	/**
	 * The unique ID of this background, which is used for (de)serialization
	 */
	@BitField(id = 3)
	@StableReferenceFieldId
	val id: UUID,
) {

	constructor() : this("", emptyArray<AnimationNode>(), 1, UUID.randomUUID())

	override fun toString() = "BattleBackground($name)"
}
