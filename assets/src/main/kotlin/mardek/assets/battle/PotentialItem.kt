package mardek.assets.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.assets.inventory.Item
import java.util.*
import kotlin.collections.ArrayList

@BitStruct(backwardCompatible = false)
class PotentialItem(

	@BitField(ordering = 0, optional = true)
	@ReferenceField(stable = false, label = "items")
	val item: Item?,

	@BitField(ordering = 1)
	@IntegerField(expectUniform = true, minValue = 1, maxValue = 100)
	val chance: Int,
) {

	constructor() : this(null, 100)

	override fun equals(other: Any?) = other is PotentialItem && this.item === other.item && this.chance == other.chance

	override fun hashCode() = chance + 31 * Objects.hashCode(item)

	override fun toString() = "$chance% $item"
}

@BitStruct(backwardCompatible = false)
class PotentialEquipment(

	@BitField(ordering = 0)
	val entries: ArrayList<PotentialItem>
) {

	init {
		if (entries.sumOf { it.chance } != 100) throw IllegalArgumentException("Sum of chances must be 100 $this")
	}

	constructor() : this(arrayListOf(PotentialItem()))

	override fun equals(other: Any?) = other is PotentialEquipment && this.entries.toSet() == other.entries.toSet()

	override fun hashCode() = entries.hashCode()

	override fun toString() = "Equipment($entries)"

	companion object {
		val EMPTY = PotentialEquipment(arrayListOf(PotentialItem(null, 100)))
	}
}
