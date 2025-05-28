package mardek.content.particle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.ReferenceField

@BitStruct(backwardCompatible = true)
class ParticleInheritance(

	@ReferenceField(stable = false, label = "particles")
	val parent: ParticleEffect,

	@ReferenceField(stable = false, label = "particle sprites")
	val overrideSprites: Array<ParticleSprite>?,
) {
}
