package mardek.state.ingame.area.loot

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.inventory.Dreamstone
import mardek.content.inventory.ItemStack
import mardek.content.inventory.PlotItem

@BitStruct(backwardCompatible = true)
class BattleLoot(
	@BitField(id = 0)
	@IntegerField(expectUniform = false, minValue = 0)
	val gold: Int,

	@BitField(id = 1)
	val items: ArrayList<ItemStack>,

	@BitField(id = 2)
	@ReferenceField(stable = true, label = "plot items")
	val plotItems: ArrayList<PlotItem>,

	@BitField(id = 3)
	@ReferenceField(stable = true, label = "dreamstones")
	val dreamStones: ArrayList<Dreamstone>,
) {
	@Suppress("unused")
	private constructor() : this(
		0, ArrayList(0),
		ArrayList(0), ArrayList(0)
	)
}
