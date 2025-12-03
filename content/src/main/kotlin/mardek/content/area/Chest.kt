package mardek.content.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.content.battle.PartyLayout
import mardek.content.inventory.Dreamstone
import mardek.content.inventory.ItemStack
import mardek.content.inventory.PlotItem
import java.util.*

/**
 * Represents a specific chest in a specific area
 */
@BitStruct(backwardCompatible = true)
class Chest(

	/**
	 * The X-coordinate of the tile containing this chest
	 */
	@BitField(id = 0)
	@IntegerField(expectUniform = false, minValue = 0, digitSize = 2)
	val x: Int,

	/**
	 * The Y-coordinate of the tile containing this chest
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 0, digitSize = 2)
	val y: Int,

	/**
	 * The sprites of this chest
	 */
	@BitField(id = 2)
	@ReferenceField(stable = false, label = "chest sprites")
	val sprite: ChestSprite,

	/**
	 * The amount of gold that is in the chest. Must be 0 if the chest has an item or dreamstone
	 */
	@BitField(id = 3)
	@IntegerField(expectUniform = false, minValue = 0)
	val gold: Int,

	/**
	 * The item that is in the chest. Must be `null` if the gold is non-zero or the chest has a plot item, or dreamstone
	 */
	@BitField(id = 4, optional = true)
	val stack: ItemStack?,

	/**
	 * The plot item that is in the chest. Must be `null` if the gold is non-zero, or if the chest contains an item
	 * stack, or if it contains a plot item
	 */
	@BitField(id = 5, optional = true)
	val plotItem: PlotItem?,

	/**
	 * The dreamstone that is in the chest. Must be `null` if the gold is non-zero, or if the chest contains an item
	 * or plot item.
	 */
	@BitField(id = 6, optional = true)
	val dreamstone: Dreamstone?,

	/**
	 * When this field is non-null, the player must win a battle before looting the chest.
	 */
	@BitField(id = 7, optional = true)
	val battle: ChestBattle?,

	/**
	 * When this field is true, the chest is hidden/invisible
	 */
	@BitField(id = 8)
	val hidden: Boolean,

	/**
	 * The unique ID of the chest, which is used for (de)serialization
	 */
	@BitField(id = 9)
	@StableReferenceFieldId
	val id: UUID,
) {

	@Suppress("unused")
	private constructor() : this(
		0, 0, ChestSprite(), 0, null,
		null, null, null, false, UUID.randomUUID(),
	)

	override fun toString() = "Chest(x=$x, y=$y, gold=$gold, item=${stack ?: plotItem ?: dreamstone})"
}

/**
 * Represents a battle that the player must win to loot a chest
 */
@BitStruct(backwardCompatible = true)
class ChestBattle(

	/**
	 * The monsters in the chest. It must be an array of length 4: 1 array element per enemy layout position. Note that
	 * this array can contain null values.
	 */
	@BitField(id = 0)
	@NestedFieldSetting(path = "", sizeField = IntegerField(expectUniform = true, minValue = 4, maxValue = 4))
	@NestedFieldSetting(path = "c", optional = true)
	val monsters: Array<ChestMonster?>,

	/**
	 * The party layout of the enemies/monsters
	 */
	@BitField(id = 1)
	@ReferenceField(stable = false, label = "enemy party layouts")
	val enemyLayout: PartyLayout,

	/**
	 * When this is null, the default battle music should be played during the battle. When non-null, the mustic track
	 * with the given name should be played instead.
	 */
	@BitField(id = 2, optional = true)
	val specialMusic: String?,
) {
	@Suppress("unused")
	private constructor() : this(arrayOf(null, null, null, null), PartyLayout(), null)

	override fun toString() = "ChestBattle$monsters"
}

/**
 * Represents a monster in a chest.
 */
@BitStruct(backwardCompatible = true)
class ChestMonster(

	/**
	 * The raw name of the monster in the chest
	 */
	@BitField(id = 0)
	val name1: String,

	/**
	 * An optional field to override the display name that the monster will have in the battle
	 */
	@BitField(id = 1, optional = true)
	val name2: String?,

	/**
	 * The level of the monster
	 */
	@BitField(id = 2)
	@IntegerField(expectUniform = false, minValue = 1)
	val level: Int,
) {

	@Suppress("unused")
	private constructor() : this("", null, 0)

	override fun toString() = "($name1, level=$level)"
}
