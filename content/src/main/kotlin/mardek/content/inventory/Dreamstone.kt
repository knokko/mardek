package mardek.content.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.StableReferenceFieldId
import java.util.*

/**
 * Represents a dreamstone: a special kind of plot item that the player can use to view the thoughts of an important
 * character in the story.
 */
@BitStruct(backwardCompatible = true)
class Dreamstone(

	/**
	 * The index/ordering of the dreamstone, starting from **1**. The index determines the order in which the
	 * dreamstones appear in the encyclopedia.
	 */
	@BitField(id = 0)
	@IntegerField(minValue = 0, expectUniform = false)
	val index: Int,

	/**
	 * The unique ID of the dreamstone, which is use for (de)serialization.
	 */
	@BitField(id = 1)
	@StableReferenceFieldId
	val id: UUID,
) {

	@Suppress("unused")
	private constructor() : this(-1, UUID.randomUUID())

	override fun toString() = "DreamStone $index"
}
