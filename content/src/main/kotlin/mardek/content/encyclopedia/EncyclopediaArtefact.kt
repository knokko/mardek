package mardek.content.encyclopedia

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.content.animation.StandaloneAnimation
import mardek.content.expression.ConstantStateExpression
import mardek.content.expression.ExpressionBooleanValue
import mardek.content.expression.StateExpression
import mardek.content.stats.Element
import java.util.UUID

/**
 * Represents an entry of the "Artefacts" section of the "Encyclopedia" tab (of the in-game menu).
 */
@BitStruct(backwardCompatible = true)
class EncyclopediaArtefact(

	/**
	 * The unique ID of this artefact entry, which is used for (de)serialization
	 */
	@BitField(id = 0)
	@StableReferenceFieldId
	val id: UUID,

	/**
	 * The name of this artefact, which appears in the artefacts list
	 */
	@BitField(id = 1)
	val name: String,

	/**
	 * The description of this artefact, which is shown when this entry is opened
	 */
	@BitField(id = 2)
	val description: String,

	/**
	 * The element of this artefact, which is shown on the left of [name] in the artefacts list
	 */
	@BitField(id = 3)
	@ReferenceField(stable = false, label = "elements")
	val element: Element,

	/**
	 * The 'animation' (usually just 1 sprite) of this artefact, which is shown when this entry is opened
	 */
	@BitField(id = 4)
	val animation: StandaloneAnimation,

	/**
	 * The 'magic scale' of [animation]. When the textures of this artefact are imported from Flash, their width
	 * and height are multiplied by `magicScale`. This is needed because the flash textures are SVGs, which are
	 * converted to PNGs because this engine cannot handle SVGs. Using a larger `magicScale` will give a higher-quality
	 * texture, but also requires more disk space and (video) memory.
	 *
	 * We need to remember this magic scale because the renderer needs it to interpret some transformations correctly.
	 */
	@BitField(id = 5)
	@IntegerField(expectUniform = false, minValue = 1)
	val magicScale: Int,

	/**
	 * Whether this entry should show up in the encyclopedia:
	 * - When this evaluates to `false`, this entry is completely hidden, and does *not* even claim any space in the
	 * encyclopedia page at all.
	 * - When this evaluates to `true`, this entry will claim space in the encyclopedia.
	 * However, the name and information about the artefact are not shown until the player has found the artefact,
	 * or has e.g. read about the artefact in some book.
	 *
	 * This expression is used to hide artefacts that the player can only learn about in later chapters. For instance:
	 * - The `shouldShowUp` of the Fire Crystal should always evaluate to `true`,
	 * since the player can read about it in a book at Deugan's house.
	 * - The `shouldShowUp` of the Ether Crystal should only evaluate to `true` in chapter 3, since the player can
	 * only read about it in a book located in Aeropolis.
	 */
	@BitField(id = 6)
	@ClassField(root = StateExpression::class)
	val shouldShowUp: StateExpression<Boolean>,
) {

	internal constructor() : this(
		UUID(0, 0), "", "", Element(), StandaloneAnimation(),
		1, ConstantStateExpression(ExpressionBooleanValue(true)),
	)
}
