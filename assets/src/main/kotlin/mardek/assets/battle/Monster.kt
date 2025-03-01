package mardek.assets.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.ReferenceFieldTarget
import mardek.assets.animations.BattleModel
import mardek.assets.combat.*
import mardek.assets.inventory.Dreamstone
import mardek.assets.skill.ActiveSkill

@BitStruct(backwardCompatible = false)
class Monster(

	@BitField(ordering = 0)
	val name: String,

	@BitField(ordering = 1)
	val model: BattleModel,

	@BitField(ordering = 2)
	val className: String,

	@BitField(ordering = 3)
	@ReferenceField(stable = false, label = "creature types")
	val type: CreatureType,

	@BitField(ordering = 4)
	@ReferenceField(stable = false, label = "elements")
	val element: Element,

	@BitField(ordering = 5)
	@NestedFieldSetting(path = "k", fieldName = "BASE_STATS_KEY_PROPERTIES")
	@IntegerField(expectUniform = false, minValue = 0)
	val baseStats: HashMap<CombatStat, Int>,

	@BitField(ordering = 6)
	@IntegerField(expectUniform = false, minValue = 0)
	val hpPerLevel: Int,

	@BitField(ordering = 7)
	@IntegerField(expectUniform = false, minValue = 0)
	val attackPerLevelNumerator: Int,

	@BitField(ordering = 8)
	@IntegerField(expectUniform = false, minValue = 0)
	val attackPerLevelDenominator: Int,

	@BitField(ordering = 9)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 100)
	val critChance: Int,

	@BitField(ordering = 10)
	@IntegerField(expectUniform = false, minValue = 0)
	val experience: Int,

	@BitField(ordering = 11)
	val loot: ArrayList<PotentialItem>,

	@BitField(ordering = 11)
	val plotLoot: ArrayList<PotentialPlotItem>,

	@BitField(ordering = 12)
	@ReferenceField(stable = false, label = "dreamstones")
	val dreamLoot: ArrayList<Dreamstone>,

	@BitField(ordering = 12)
	val weapon: PotentialEquipment,

	@BitField(ordering = 13)
	val shield: PotentialEquipment,

	@BitField(ordering = 14)
	val helmet: PotentialEquipment,

	@BitField(ordering = 15)
	val armor: PotentialEquipment,

	@BitField(ordering = 16)
	val accessory1: PotentialEquipment,

	@BitField(ordering = 17)
	val accessory2: PotentialEquipment,

	@BitField(ordering = 18)
	val resistances: Resistances,

	@BitField(ordering = 19)
	@NestedFieldSetting(path = "k", fieldName = "SHIFT_RESISTANCES_KEY_PROPERTIES")
	val elementalShiftResistances: HashMap<Element, Resistances>,

	@BitField(ordering = 20)
	val attackEffects: ArrayList<PossibleStatusEffect>,

	@BitField(ordering = 21)
	@ReferenceFieldTarget(label = "skills")
	val actions: ArrayList<ActiveSkill>,

	@BitField(ordering = 22)
	val strategies: ArrayList<StrategyPool>,

	@BitField(ordering = 23)
	val meleeCounterAttacks: ArrayList<CounterAttack>,

	@BitField(ordering = 24)
	val rangedCounterAttacks: ArrayList<CounterAttack>,
) {

	constructor() : this(
		name = "",
		model = BattleModel(),
		className = "",
		type = CreatureType(),
		element = Element(),
		baseStats = HashMap(),
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
		actions = ArrayList(),
		strategies = ArrayList(),
		meleeCounterAttacks = ArrayList(),
		rangedCounterAttacks = ArrayList(),
	)

	override fun toString() = name

	companion object {

		@Suppress("unused")
		@JvmStatic
		@ReferenceField(stable = false, label = "stats")
		private val BASE_STATS_KEY_PROPERTIES = false

		@Suppress("unused")
		@JvmStatic
		@ReferenceField(stable = false, label = "elements")
		private val SHIFT_RESISTANCES_KEY_PROPERTIES = false
	}
}

/**
if(stats.name == "Steele")
{
stats.ATK = 3 + 3 * stats.level;
}
else if(stats.monStats != null && (stats.weapon == "none" || stats.weapon == null))
{
stats.ATK = stats.monStats.nAtk + (!(stats.monStats.atkGrowth[1] && stats.level) ? 0 : stats.monStats.atkGrowth[0] * Math.floor(stats.level / stats.monStats.atkGrowth[1]));
}
else
{
stats.ATK = GetItemInfo(stats.weapon).atk;
}
 */