package mardek.assets.combat

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.StringField

@BitStruct(backwardCompatible = false)
class Element(
	@BitField(ordering = 0, optional = true)
	val rawName: String,

	@BitField(ordering = 1, optional = true)
	@ReferenceField(stable = false, label = "stats")
	val bonusStat: CombatStat?,

	@BitField(ordering = 2)
	@StringField(length = IntegerField(expectUniform = true, minValue = 1, maxValue = 1))
	val primaryChar: String,

	@BitField(ordering = 3)
	val properName: String = rawName
) {

	internal constructor() : this("", null, "")

	@BitField(ordering = 4, optional = true)
	@ReferenceField(stable = false, label = "elements")
	var weakAgainst: Element? = null
		private set

	fun setWeakAgainst(element: Element) {
		if (weakAgainst != null) throw IllegalStateException("$this is already weak against $weakAgainst")
		weakAgainst = element
	}

	override fun toString() = properName
}
