package mardek.assets.animations

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceField

@BitStruct(backwardCompatible = false)
class BattleModel(

	@BitField(ordering = 0)
	@ReferenceField(stable = false, label = "skeletons")
	val skeleton: Skeleton,

	@BitField(ordering = 1, optional = true)
	val skin: String?,
) {

	constructor() : this(Skeleton(), null)

	override fun toString() = skin ?: "no skin"
}

