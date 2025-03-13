package mardek.content.animations

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceField

@BitStruct(backwardCompatible = true)
class BattleModel(

	@BitField(id = 0)
	@ReferenceField(stable = false, label = "skeletons")
	val skeleton: Skeleton,

	@BitField(id = 1, optional = true)
	val skin: String?,
) {

	constructor() : this(Skeleton(), null)

	override fun toString() = skin ?: "no skin"
}

