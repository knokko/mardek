package mardek.assets.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.assets.combat.Element
import mardek.assets.skill.Skill

@BitStruct(backwardCompatible = false)
class Item(
	@BitField(ordering = 0)
	val flashName: String,

	@BitField(ordering = 0)
	val description: String,

	@BitField(ordering = 1)
	@ReferenceField(stable = false, label = "item types")
	val type: ItemType,

	@BitField(ordering = 0)
	@ReferenceField(stable = false, label = "elements")
	val element: Element,

	@BitField(ordering = 3)
	@IntegerField(expectUniform = false, minValue = 0)
	val cost: Int,

	@BitField(ordering = 0)
	@ReferenceField(stable = false, label = "skills")
	val skills: ArrayList<Skill>,

	@BitField(ordering = 2, optional = true)
	val weapon: WeaponProperties?,

	// TODO Armor properties

	@BitField(ordering = 0) // TODO Turn into reference
	val onlyUser: String,

	// TODO effects, HP_DRAIN, stfx
) {

}
