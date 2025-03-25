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

@BitStruct(backwardCompatible = true)
class Item(
	@BitField(id = 0)
	val flashName: String,

	@BitField(id = 1)
	val description: String,

	@BitField(id = 2)
	@ReferenceField(stable = false, label = "item types")
	val type: ItemType,

	@BitField(id = 3, optional = true)
	@ReferenceField(stable = false, label = "elements")
	val element: Element?,

	@BitField(id = 4, optional = true)
	@IntegerField(expectUniform = false, minValue = 0)
	val cost: Int?,

	@BitField(id = 5, optional = true)
	val equipment: EquipmentProperties?,

	@BitField(id = 6, optional = true)
	val consumable: ConsumableProperties?,
) {

	@BitField(id = 7)
	lateinit var sprite: KimSprite

	@BitField(id = 8)
	@StableReferenceFieldId
	val id = UUID.randomUUID()!!

	constructor() : this(
			"", "", ItemType(), null,
			0, null, null
	)

	override fun toString() = flashName

	fun getModifier(stat: CombatStat): Int {
		var total = 0
		if (equipment != null) {
			for (modifier in equipment.stats) {
				if (modifier.stat == stat) total += modifier.adder
			}
		}
		return total
	}
}
