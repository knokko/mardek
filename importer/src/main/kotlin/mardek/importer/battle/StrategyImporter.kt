package mardek.importer.battle

import mardek.content.Content
import mardek.content.battle.*
import mardek.content.inventory.Item
import mardek.content.skill.ActiveSkill
import mardek.content.skill.SkillTargetType
import mardek.content.stats.ElementalResistance
import mardek.importer.area.parseFlashString
import mardek.importer.skills.SkillParseException
import mardek.importer.util.parseActionScriptNestedList
import mardek.importer.util.parseActionScriptObjectList
import java.lang.Double.parseDouble
import java.lang.Integer.parseInt
import java.util.UUID
import kotlin.math.roundToInt

internal fun importMonsterStrategies(
	monsterName: String, rawGambits: String, actions: List<ActiveSkill>, content: Content,
	targetMap: MutableMap<ActiveSkill, StrategyTarget>
): ArrayList<StrategyPool> {
	val jsonList = parseActionScriptObjectList(rawGambits)
	var rawStrategies = jsonList.map {
		rawProperties -> importRawMonsterStrategy(rawProperties, actions, content)
	}
	for (raw in rawStrategies) {
		if (raw.action != null && !targetMap.containsKey(raw.action)) {
			targetMap[raw.action] = raw.target

			// Due to a couple of inconsistencies in the Flash definitions, this work-around is needed
			if (raw.target == StrategyTarget.AllEnemies && raw.action.targetType == SkillTargetType.AllAllies) {
				raw.action.targetType = SkillTargetType.AllEnemies
			}
		}
	}
	rawStrategies = rawStrategies.filter { it.chance != 0 }
	val previousPools = ArrayList<StrategyPool>()

	var currentPool = StrategyPool(
		criteria = rawStrategies[0].criteria,
		entries = arrayListOf(rawStrategies[0].toEntry(100)),
		id = UUID.nameUUIDFromBytes("StrategyImporter$monsterName$rawGambits".encodeToByteArray()),
	)
	var remainingChance = 100 - rawStrategies[0].chance
	for (index in 1 until rawStrategies.size) {
		val raw = rawStrategies[index]
		if (raw.criteria == currentPool.criteria) {
			currentPool.entries.add(raw.toEntry(remainingChance))
			remainingChance -= currentPool.entries.last().chance
		} else {
			previousPools.add(currentPool)
			currentPool = StrategyPool(
				criteria = raw.criteria,
				entries = arrayListOf(raw.toEntry(100)),
				id = UUID.nameUUIDFromBytes("StrategyImporter$monsterName$rawGambits${previousPools.size}".encodeToByteArray()),
			)
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
	rawProperties: Map<String, String>, actions: List<ActiveSkill>, content: Content
): RawMonsterStrategy {
	val skillName = parseFlashString(rawProperties["command"]!!, "gambit command")!!
	val (action, item) = if (skillName == "Attack") Pair(null, null) else {
		val myAction = actions.find { it.name == skillName }
		if (myAction != null) Pair(myAction, null) else {
			val mimicry = content.skills.classes.find { it.name == "Mimicry" }!!
			val legionAction = mimicry.actions.find { it.name == skillName }
			if (legionAction != null) Pair(legionAction, null) else {
				val item = content.items.items.find { it.displayName == skillName } ?:
						throw SkillParseException("Can't find skill $skillName")
				Pair(null, item)
			}
		}
	}
	val rawTarget = parseFlashString(rawProperties["target"]!!, "gambit target")!!
	val target = StrategyTarget.entries.find { it.raw == rawTarget } ?:
			throw SkillParseException("Unexpected gambit target in $rawProperties")

	var criteria = StrategyCriteria.NONE
	val rawUses = rawProperties["uses"]
	if (rawUses != null) criteria = criteria.merge(StrategyCriteria(maxUses = parseInt(rawUses)))

	val rawOtherTurn = rawProperties["OtherTurn"]
	if (rawOtherTurn == "0") criteria = criteria.merge(StrategyCriteria(canUseOnOddTurns = false))
	if (rawOtherTurn == "1") criteria = criteria.merge(StrategyCriteria(canUseOnEvenTurns = false))

	val rawCriteria = parseActionScriptNestedList(rawProperties["criteria"] ?: "null")
	val rawChance = rawProperties["random"]
	var chance = if (rawChance != null) parseInt(rawChance) else 100

	if (rawCriteria is ArrayList<*> && rawCriteria.size >= 2) {
		val type = parseFlashString(rawCriteria[0].toString(), "gambit criteria type")!!
		if (type == "random") {
			chance = parseInt(rawCriteria[1].toString())
		} else {
			criteria = criteria.merge(when (type) {
				"HP<" -> StrategyCriteria(
					targetHpPercentageAtMost = (100.0 * parseDouble(rawCriteria[1].toString())).roundToInt()
				)

				"HP>" -> StrategyCriteria(
					targetHpPercentageAtLeast = (100.0 * parseDouble(rawCriteria[1].toString())).roundToInt()
				)

				@Suppress("SpellCheckingInspection")
				"MYHP<" -> StrategyCriteria(
					myHpPercentageAtMost = (100.0 * parseDouble(rawCriteria[1].toString())).roundToInt()
				)

				@Suppress("SpellCheckingInspection")
				"MYHP>" -> StrategyCriteria(
					myHpPercentageAtLeast = (100.0 * parseDouble(rawCriteria[1].toString())).roundToInt()
				)

				"has_status" -> StrategyCriteria(
					targetHasEffect = content.stats.statusEffects.find {
						it.flashName == parseFlashString(rawCriteria[1].toString(), "has_status effect")!!
					}!!
				)

				"no_status" -> StrategyCriteria(
					targetMissesEffect = content.stats.statusEffects.find {
						it.flashName == parseFlashString(
							// The BRN status effect referenced by Fire Guardian's Burn move doesn't exist,
							// Tobias probably meant Numbness instead?
							rawCriteria[1].toString().replace("BRN", "NUM"
						), "no_status effect")!!
					}!!
				)

				"resist<" -> StrategyCriteria(
					resistanceAtMost = ElementalResistance(
						content.stats.elements.find { it.rawName == parseFlashString(rawCriteria[1].toString(), "resist element") }!!,
						parseInt(rawCriteria[2].toString()) / 100f
					)
				)

				"resist>" -> StrategyCriteria(
					resistanceAtLeast = ElementalResistance(
						content.stats.elements.find { it.rawName == parseFlashString(rawCriteria[1].toString(), "resist element") }!!,
						parseInt(rawCriteria[2].toString()) / 100f
					)
				)

				"elem=" -> StrategyCriteria(myElement = content.stats.elements.find {
					it.rawName == parseFlashString(rawCriteria[1].toString(), "Element strategy criteria")!!
				}!!)

				"FreeSlots" -> StrategyCriteria(freeAllySlots = parseInt(rawCriteria[1].toString()))

				"corpse" -> StrategyCriteria(targetFainted = if (rawCriteria[1] == "\"p\"") true
						else throw SkillParseException("Unexpected $rawCriteria"))

				else -> throw SkillParseException("Unexpected gambit criteria $rawCriteria")
			})
		}
	} else if (rawCriteria is ArrayList<*> && rawCriteria.size == 1) {
		val criteriaName = parseFlashString(rawCriteria[0].toString(), "criteria")!!
		criteria = when (criteriaName) {
			"NotLastTech" -> criteria.merge(StrategyCriteria(canRepeat = false))
			"alone" -> criteria.merge(StrategyCriteria(freeAllySlots = 3))
			else -> throw SkillParseException("Unexpected raw criteria $rawCriteria")
		}
	} else {
		if (rawCriteria != "null") throw SkillParseException("Unexpected raw criteria $rawCriteria")
	}

	return RawMonsterStrategy(action = action, item = item, target = target, criteria = criteria, chance = chance)
}
