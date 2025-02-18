package mardek.importer.battle

import mardek.assets.battle.CounterAttack
import mardek.assets.battle.StrategyTarget
import mardek.assets.skill.ActiveSkill
import mardek.importer.area.parseFlashString
import mardek.importer.skills.SkillParseException
import mardek.importer.util.parseActionScriptNestedList
import java.lang.Integer.parseInt

internal fun importCounterAttacks(rawList: String, actions: List<ActiveSkill>, targetMap: Map<ActiveSkill, StrategyTarget>): ArrayList<CounterAttack> {
	if (rawList == "null") return ArrayList(0)
	val nestedList = parseActionScriptNestedList(rawList)
	if (nestedList !is ArrayList<*>) throw SkillParseException("Unexpected counter attacks $rawList")
	return ArrayList(nestedList.map { rawPair ->
		if (rawPair !is ArrayList<*>) throw SkillParseException("Unexpected counter attacks $rawList")
		val actionName = parseFlashString(rawPair[0].toString(), "counter attack skill")!!
		val action = actions.find { it.name == actionName }!!
		CounterAttack(action = action, chance = parseInt(rawPair[1].toString()), target = targetMap[action]!!)
	})
}
