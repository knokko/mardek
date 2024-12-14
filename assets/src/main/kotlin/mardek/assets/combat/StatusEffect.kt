package mardek.assets.combat

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.StableReferenceFieldId
import java.util.*

@BitStruct(backwardCompatible = false)
class StatusEffect(
	@BitField(ordering = 0)
	val flashName: String,

	@BitField(ordering = 1, optional = true)
	val niceName: String?,

	@BitField(ordering = 2)
	val isPositive: Boolean,

	@BitField(ordering = 3)
	val disappearsAfterCombat: Boolean,

	@BitField(ordering = 4)
	@FloatField(expectMultipleOf = 0.05)
	val damageFractionPerTurn: Float = 0f,

	@BitField(ordering = 5)
	@IntegerField(expectUniform = false, minValue = 0)
	val damageOutsideCombat: Int = 0,

	@BitField(ordering = 6)
	@FloatField(expectMultipleOf = 0.25)
	val meleeDamageModifier: Float = 1f,

	@BitField(ordering = 7)
	@FloatField(expectMultipleOf = 0.25)
	val meleeDamageReduction: Float = 0f,

	@BitField(ordering = 8)
	@FloatField(expectMultipleOf = 0.25)
	val rangedDamageReduction: Float = 0f,

	@BitField(ordering = 9, optional = true)
	@ReferenceField(stable = false, label = "elements")
	val nullifiesElement: Element? = null,

	@BitField(ordering = 10, optional = true)
	@ReferenceField(stable = false, label = "elements")
	val elementShell: Element? = null,

	@BitField(ordering = 11)
	@IntegerField(expectUniform = false, minValue = 0)
	val missChance: Int = 0,

	@BitField(ordering = 12)
	val blocksRangedSkills: Boolean = false,

	@BitField(ordering = 13)
	val blocksMeleeSkills: Boolean = false,

	@BitField(ordering = 14)
	val blocksBasicAttacks: Boolean = false,

	@BitField(ordering = 15)
	val isConfusing: Boolean = false,

	@BitField(ordering = 16)
	val isZombie: Boolean = false,

	@BitField(ordering = 17)
	val hasHaste: Boolean = false,

	@BitField(ordering = 18)
	val isReckless: Boolean = false,

	@BitField(ordering = 19)
	val canWaterBreathe: Boolean = false,

	@BitField(ordering = 20)
	val hasBarskin: Boolean = false,

	@BitField(ordering = 21)
	val isAstralForm: Boolean = false,

	@BitField(ordering = 22)
	@IntegerField(expectUniform = false, minValue = 0, maxValue = 100)
	val skipTurnChance: Int = 0,

	@BitField(ordering = 23)
	@IntegerField(expectUniform = false, minValue = 0, maxValue = 100)
	val disappearChancePerTurn: Int = 0,
) {

	@Suppress("unused")
	@BitField(ordering = 24)
	@StableReferenceFieldId
	val id = UUID.randomUUID()!!

	internal constructor() : this("", null, false, false)

	override fun toString() = niceName ?: flashName
}
