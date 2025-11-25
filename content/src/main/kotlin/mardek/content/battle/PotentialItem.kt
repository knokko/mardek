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

/**
 * A tuple (item, chance). This is used to specify the loot that monsters can drop, as well as the equipment that
 * monsters may equip.
 */
@BitStruct(backwardCompatible = true)
class PotentialItem(

	/**
	 * The item, which can be `null` in `PotentialEquipment`, which would indicate a `chance` percentage that the
	 * monster does not carry any equipment in the corresponding equipment slot.
	 */
	@BitField(id = 0, optional = true)
	@ReferenceField(stable = false, label = "items")
	val item: Item?,

	/**
	 * The chance to get `item`, in percentages
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = true, minValue = 1, maxValue = 100)
	val chance: Int,
) {

	constructor() : this(null, 100)

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)

	override fun toString() = "$chance% $item"
}

/**
 * The potential equipment that a `Monster` can have in a given slot.
 */
@BitStruct(backwardCompatible = true)
class PotentialEquipment(

	/**
	 * The potential items that the `Monster` can get. The sum of their `chance` must be 100%.
	 */
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

	/**
	 * (Randomly) picks an item from this `PotentialEquipment`, using the `chance`s of each `PotentialItem` in
	 * `entries`.
	 */
	fun pick(): Item? {
		val total = entries.sumOf { it.chance }
		val selected = Random.nextInt(total)

		var current = 0
		for (candidate in entries) {
			if (current + candidate.chance >= selected) return candidate.item
			current += candidate.chance
		}

		throw Error("Should not happen")
	}

	companion object {

		/**
		 * A `PotentialEquipment` whose `pick()` method will always return `null`. When a monster has this
		 * `PotentialEquipment` for e.g. its shield equipment slot, the monster will never carry a shield.
		 */
		val EMPTY = PotentialEquipment(arrayListOf(PotentialItem(null, 100)))
	}
}

/**
 * Represents a `PlotItem` that a monster may drop after it is slain.
 */
@BitStruct(backwardCompatible = true)
class PotentialPlotItem(

	/**
	 * The `PlotItem` that may be dropped.
	 */
	@BitField(id = 0)
	@ReferenceField(stable = false, label = "plot items")
	val item: PlotItem,

	/**
	 * The chance that `item` is dropped. This is usually 100% chance, but there are exceptions (e.g. the zombies in
	 * Canonia may or may not drop a Trilobite Key).
	 */
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
