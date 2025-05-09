package mardek.content.stats

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.StableReferenceFieldId
import java.util.*
import kotlin.collections.ArrayList

@BitStruct(backwardCompatible = true)
class StatusEffect(
	@BitField(id = 0)
	val flashName: String,

	@BitField(id = 1, optional = true)
	val niceName: String?,

	@BitField(id = 2)
	val isPositive: Boolean,

	@BitField(id = 3)
	val disappearsAfterCombat: Boolean,

	@BitField(id = 4)
	@FloatField(expectMultipleOf = 0.05)
	val damageFractionPerTurn: Float = 0f,

	@BitField(id = 5)
	@IntegerField(expectUniform = false, minValue = 0)
	val damageOutsideCombat: Int = 0,

	@BitField(id = 6)
	@FloatField(expectMultipleOf = 0.25)
	val meleeDamageModifier: Float = 1f,

	@BitField(id = 7)
	@FloatField(expectMultipleOf = 0.25)
	val meleeDamageReduction: Float = 0f,

	@BitField(id = 8)
	@FloatField(expectMultipleOf = 0.25)
	val rangedDamageReduction: Float = 0f,

	@BitField(id = 9, optional = true)
	@ReferenceField(stable = false, label = "elements")
	val nullifiesElement: Element? = null,

	@BitField(id = 10, optional = true)
	@ReferenceField(stable = false, label = "elements")
	val elementShell: Element? = null,

	@BitField(id = 11)
	@IntegerField(expectUniform = false, minValue = 0)
	val missChance: Int = 0,

	@BitField(id = 12)
	val blocksRangedSkills: Boolean = false,

	@BitField(id = 13)
	val blocksMeleeSkills: Boolean = false,

	@BitField(id = 14)
	val blocksBasicAttacks: Boolean = false,

	@BitField(id = 15)
	val isConfusing: Boolean = false,

	@BitField(id = 16)
	val isZombie: Boolean = false,

	@BitField(id = 17)
	@IntegerField(expectUniform = false, minValue = 0)
	val extraTurns: Int = 0,

	@BitField(id = 18)
	val isReckless: Boolean = false,

	@BitField(id = 19)
	val canWaterBreathe: Boolean = false,

	@BitField(id = 20)
	val hasBarskin: Boolean = false,

	@BitField(id = 21)
	val isAstralForm: Boolean = false,

	@BitField(id = 22)
	val statModifiers: ArrayList<StatModifier> = ArrayList(0),

	@BitField(id = 23)
	val resistances: Resistances = Resistances(),

	@BitField(id = 24)
	@IntegerField(expectUniform = false, minValue = 0, maxValue = 100)
	val skipTurnChance: Int = 0,

	@BitField(id = 25)
	@IntegerField(expectUniform = false, minValue = 0, maxValue = 100)
	val disappearChancePerTurn: Int = 0,
) {

	@Suppress("unused")
	@BitField(id = 26)
	@StableReferenceFieldId
	val id = UUID.randomUUID()!!

	internal constructor() : this("", null, false, false)

	override fun toString() = niceName ?: flashName

	fun getModifier(stat: CombatStat) = statModifiers.sumOf { if (it.stat == stat) it.adder else 0 }
}
