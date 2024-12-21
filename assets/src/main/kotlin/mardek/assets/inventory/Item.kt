package mardek.assets.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.assets.combat.CombatStat
import mardek.assets.combat.Element
import mardek.assets.sprite.KimSprite
import java.util.*

@BitStruct(backwardCompatible = false)
class Item(
	@BitField(ordering = 0)
	val flashName: String,

	@BitField(ordering = 1)
	val description: String,

	@BitField(ordering = 2)
	@ReferenceField(stable = false, label = "item types")
	val type: ItemType,

	@BitField(ordering = 3, optional = true)
	@ReferenceField(stable = false, label = "elements")
	val element: Element?,

	@BitField(ordering = 4, optional = true)
	@IntegerField(expectUniform = false, minValue = 0)
	val cost: Int?,

	@BitField(ordering = 5, optional = true)
	val equipment: EquipmentProperties?,

	@BitField(ordering = 6, optional = true)
	val consumable: ConsumableProperties?,
) {

	@BitField(ordering = 7)
	lateinit var sprite: KimSprite

	@BitField(ordering = 8)
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
