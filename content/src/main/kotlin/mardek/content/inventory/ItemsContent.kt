package mardek.content.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceFieldTarget
import java.io.Serializable

/**
 * The item-related part of the `Content`
 */
@BitStruct(backwardCompatible = true)
class ItemsContent(

	/**
	 * All the possible item types, e.g. WEAPON and CONSUMABLE
	 */
	@BitField(id = 0)
	@ReferenceFieldTarget(label = "item types")
	val itemTypes: ArrayList<ItemType>,

	/**
	 * All the possible items, e.g. Champion Sword and Potion
	 */
	@BitField(id = 1)
	@ReferenceFieldTarget(label = "items")
	val items: ArrayList<Item>,

	/**
	 * All the plot items, e.g. Trilobite Key I
	 */
	@BitField(id = 2)
	@ReferenceFieldTarget(label = "plot items")
	val plotItems: ArrayList<PlotItem>,

	/**
	 * All the dreamstones
	 */
	@BitField(id = 3)
	@ReferenceFieldTarget(label = "dreamstones")
	val dreamstones: ArrayList<Dreamstone>,
) : Serializable {
	constructor() : this(
		ArrayList(0), ArrayList(0),
		ArrayList(0), ArrayList(0)
	)
}
