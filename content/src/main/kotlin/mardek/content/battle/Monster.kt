package mardek.content.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.ReferenceFieldTarget
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.content.animation.CombatantAnimations
import mardek.content.stats.*
import mardek.content.inventory.Dreamstone
import mardek.content.skill.ActiveSkill
import java.util.UUID

@BitStruct(backwardCompatible = true)
class Monster(

	@BitField(id = 0)
	val name: String,

	@BitField(id = 1)
	val displayName: String,

	@BitField(id = 2)
	val animations: CombatantAnimations,

	@BitField(id = 3)
	val className: String,

	@BitField(id = 4)
	@ReferenceField(stable = false, label = "creature types")
	val type: CreatureType,

	@BitField(id = 5)
	@ReferenceField(stable = false, label = "elements")
	val element: Element,

	@BitField(id = 6)
	@IntegerField(expectUniform = false, minValue = 0)
	val baseStats: HashMap<CombatStat, Int>,

	@BitField(id = 7)
	@IntegerField(expectUniform = false, minValue = 0)
	val playerStatModifier: Int,

	@BitField(id = 8)
	@IntegerField(expectUniform = false, minValue = 0)
	val hpPerLevel: Int,

	@BitField(id = 9)
	@IntegerField(expectUniform = false, minValue = 0)
	val attackPerLevelNumerator: Int,

	@BitField(id = 10)
	@IntegerField(expectUniform = false, minValue = 0)
	val attackPerLevelDenominator: Int,

	@BitField(id = 11)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 100)
	val critChance: Int,

	@BitField(id = 12)
	@IntegerField(expectUniform = false, minValue = 0)
	val experience: Int,

	@BitField(id = 13)
	val loot: ArrayList<PotentialItem>,

	@BitField(id = 14)
	val plotLoot: ArrayList<PotentialPlotItem>,

	@BitField(id = 15)
	@ReferenceField(stable = false, label = "dreamstones")
	val dreamLoot: ArrayList<Dreamstone>,

	@BitField(id = 16)
	val weapon: PotentialEquipment,

	@BitField(id = 17)
	val shield: PotentialEquipment,

	@BitField(id = 18)
	val helmet: PotentialEquipment,

	@BitField(id = 19)
	val armor: PotentialEquipment,

	@BitField(id = 20)
	val accessory1: PotentialEquipment,

	@BitField(id = 21)
	val accessory2: PotentialEquipment,

	@BitField(id = 22)
	val resistances: Resistances,

	@BitField(id = 23)
	@NestedFieldSetting(path = "k", fieldName = "SHIFT_RESISTANCES_KEY_PROPERTIES")
	val elementalShiftResistances: HashMap<Element, Resistances>,

	@BitField(id = 24)
	val attackEffects: ArrayList<PossibleStatusEffect>,

	@BitField(id = 25)
	@ReferenceField(stable = false, label = "status effects")
	val initialEffects: ArrayList<StatusEffect>,

	@BitField(id = 26)
	@ReferenceFieldTarget(label = "skills")
	val actions: ArrayList<ActiveSkill>,

	@BitField(id = 27)
	@ReferenceFieldTarget(label = "strategy pools")
	val strategies: ArrayList<StrategyPool>,

	@BitField(id = 28)
	val meleeCounterAttacks: ArrayList<CounterAttack>,

	@BitField(id = 29)
	val rangedCounterAttacks: ArrayList<CounterAttack>,
) {

	@BitField(id = 30)
	@StableReferenceFieldId
	val id = UUID.randomUUID()!!

	constructor() : this(
		name = "",
		displayName = "",
		animations = CombatantAnimations(),
		className = "",
		type = CreatureType(),
		element = Element(),
		baseStats = hashMapOf(Pair(CombatStat.MaxHealth, 1), Pair(CombatStat.MaxMana, 1)),
		playerStatModifier = 0,
		hpPerLevel = 0,
		attackPerLevelNumerator = 0,
		attackPerLevelDenominator = 0,
		critChance = 0,
		experience = 0,
		loot = ArrayList(),
		plotLoot = ArrayList(),
		dreamLoot = ArrayList(),
		weapon = PotentialEquipment(),
		shield = PotentialEquipment(),
		helmet = PotentialEquipment(),
		armor = PotentialEquipment(),
		accessory1 = PotentialEquipment(),
		accessory2 = PotentialEquipment(),
		resistances = Resistances(),
		elementalShiftResistances = HashMap(0),
		attackEffects = ArrayList(),
		initialEffects = ArrayList(),
		actions = ArrayList(),
		strategies = ArrayList(),
		meleeCounterAttacks = ArrayList(),
		rangedCounterAttacks = ArrayList(),
	)

	override fun toString() = name

	companion object {

		@Suppress("unused")
		@JvmStatic
		@ReferenceField(stable = false, label = "elements")
		private val SHIFT_RESISTANCES_KEY_PROPERTIES = false
	}
}
