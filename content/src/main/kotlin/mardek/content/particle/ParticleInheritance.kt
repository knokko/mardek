package mardek.content.particle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField

@BitStruct(backwardCompatible = true)
class ParticleInheritance(

	@BitField(id = 0)
	@ReferenceField(stable = false, label = "particles")
	val parent: ParticleEffect,

	@BitField(id = 1)
	@NestedFieldSetting(path = "", optional = true)
	@ReferenceField(stable = false, label = "particle sprites")
	val overrideSprites: Array<ParticleSprite>?,
) {
	@Suppress("unused")
	private constructor() : this(ParticleEffect(), null)
}
