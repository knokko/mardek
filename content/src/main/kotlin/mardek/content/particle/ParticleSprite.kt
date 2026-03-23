package mardek.content.particle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import mardek.content.sprite.BcSprite

/**
 * The (possibly shared) sprite of a particle. It is just a sprite with an associated name.
 */
@BitStruct(backwardCompatible = true)
class ParticleSprite(

	/**
	 * The name of the sprite, which is only used for debugging and editing
	 */
	@BitField(id = 0)
	val name: String,

	/**
	 * The actual sprite
	 */
	@BitField(id = 1)
	val sprite: BcSprite,
) {
	internal constructor() : this("", BcSprite())

	override fun toString() = name
}
