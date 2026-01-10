package mardek.content.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.stats.Element
import mardek.content.sprite.KimSprite

/**
 * Represents a plot item: a special item that is somehow relevant for the plot/story. When players obtain plot items,
 * they do **not** show up in the inventory. Instead, players can see them in a designated tab in the in-game menu.
 */
@BitStruct(backwardCompatible = true)
class PlotItem(

	/**
	 * The display name of the plot item, which is shown when the player acquires (or loses) the plot item. Furthermore,
	 * this name is shown in the Plot Items tab of the in-game menu.
	 */
	@BitField(id = 0)
	val displayName: String,

	/**
	 * The sprite/image of the item, which is shown when the player acquires (or loses) the plot item. Furthermore,
	 * this sprite is shown in the Plot Items tab of the in-game menu.
	 */
	@BitField(id = 1)
	val sprite: KimSprite,

	/**
	 * The description of the plot item, which the player can read in the Plot Items tab of the in-game menu.
	 */
	@BitField(id = 2)
	val description: String,

	/**
	 * The element of the plot item. This element influences the styling in the PLot Items tab, but doesn't really do
	 * anything.
	 */
	@BitField(id = 3, optional = true)
	@ReferenceField(stable = false, label = "elements")
	val element: Element?,

	/**
	 * The cost/price of the plot item. This is only relevant for plot items that the player can buy.
	 */
	@BitField(id = 4, optional = true)
	@IntegerField(expectUniform = false, minValue = 0)
	val cost: Int?,
) {

	constructor() : this("", KimSprite(), "", null, null)

	override fun toString() = displayName
}
