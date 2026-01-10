package mardek.content.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.BITSER

/**
 * Represents a stack of an `Item`, which is a tuple `(Item, amount)`, e.g. 4 potions.
 */
@BitStruct(backwardCompatible = true)
class ItemStack(

	/**
	 * The item
	 */
	@BitField(id = 0)
	@ReferenceField(stable = true, label = "items")
	val item: Item,

	/**
	 * The stack size/amount. This must be positive.
	 */
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

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)

	/**
	 * Returns an `ItemStack` with the same `item` as this stack, but whose amount is `this.amount - 1`. When
	 * `this.amount == 1`, this method returns `null` instead.
	 */
	fun decremented() = if (amount > 1) ItemStack(item, amount - 1) else null

	/**
	 * Returns an `ItemStack` with the same `item` as this stack, but whose amount is `this.amount + 1`.
	 */
	fun incremented() = ItemStack(item, amount + 1)
}
