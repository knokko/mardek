package mardek.assets.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.assets.battle.PartyLayout
import mardek.assets.inventory.Dreamstone
import mardek.assets.inventory.ItemStack
import mardek.assets.inventory.PlotItem
import java.util.*

@BitStruct(backwardCompatible = true)
class Chest(
	@BitField(id = 0)
	@IntegerField(expectUniform = false, minValue = 0)
	val x: Int,

	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	val y: Int,

	@BitField(id = 2)
	@ReferenceField(stable = false, label = "chest sprites")
	val sprite: ChestSprite,

	@BitField(id = 3)
	@IntegerField(expectUniform = false, minValue = 0)
	val gold: Int,

	@BitField(id = 4, optional = true)
	val stack: ItemStack?,

	@BitField(id = 5, optional = true)
	val plotItem: PlotItem?,

	@BitField(id = 6, optional = true)
	val dreamstone: Dreamstone?,

	@BitField(id = 7, optional = true)
	val battle: ChestBattle?,

	@BitField(id = 8)
	val hidden: Boolean,
) {

	@BitField(id = 9)
	@StableReferenceFieldId
	val id = UUID.randomUUID()!!

	@Suppress("unused")
	private constructor() : this(
		0, 0, ChestSprite(), 0, null,
		null, null, null, false
	)

	override fun toString() = "Chest(x=$x, y=$y, gold=$gold, item=${stack ?: plotItem ?: dreamstone})"
}

@BitStruct(backwardCompatible = true)
class ChestBattle(
	@BitField(id = 0)
	@NestedFieldSetting(path = "", sizeField = IntegerField(expectUniform = true, minValue = 4, maxValue = 4))
	@NestedFieldSetting(path = "c", optional = true)
	val monsters: Array<ChestMonster?>,

	@BitField(id = 1)
	@ReferenceField(stable = false, label = "enemy party layouts")
	val enemyLayout: PartyLayout,

	@BitField(id = 2, optional = true)
	val specialMusic: String?,
) {
	@Suppress("unused")
	private constructor() : this(arrayOf(null, null, null, null), PartyLayout(), null)

	override fun toString() = "ChestBattle$monsters"
}

@BitStruct(backwardCompatible = true)
class ChestMonster(
	@BitField(id = 0)
	val name1: String,

	@BitField(id = 1, optional = true)
	val name2: String?,

	@BitField(id = 2)
	@IntegerField(expectUniform = false, minValue = 1)
	val level: Int,
) {

	@Suppress("unused")
	private constructor() : this("", null, 0)

	override fun toString() = "($name1, level=$level)"
}
