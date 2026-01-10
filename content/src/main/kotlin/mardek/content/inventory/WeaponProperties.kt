package mardek.content.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.audio.SoundEffect
import mardek.content.stats.PossibleStatusEffect
import mardek.content.stats.ElementalDamageBonus
import mardek.content.stats.CreatureTypeBonus

@BitStruct(backwardCompatible = true)
class WeaponProperties(

	@BitField(id = 0)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 999)
	val hitChance: Int,

	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 0, maxValue = 100)
	val critChance: Int,

	@BitField(id = 2)
	@FloatField(expectMultipleOf = 0.1)
	val hpDrain: Float,

	@BitField(id = 3)
	@FloatField(expectMultipleOf = 0.1)
	val mpDrain: Float,

	@BitField(id = 4)
	val effectiveAgainstCreatureTypes: ArrayList<CreatureTypeBonus>,

	@BitField(id = 5)
	val effectiveAgainstElements: ArrayList<ElementalDamageBonus>,

	@BitField(id = 6)
	val addEffects: ArrayList<PossibleStatusEffect>,

	@BitField(id = 7, optional = true)
	@ReferenceField(stable = false, label = "sound effects")
	val hitSound: SoundEffect?,
) {

	@Suppress("unused")
	private constructor() : this(
			0, 0, 0f, 0f,
		ArrayList(0),
		ArrayList(0),
		ArrayList(0), null,
	)
}
