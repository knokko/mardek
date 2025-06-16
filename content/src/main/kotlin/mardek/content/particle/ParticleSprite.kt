package mardek.content.particle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import mardek.content.sprite.BcSprite

@BitStruct(backwardCompatible = true)
class ParticleSprite(
	@BitField(id = 0)
	val name: String,

	@BitField(id = 1)
	val sprite: BcSprite,
) {
	internal constructor() : this("", BcSprite())
}
