package mardek.importer.battle

import mardek.assets.battle.*
import mardek.assets.combat.CombatAssets
import mardek.assets.inventory.InventoryAssets
import mardek.assets.inventory.Item
import mardek.assets.skill.ActiveSkill
import mardek.assets.skill.ElementalDamageBonus
import mardek.assets.skill.SkillAssets
import mardek.importer.area.parseFlashString
import mardek.importer.skills.SkillParseException
import mardek.importer.util.parseActionScriptNestedList
import mardek.importer.util.parseActionScriptObjectList
import java.lang.Double.parseDouble
import java.lang.Integer.parseInt
import kotlin.math.roundToInt

internal fun importMonsterStrategies(
	rawGambits: String, actions: List<ActiveSkill>,
	combatAssets: CombatAssets, skillAssets: SkillAssets, itemAssets: InventoryAssets,
	targetMap: MutableMap<ActiveSkill, StrategyTarget>
): ArrayList<StrategyPool> {
	val jsonList = parseActionScriptObjectList(rawGambits)
	var rawStrategies = jsonList.map {
		rawProperties -> importRawMonsterStrategy(rawProperties, actions, combatAssets, skillAssets, itemAssets)
	}
	for (raw in rawStrategies) {
		if (raw.action != null && !targetMap.containsKey(raw.action)) targetMap[raw.action] = raw.target
	}
	rawStrategies = rawStrategies.filter { it.chance != 0 }
	val previousPools = ArrayList<StrategyPool>()

	var currentPool = StrategyPool(criteria = rawStrategies[0].criteria, entries = arrayListOf(rawStrategies[0].toEntry(100)))
	var remainingChance = 100 - rawStrategies[0].chance
	for (index in 1 until rawStrategies.size) {
		val raw = rawStrategies[index]
		if (raw.criteria == currentPool.criteria) {
			currentPool.entries.add(raw.toEntry(remainingChance))
			remainingChance -= currentPool.entries.last().chance
		} else {
			previousPools.add(currentPool)
			currentPool = StrategyPool(criteria = raw.criteria, entries = arrayListOf(raw.toEntry(100)))
			remainingChance = 100 - raw.chance
		}
	}

	previousPools.add(currentPool)
	return previousPools
}

private class RawMonsterStrategy(
	val action: ActiveSkill?,
	val item: Item?,
	val target: StrategyTarget,
	val chance: Int,
	val criteria: StrategyCriteria
) {
	fun toEntry(remainingChance: Int) = StrategyEntry(
		skill = action, item = item, target = target, chance = (chance * remainingChance / 100.0).roundToInt()
	)
}

private fun importRawMonsterStrategy(
	rawProperties: Map<String, String>, actions: List<ActiveSkill>,
	combatAssets: CombatAssets, skillAssets: SkillAssets, itemAssets: InventoryAssets
): RawMonsterStrategy {
	val skillName = parseFlashString(rawProperties["command"]!!, "gambit command")!!
	val (action, item) = if (skillName == "Attack") Pair(null, null) else {
		val myAction = actions.find { it.name == skillName }
		if (myAction != null) Pair(myAction, null) else {
			val mimicry = skillAssets.classes.find { it.name == "Mimicry" }!!
			val legionAction = mimicry.actions.find { it.name == skillName }
			if (legionAction != null) Pair(legionAction, null) else {
				val item = itemAssets.items.find { it.flashName == skillName } ?:
						throw SkillParseException("Can't find skill $skillName")
				Pair(null, item)
			}
		}
	}
	val target = when (parseFlashString(rawProperties["target"]!!, "gambit target")!!) {
		"ANY_PC" -> StrategyTarget.AnyPlayer
		"SELF" -> StrategyTarget.Self
		"ALL_p" -> StrategyTarget.AllPlayers
		else -> throw SkillParseException("Unexpected gambit target in $rawProperties")
	}

	var criteria = StrategyCriteria.NONE
	val rawUses = rawProperties["uses"]
	if (rawUses != null) criteria = criteria.merge(StrategyCriteria(maxUses = parseInt(rawUses)))

	val rawOtherTurn = rawProperties["OtherTurn"]
	if (rawOtherTurn == "0") criteria = criteria.merge(StrategyCriteria(canUseOnOddTurns = false))
	if (rawOtherTurn == "1") criteria = criteria.merge(StrategyCriteria(canUseOnEvenTurns = false))

	val rawCriteria = parseActionScriptNestedList(rawProperties["criteria"]!!)
	val rawChance = rawProperties["random"]
	var chance = if (rawChance != null) parseInt(rawChance) else 100

	if (rawCriteria is ArrayList<*> && rawCriteria.size >= 2) {
		var type = parseFlashString(rawCriteria[0].toString(), "gambit criteria type")!!
		if (type == "random") {
			chance = parseInt(rawCriteria[1].toString())
		} else {
			@Suppress("SpellCheckingInspection")
			if (type == "MYHP<") type = "HP<"
			criteria = criteria.merge(when (type) {
				"HP<" -> StrategyCriteria(
					hpPercentageAtMost = (100.0 * parseDouble(rawCriteria[1].toString())).roundToInt()
				)

				"HP>" -> StrategyCriteria(
					hpPercentageAtLeast = (100.0 * parseDouble(rawCriteria[1].toString())).roundToInt()
				)

				"has_status" -> StrategyCriteria(
					targetHasEffect = combatAssets.statusEffects.find {
						it.flashName == parseFlashString(rawCriteria[1].toString(), "has_status effect")!!
					}!!
				)

				"resist<" -> StrategyCriteria(
					resistanceAtMost = ElementalDamageBonus(
						combatAssets.elements.find { it.rawName == parseFlashString(rawCriteria[1].toString(), "resist element") }!!,
						parseInt(rawCriteria[2].toString()) / 100f
					)
				)

				else -> throw SkillParseException("Unexpected gambit criteria $rawCriteria")
			})
		}
	} else {
		if (rawCriteria != "null") throw SkillParseException("Unexpected raw criteria $rawCriteria")
	}

	return RawMonsterStrategy(action = action, item = item, target = target, criteria = criteria, chance = chance)
}
