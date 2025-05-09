package mardek.content.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.BITSER
import mardek.content.inventory.Item
import mardek.content.inventory.PlotItem
import kotlin.collections.ArrayList
import kotlin.random.Random

@BitStruct(backwardCompatible = true)
class PotentialItem(

	@BitField(id = 0, optional = true)
	@ReferenceField(stable = false, label = "items")
	val item: Item?,

	@BitField(id = 1)
	@IntegerField(expectUniform = true, minValue = 1, maxValue = 100)
	val chance: Int,
) {

	constructor() : this(null, 100)

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)

	override fun toString() = "$chance% $item"
}

@BitStruct(backwardCompatible = true)
class PotentialEquipment(

	@BitField(id = 0)
	val entries: ArrayList<PotentialItem>
) {

	init {
		if (entries.sumOf { it.chance } != 100) throw IllegalArgumentException("Sum of chances must be 100 $this")
	}

	constructor() : this(arrayListOf(PotentialItem()))

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)

	override fun toString() = "Equipment($entries)"

	fun pick(): Item? {
		val total = entries.sumOf { it.chance }
		val selected = Random.Default.nextInt(total)

		var current = 0
		for (candidate in entries) {
			if (current + candidate.chance >= selected) return candidate.item
			current += candidate.chance
		}

		throw Error("Should not happen")
	}

	companion object {
		val EMPTY = PotentialEquipment(arrayListOf(PotentialItem(null, 100)))
	}
}

@BitStruct(backwardCompatible = true)
class PotentialPlotItem(
	@BitField(id = 0)
	@ReferenceField(stable = false, label = "plot items")
	val item: PlotItem,

	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 0, maxValue = 100)
	val chance: Int,
) {
	@Suppress("unused")
	private constructor() : this(PlotItem(), 0)

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)

	override fun toString() = "$chance% $item"
}
