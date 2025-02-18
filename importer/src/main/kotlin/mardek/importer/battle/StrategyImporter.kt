package mardek.importer.battle

import mardek.assets.battle.*
import mardek.assets.skill.ActiveSkill
import mardek.assets.skill.SkillAssets
import mardek.importer.area.parseFlashString
import mardek.importer.skills.SkillParseException
import mardek.importer.util.parseActionScriptNestedList
import mardek.importer.util.parseActionScriptObjectList
import java.lang.Double.parseDouble
import java.lang.Integer.parseInt
import kotlin.math.roundToInt

internal fun importMonsterStrategies(
	rawGambits: String, actions: List<ActiveSkill>, skillAssets: SkillAssets, targetMap: MutableMap<ActiveSkill, StrategyTarget>
): ArrayList<StrategyPool> {
	val jsonList = parseActionScriptObjectList(rawGambits)
	var rawStrategies = jsonList.map {
		rawProperties -> importRawMonsterStrategy(rawProperties, actions, skillAssets)
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
			remainingChance -= currentPool.entries.last.chance
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
	val target: StrategyTarget,
	val chance: Int,
	val criteria: StrategyCriteria
) {
	fun toEntry(remainingChance: Int) = StrategyEntry(skill = action, target = target, chance = chance * remainingChance / 100)
}

private fun importRawMonsterStrategy(
	rawProperties: Map<String, String>, actions: List<ActiveSkill>, skillAssets: SkillAssets
): RawMonsterStrategy {
	val skillName = parseFlashString(rawProperties["command"]!!, "gambit command")!!
	val action = if (skillName == "Attack") null
	else actions.find { it.name == skillName } ?:
			skillAssets.classes.find { it.name == "Mimicry" }!!.actions.find { it.name == skillName }
	val target = when (parseFlashString(rawProperties["target"]!!, "gambit target")!!) {
		"ANY_PC" -> StrategyTarget.AnyPlayer
		"SELF" -> StrategyTarget.Self
		"ALL_p" -> StrategyTarget.AllPlayers
		else -> throw SkillParseException("Unexpected gambit target in $rawProperties")
	}

	val rawUses = rawProperties["uses"]
	val maxUses = if (rawUses != null) parseInt(rawUses) else null

	val rawCriteria = parseActionScriptNestedList(rawProperties["criteria"]!!)
	val rawChance = rawProperties["chance"]
	var chance = if (rawChance != null) parseInt(rawChance) else 100
	val criteria: StrategyCriteria

	if (rawCriteria is ArrayList<*> && rawCriteria.size == 2) {
		val type = parseFlashString(rawCriteria[0].toString(), "gambit criteria type")!!
		if (type == "random") {
			chance = parseInt(rawCriteria[1].toString())
			criteria = StrategyCriteria.NONE
		} else {
			criteria = when (type) {
				"HP<" -> StrategyCriteria(maxUses = maxUses, hpPercentageAtMost = (100.0 * parseDouble(rawCriteria[1].toString())).roundToInt())
				"HP>" -> StrategyCriteria(maxUses = maxUses, hpPercentageAtLeast = (100.0 * parseDouble(rawCriteria[1].toString())).roundToInt())
				else -> throw SkillParseException("Unexpected gambit criteria $rawCriteria")
			}
		}
	} else {
		if (rawCriteria != "null") throw SkillParseException("Unexpected raw criteria $rawCriteria")
		criteria = StrategyCriteria.NONE
	}

	return RawMonsterStrategy(action = action, target = target, criteria = criteria, chance = chance)
}
