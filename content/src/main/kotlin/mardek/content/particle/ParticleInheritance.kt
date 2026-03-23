package mardek.content.particle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField

/**
 * When a particle effect uses inheritance, this class specifies the parent particle effect, as well as the
 * textures/sprites that override the sprites of the parent effect.
 */
@BitStruct(backwardCompatible = true)
class ParticleInheritance(

	/**
	 * The parent particle effect, which from this particle effect will inherit most of its properties
	 */
	@BitField(id = 0)
	@ReferenceField(stable = false, label = "particles")
	val parent: ParticleEffect,

	/**
	 * The textures/sprites of **this** particle effect, or `null` to use the parent sprites
	 */
	@BitField(id = 1)
	@NestedFieldSetting(path = "", optional = true)
	@ReferenceField(stable = false, label = "particle sprites")
	val overrideSprites: Array<ParticleSprite>?,
) {
	@Suppress("unused")
	private constructor() : this(ParticleEffect(), null)
}
