package mardek.content.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField

@BitStruct(backwardCompatible = true)
class ItemStack(
	@BitField(id = 0)
	@ReferenceField(stable = true, label = "items")
	val item: Item,

	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 1)
	val amount: Int
) {
	init {
		if (amount <= 0) throw IllegalArgumentException("Amount $amount must be positive")
	}

	@Suppress("unused")
	private constructor() : this(Item(), 1)

	override fun toString() = if (amount == 1) item.toString() else "$item x$amount"

	override fun equals(other: Any?) = other is ItemStack && item === other.item && amount == other.amount

	override fun hashCode() = item.hashCode() - 17 * amount
}
