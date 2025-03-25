package mardek.content.stats

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.StringField
import mardek.content.sprite.KimSprite

@BitStruct(backwardCompatible = true)
class Element(
	@BitField(id = 0, optional = true)
	val rawName: String,

	@BitField(id = 1, optional = true)
	val bonusStat: CombatStat?,

	@BitField(id = 2)
	@StringField(length = IntegerField(expectUniform = true, minValue = 1, maxValue = 1))
	val primaryChar: String,

	@BitField(id = 3)
	val properName: String = rawName,

	@BitField(id = 4)
	@IntegerField(expectUniform = true)
	val color: Int,

	@BitField(id = 5)
	val sprite: KimSprite,
) {

	constructor() : this("", null, "", "", 0, KimSprite())

	@BitField(id = 6, optional = true)
	@ReferenceField(stable = false, label = "elements")
	var weakAgainst: Element? = null
		private set

	fun setWeakAgainst(element: Element) {
		if (weakAgainst != null) throw IllegalStateException("$this is already weak against $weakAgainst")
		weakAgainst = element
	}

	override fun toString() = properName
}
