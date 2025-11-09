package mardek.content.stats

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.content.particle.ParticleEffect
import mardek.content.sprite.BcSprite
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

	@BitField(id = 4, optional = true)
	val damagePerTurn: TurnDamage? = null,

	@BitField(id = 5, optional = true)
	val damageWhileWalking: WalkDamage? = null,

	@BitField(id = 6)
	@FloatField(expectMultipleOf = 0.25)
	val meleeDamageModifier: Float = 0f,

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

	@BitField(id = 24, optional = true)
	val skipTurn: SkipTurn? = null,

	@BitField(id = 25)
	@IntegerField(expectUniform = false, minValue = 0, maxValue = 100)
	val disappearChancePerTurn: Int = 0,

	@BitField(id = 26)
	@IntegerField(expectUniform = false, minValue = 0, maxValue = 100)
	val disappearAfterHitChance: Int = 0,

	@BitField(id = 27)
	val icon: BcSprite,

	@BitField(id = 28)
	@IntegerField(expectUniform = true)
	val innerTextColor: Int,

	@BitField(id = 29)
	@IntegerField(expectUniform = true)
	val outerTextColor: Int,

	@BitField(id = 30)
	val passiveParticleSprites: Array<BcSprite>,

	@BitField(id = 31)
	val shortName: String = niceName ?: flashName,

	@Suppress("unused")
	@BitField(id = 32)
	@StableReferenceFieldId
	val id: UUID,
) {

	constructor() : this(
		"", null, false, false,
		icon = BcSprite(), innerTextColor = 0, outerTextColor = 0,
		passiveParticleSprites = emptyArray<BcSprite>(), id = UUID.randomUUID(),
	)

	override fun toString() = niceName ?: flashName

	fun getModifier(stat: CombatStat) = statModifiers.sumOf { if (it.stat == stat) it.adder else 0 }

	@BitStruct(backwardCompatible = true)
	class TurnDamage(

		@BitField(id = 0)
		@FloatField(expectMultipleOf = 0.05)
		val hpFraction: Float,

		@BitField(id = 1)
		@ReferenceField(stable = false, label = "elements")
		val element: Element,

		@BitField(id = 2)
		@ReferenceField(stable = false, label = "particles")
		val particleEffect: ParticleEffect,

		@BitField(id = 3)
		@IntegerField(expectUniform = true)
		val blinkColor: Int,
	) {
		@Suppress("unused")
		private constructor() : this(0f, Element(), ParticleEffect(), 0)
	}

	@BitStruct(backwardCompatible = true)
	class WalkDamage(

		/**
		 * The number of steps between taking consecutive damage
		 */
		@BitField(id = 0)
		@IntegerField(expectUniform = false, minValue = 1)
		val period: Int,

		@BitField(id = 1)
		@FloatField(expectMultipleOf = 0.01)
		val hpFraction: Float,

		@BitField(id = 2)
		@IntegerField(expectUniform = true)
		val blinkColor: Int,
	) {
		@Suppress("unused")
		private constructor() : this(0, 0f, 0)
	}

	@BitStruct(backwardCompatible = true)
	class SkipTurn(
		@BitField(id = 0)
		@IntegerField(expectUniform = true, minValue = 1, maxValue = 100)
		val chance: Int,

		@BitField(id = 1)
		@IntegerField(expectUniform = true)
		val blinkColor: Int,

		@BitField(id = 2, optional = true)
		@ReferenceField(stable = false, label = "particles")
		val particleEffect: ParticleEffect?,
	) {
		@Suppress("unused")
		private constructor() : this(0, 0, null)
	}
}
