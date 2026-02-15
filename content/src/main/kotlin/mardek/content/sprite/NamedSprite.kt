package mardek.content.sprite

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.StableReferenceFieldId
import java.util.UUID

/**
 * This class is just a [BcSprite] with an associated name and ID
 */
@BitStruct(backwardCompatible = true)
class NamedSprite(

	/**
	 * The unique ID, which can be used for (de)serialization
	 */
	@BitField(id = 0)
	@StableReferenceFieldId
	val id: UUID,

	/**
	 * The name
	 */
	@BitField(id = 1)
	val name: String,

	/**
	 * The sprite
	 */
	@BitField(id = 2)
	val sprite: BcSprite,
) {

	@Suppress("unused")
	private constructor() : this(UUID(0, 0), "", BcSprite())

	override fun toString() = "NamedSprite($name)"
}
