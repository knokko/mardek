package mardek.content.stats

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import mardek.content.sprite.KimSprite

@BitStruct(backwardCompatible = true)
class CreatureType(
	@BitField(id = 0)
	val flashName: String,

	@BitField(id = 1)
	val icon: KimSprite,

	@BitField(id = 2)
	val revertsHealing: Boolean,
) {

	constructor() : this("", KimSprite(), false)

	override fun toString() = flashName
}
