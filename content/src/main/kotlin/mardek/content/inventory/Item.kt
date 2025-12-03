package mardek.content.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.content.stats.CombatStat
import mardek.content.stats.Element
import mardek.content.sprite.KimSprite
import java.util.*

/**
 * This class defines the properties that `Item`s can have (e.g. name, description, element, etc...). All `Item`s are
 * part of the `Content`. This is in contrast to `ItemStack`s, which are tuples `(Item, amount)`, and are used as
 * building block for inventories. The same `Item` can be used in many `ItemStack`s.
 */
@BitStruct(backwardCompatible = true)
class Item(
	/**
	 * The display name of the item, as imported from Flash.
	 */
	@BitField(id = 0)
	val flashName: String,

	/**
	 * The description of the item.
	 */
	@BitField(id = 1)
	val description: String,

	/**
	 * The type/category of the item. This is displayed in the inventory UI, and determines whether the item can be
	 * stacked.
	 */
	@BitField(id = 2)
	@ReferenceField(stable = false, label = "item types")
	val type: ItemType,

	/**
	 * The element of the item, or `null` for boring items without element.
	 */
	@BitField(id = 3, optional = true)
	@ReferenceField(stable = false, label = "elements")
	val element: Element?,

	/**
	 * The price/cost of the item. It costs this amount to buy the item in a store. The player can sell the item for
	 * half this amount.
	 */
	@BitField(id = 4, optional = true)
	@IntegerField(expectUniform = false, minValue = 0, commonValues=[1000, 5000, 10000, 500])
	val cost: Int?,

	/**
	 * This field will be non-null if and only if this item can be equipped. If so, it specifies the properties that
	 * only equippable items have.
	 */
	@BitField(id = 5, optional = true)
	val equipment: EquipmentProperties?,

	/**
	 * This field will be non-null if and only if this item can be consumed (e.g. potions). If so, it specifies the
	 * properties that only consumable items have.
	 */
	@BitField(id = 6, optional = true)
	val consumable: ConsumableProperties?,

	/**
	 * The unique ID of the item, which is used for (de)serialization.
	 */
	@BitField(id = 7)
	@StableReferenceFieldId
	val id: UUID,
) {

	/**
	 * The inventory sprite of the item
	 */
	@BitField(id = 8)
	lateinit var sprite: KimSprite

	constructor() : this(
			"", "", ItemType(), null,
			0, null, null, UUID.randomUUID(),
	)

	override fun toString() = flashName

	/**
	 * If this item is not equippable, this method returns 0.
	 *
	 * Otherwise, if someone were to equip this item, this method compute by how much the given `stat` of that person
	 * would be increased. This is mostly used in the UI, e.g. to predict how much the ATK of a player would be
	 * increased by equipping this weapon.
	 */
	fun getModifier(stat: CombatStat) = equipment?.getStat(stat) ?: 0

	override fun hashCode() = id.hashCode()

	override fun equals(other: Any?) = other is Item && id == other.id
}
