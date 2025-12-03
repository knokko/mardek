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

/**
 * Represents a type of monster, for instance a Fungoblin. Some monsters (e.g. Lake Hag) are unique, whereas other
 * monsters (e.g. Fungoblin) are common and can occur more than once in the same battle.
 *
 * Every combatant in the game is either a monster or a player. The core difference is that players are controlled by
 * the player, whereas monsters are not. This engine allows monsters to fight alongside the player, and even allows
 * player combatants to side with the monsters.
 */
@BitStruct(backwardCompatible = true)
class Monster(

	/**
	 * The name of the monster, as imported from Flash. It doesn't really serve a purpose at the moment.
	 */
	@BitField(id = 0)
	val name: String,

	/**
	 * The display name of the monster, which will be shown near its health bar at the top of the battle screen.
	 */
	@BitField(id = 1)
	val displayName: String,

	/**
	 * This defines the appearance and animations of the monster. It is simply a combatant skeleton with an associated
	 * skin.
	 */
	@BitField(id = 2)
	val animations: CombatantAnimations,

	/**
	 * The class name of the monster, as shown in the battle info popup/modal. Aside from being shown in the info
	 * popup, it doesn't do anything.
	 */
	@BitField(id = 3)
	val className: String,

	/**
	 * The creature type of this monster, as shown in the battle info popup/modal. It also determines whether `QUARRY`
	 * (creature type bonus) reaction skills work on this monster.
	 */
	@BitField(id = 4)
	@ReferenceField(stable = false, label = "creature types")
	val type: CreatureType,

	/**
	 * The element of this monster, which is shown next to its health bar. Some skills (e.g. Air Slash & Smite Evil)
	 * deal more damage against monsters of a specific element.
	 */
	@BitField(id = 5)
	@ReferenceField(stable = false, label = "elements")
	val element: Element,

	/**
	 * The default/base stats (STR, SPR, max health, etc...) of this monster. When the monster doesn't have any stat
	 * modifiers (or stat-increasing equipment), these will be their effective stats.
	 */
	@BitField(id = 6)
	@IntegerField(expectUniform = false, minValue = 0, digitSize = 2)
	val baseStats: HashMap<CombatStat, Int>,

	/**
	 * For almost all monsters, the `playerStatModifier` is 0, which means that the maximum HP/MP of the monster are
	 * determined by their `CombatStat.MaxHealth + level * hpPerLevel` and `CombatStat.MaxMana + level * mpPerLevel`.
	 *
	 * When `playerStatModifier > 0`, the HP/MP of the monster follows the same rules as the HP/MP of players, except
	 * that their value is multiplied by `playerStatModifier`, so:
	 * - `playerStatModifier == 1`: monster HP formula = player HP formula
	 * - `playerStatModifier == 4`: monster HP formula = 4 * player HP formula
	 */
	@BitField(id = 7)
	@IntegerField(expectUniform = false, minValue = 0)
	val playerStatModifier: Int,

	/**
	 * When `playerStatModifier == 0`, the maximum HP of the monster is its `CombatStat.MaxHealth + hpPerLevel * level`
	 */
	@BitField(id = 8)
	@IntegerField(expectUniform = false, minValue = 0)
	val hpPerLevel: Int,

	/**
	 * Used in the formula to calculate the ATK of some monsters TODO CHAP1 Figure this out
	 */
	@BitField(id = 9)
	@IntegerField(expectUniform = false, minValue = 0)
	val attackPerLevelNumerator: Int,

	/**
	 * Used in the formula to calculate the ATK of some monsters TODO CHAP1 Figure this out
	 */
	@BitField(id = 10)
	@IntegerField(expectUniform = false, minValue = 0)
	val attackPerLevelDenominator: Int,

	/**
	 * The (base) critical hit chance of the monster. If the monster uses a weapon, the weapon can override this.
	 * If the monster uses a skill rather than basic attack, the skill can also override it.
	 */
	@BitField(id = 11)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 100)
	val critChance: Int,

	/**
	 * This has something to do with the amount of EXP that the player gains by killing this monster.
	 * TODO CHAP1 Figure this out
	 */
	@BitField(id = 12)
	@IntegerField(expectUniform = false, minValue = 0)
	val experience: Int,

	/**
	 * The loot that this monster can drop after it has been slain in combat.
	 */
	@BitField(id = 13)
	val loot: ArrayList<PotentialItem>,

	/**
	 * The plot items that this monster may drop after it has been slain in combat.
	 */
	@BitField(id = 14)
	val plotLoot: ArrayList<PotentialPlotItem>,

	/**
	 * The dreamstones that this monster may drop after it has been slain in combat.
	 */
	@BitField(id = 15)
	@ReferenceField(stable = false, label = "dreamstones")
	val dreamLoot: ArrayList<Dreamstone>,

	/**
	 * The weapons that this monster may spawn with.
	 */
	@BitField(id = 16)
	val weapon: PotentialEquipment,

	/**
	 * The shield that this monster may spawn with.
	 */
	@BitField(id = 17)
	val shield: PotentialEquipment,

	/**
	 * The helmets that this monster may spawn with.
	 */
	@BitField(id = 18)
	val helmet: PotentialEquipment,

	/**
	 * The armor that this monster may spawn with.
	 */
	@BitField(id = 19)
	val armor: PotentialEquipment,

	/**
	 * The accessories that this monster may carry in its first accessory slot.
	 */
	@BitField(id = 20)
	val accessory1: PotentialEquipment,

	/**
	 * The accessories that this monster may carry in its second accessory slot.
	 */
	@BitField(id = 21)
	val accessory2: PotentialEquipment,

	/**
	 * The (base) elemental resistances and status effect resistances. Note that these can be increased/decreased by
	 * potential equipment or status effects.
	 */
	@BitField(id = 22)
	val resistances: Resistances,

	/**
	 * For monsters that can change their own element (Master Stone & Karnos), this map overrides `resistances`:
	 * when the current element of the monster is a key into `elementalShiftResistances`, the `resistances` are
	 * overridden by the mapped value.
	 */
	@BitField(id = 23)
	@NestedFieldSetting(path = "k", fieldName = "SHIFT_RESISTANCES_KEY_PROPERTIES")
	val elementalShiftResistances: HashMap<Element, Resistances>,

	/**
	 * When this monster hits a melee attack, the target may get these status effects.
	 */
	@BitField(id = 24)
	val attackEffects: ArrayList<PossibleStatusEffect>,

	/**
	 * This monster will start each battle with these status effects
	 */
	@BitField(id = 25)
	@ReferenceField(stable = false, label = "status effects")
	val initialEffects: ArrayList<StatusEffect>,

	/**
	 * The active skills that this monster has. The `strategies` will determine which skill the monster chooses, and
	 * when.
	 */
	@BitField(id = 26)
	@ReferenceFieldTarget(label = "skills")
	val actions: ArrayList<ActiveSkill>,

	/**
	 * These strategy 'pools' determine how this monster will choose its next skill/move each turn.
	 *
	 * This is a list of `StrategyPool`s. When a monster gets on turn, it will iterate over its strategy pools. It will
	 * skip any pool whose `criteria` are not satisfied.
	 *
	 * TODO CHAP2 numb, curse, and silence
	 * When all criteria of a pool are satisfied, and the monster has enough mana to perform at least 1 of the `entries`
	 * of the pool, then the pool is *potentially selected*. To determine whether the pool is *selected* the *sum* of
	 * the `chance` of all its `entries` is computed. Let's call this `totalChance`. If `totalChance` is e.g. 70%,
	 * then there is a 70% chance that the pool is *selected*, and the iteration ends.
	 * Otherwise, the pool is skipped, and the next strategy pool is considered.
	 *
	 * When a strategy pool is *selected*, one of its `entries` will be *chosen*. First, the entries that cost too much
	 * mana are discarded. The sum of the `chance` of the remaining entries is computed. Let's call it
	 * `remainingChance`. When `remainingChance == 0` (so all remaining entries have a chance of `0`), the *chosen*
	 * entry is randomly selected from the remaining entries, each with a chance of `100% / remainingEntries.size`.
	 * When `remainingChance > 0`, each entry has a chance of `100% * entry.chance / remainingChance` to be selected.
	 *
	 * With this behavior, an entry with `chance == 0` can only be selected if all entries with `chance > 0` cost too
	 * much mana.
	 *
	 * When no strategy pool was *selected*, the monster will skip its turn. Note that this is uncommon, since the last
	 * strategy pool of a monster usually has no criteria and 100% chance. Skipping a turn usually only happens when
	 * the monster is out of mana, numbed, cursed, or silenced.
	 */
	@BitField(id = 27)
	@ReferenceFieldTarget(label = "strategy pools")
	val strategies: ArrayList<StrategyPool>,

	/**
	 * When this monster gets hit by a melee attack, it can respond by using these counter-attacks. Such counter-attacks
	 * happen immediately, and do **not** cost a turn.
	 */
	@BitField(id = 28)
	val meleeCounterAttacks: ArrayList<CounterAttack>,

	/**
	 * When this monster gets hit by a ranged attack, it can respond by using these counter-attacks. Such
	 * counter-attacks happen immediately, and do **not** cost a turn.
	 */
	@BitField(id = 29)
	val rangedCounterAttacks: ArrayList<CounterAttack>,

	/**
	 * The unique ID of this monster, which is used for (de)serialization
	 */
	@BitField(id = 30)
	@StableReferenceFieldId
	val id: UUID,
) {

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
		id = UUID.randomUUID(),
	)

	override fun toString() = name

	companion object {

		@Suppress("unused")
		@JvmStatic
		@ReferenceField(stable = false, label = "elements")
		private val SHIFT_RESISTANCES_KEY_PROPERTIES = false
	}
}
