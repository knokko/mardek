package mardek.content.stats

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.StringField
import mardek.content.particle.ParticleEffect
import mardek.content.sprite.BcSprite

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
	val thickSprite: BcSprite,

	@BitField(id = 6)
	val thinSprite: BcSprite,

	@BitField(id = 7, optional = true)
	val swingEffect: BcSprite?,

	@BitField(id = 8, optional = true)
	@ReferenceField(stable = false, label = "particles")
	val spellCastEffect: ParticleEffect?,

	@BitField(id = 9, optional = true)
	val spellCastBackground: BcSprite?,
) {

	constructor() : this(
		"", null, "", "", 0, BcSprite(),
		BcSprite(), BcSprite(), null, null
	)

	@BitField(id = 10, optional = true)
	@ReferenceField(stable = false, label = "elements")
	var weakAgainst: Element? = null
		private set

	fun setWeakAgainst(element: Element) {
		if (weakAgainst != null) throw IllegalStateException("$this is already weak against $weakAgainst")
		weakAgainst = element
	}

	override fun toString() = properName
}
