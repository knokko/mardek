package mardek.assets.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.assets.inventory.Dreamstone
import mardek.assets.inventory.ItemStack
import mardek.assets.inventory.PlotItem
import java.util.*

@BitStruct(backwardCompatible = false)
class Chest(
	@BitField(ordering = 0)
	@IntegerField(expectUniform = false, minValue = 0)
	val x: Int,

	@BitField(ordering = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	val y: Int,

	@BitField(ordering = 2)
	@ReferenceField(stable = false, label = "chest sprites")
	val sprite: ChestSprite,

	@BitField(ordering = 3)
	@IntegerField(expectUniform = false, minValue = 0)
	val gold: Int,

	@BitField(ordering = 4, optional = true)
	val stack: ItemStack?,

	@BitField(ordering = 5, optional = true)
	val plotItem: PlotItem?,

	@BitField(ordering = 6, optional = true)
	val dreamstone: Dreamstone?,

	@BitField(ordering = 7, optional = true)
	val battle: ChestBattle?,

	@BitField(ordering = 8)
	val hidden: Boolean,
) {

	@BitField(ordering = 9)
	@StableReferenceFieldId
	val id = UUID.randomUUID()!!

	@Suppress("unused")
	private constructor() : this(
		0, 0, ChestSprite(), 0, null,
		null, null, null, false
	)

	override fun toString() = "Chest(x=$x, y=$y, gold=$gold, item=${stack ?: plotItem ?: dreamstone})"
}

@BitStruct(backwardCompatible = false)
class ChestBattle(
	@BitField(ordering = 0)
	@NestedFieldSetting(path = "", sizeField = IntegerField(expectUniform = true, minValue = 4, maxValue = 4))
	@NestedFieldSetting(path = "c", optional = true)
	val monsters: Array<ChestMonster?>,

	@BitField(ordering = 1)
	val position: String,

	@BitField(ordering = 2, optional = true)
	val specialMusic: String?,
) {
	@Suppress("unused")
	private constructor() : this(arrayOf(null, null, null, null), "", null)

	override fun toString() = "ChestBattle$monsters"
}

@BitStruct(backwardCompatible = false)
class ChestMonster(
	@BitField(ordering = 0)
	val name1: String,

	@BitField(ordering = 1, optional = true)
	val name2: String?,

	@BitField(ordering = 2)
	@IntegerField(expectUniform = false, minValue = 1)
	val level: Int,
) {

	@Suppress("unused")
	private constructor() : this("", null, 0)

	override fun toString() = "($name1, level=$level)"
}
