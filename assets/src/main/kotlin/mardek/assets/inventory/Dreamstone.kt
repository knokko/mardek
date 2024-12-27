package mardek.assets.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.StableReferenceFieldId
import java.util.*

@BitStruct(backwardCompatible = false)
class Dreamstone(
	@BitField(ordering = 0)
	@IntegerField(minValue = 0, expectUniform = false)
	val index: Int
) {

	@BitField(ordering = 1)
	@StableReferenceFieldId
	val id = UUID.randomUUID()!!

	@Suppress("unused")
	private constructor() : this(-1)
}
