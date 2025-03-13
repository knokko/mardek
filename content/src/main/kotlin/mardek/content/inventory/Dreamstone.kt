package mardek.content.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.StableReferenceFieldId
import java.util.*

@BitStruct(backwardCompatible = true)
class Dreamstone(
	@BitField(id = 0)
	@IntegerField(minValue = 0, expectUniform = false)
	val index: Int
) {

	@BitField(id = 1)
	@StableReferenceFieldId
	val id = UUID.randomUUID()!!

	@Suppress("unused")
	private constructor() : this(-1)
}
