package mardek.importer.battle

import mardek.assets.battle.CounterAttack
import mardek.assets.battle.StrategyTarget
import mardek.assets.skill.ActiveSkill
import mardek.importer.area.parseFlashString
import mardek.importer.skills.SkillParseException
import mardek.importer.util.parseActionScriptNestedList
import java.lang.Integer.parseInt
import kotlin.math.roundToInt

internal fun importCounterAttacks(
	rawList: String, actions: List<ActiveSkill>,
	targetMap: Map<ActiveSkill, StrategyTarget>,
	legionSkills: List<ActiveSkill>,
): ArrayList<CounterAttack> {
	if (rawList == "null") return ArrayList(0)
	val nestedList = parseActionScriptNestedList(rawList)
	if (nestedList !is ArrayList<*>) throw SkillParseException("Unexpected counter attacks $rawList")
	var remainingChance = 100
	return ArrayList(nestedList.map { rawPair ->
		if (rawPair !is ArrayList<*>) throw SkillParseException("Unexpected counter attacks $rawList")
		val actionName = parseFlashString(rawPair[0].toString(), "counter attack skill")!!
		val action = actions.find { it.name == actionName } ?: legionSkills.find { it.name == actionName }!!
		val rawChance = parseInt(rawPair[1].toString())
		val chance = (rawChance * remainingChance / 100.0).roundToInt()
		remainingChance -= chance
		CounterAttack(action = action, chance = chance, target = targetMap[action]!!)
	})
}
