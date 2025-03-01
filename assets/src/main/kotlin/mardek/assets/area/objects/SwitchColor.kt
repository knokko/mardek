package mardek.assets.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.assets.sprite.KimSprite
import java.util.*

@BitStruct(backwardCompatible = true)
class SwitchColor(
	@BitField(id = 0)
	val name: String,

	@BitField(id = 1)
	val offSprite: KimSprite,

	@BitField(id = 2)
	val onSprite: KimSprite,

	@BitField(id = 3)
	val gateSprite: KimSprite,

	@BitField(id = 4)
	val platformSprite: KimSprite,
) {

	@BitField(id = 5)
	@StableReferenceFieldId
	val id = UUID.randomUUID()!!

	internal constructor() : this("", KimSprite(), KimSprite(), KimSprite(), KimSprite())
}
