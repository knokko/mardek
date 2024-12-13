package mardek.assets.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.assets.sprite.KimSprite
import java.util.*

@BitStruct(backwardCompatible = false)
class SwitchColor(
	@BitField(ordering = 0)
	val name: String,

	@BitField(ordering = 1)
	val offSprite: KimSprite,

	@BitField(ordering = 2)
	val onSprite: KimSprite,

	@BitField(ordering = 3)
	val gateSprite: KimSprite,

	@BitField(ordering = 4)
	val platformSprite: KimSprite,
) {

	@BitField(ordering = 5)
	@StableReferenceFieldId
	val id = UUID.randomUUID()!!

	internal constructor() : this("", KimSprite(), KimSprite(), KimSprite(), KimSprite())
}
