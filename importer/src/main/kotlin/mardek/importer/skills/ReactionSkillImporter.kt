package mardek.importer.skills

import mardek.content.Content
import mardek.content.stats.CreatureTypeBonus
import mardek.content.stats.ElementalDamageBonus
import mardek.content.stats.PossibleStatusEffect
import mardek.content.skill.*
import mardek.importer.area.parseFlashString
import mardek.importer.util.parseActionScriptObject
import mardek.importer.util.parseActionScriptObjectList
import org.joml.Math.clamp
import java.lang.Float.parseFloat
import java.lang.Integer.parseInt
import java.util.UUID
import kotlin.math.roundToInt

fun parseReactionSkillsAndPassiveSkills(content: Content, rawSkills: String) {
	val reactionTypeMap = parseActionScriptObject(rawSkills)
	for ((key, type) in arrayOf(
		Pair("P_ATK", ReactionSkillType.MeleeAttack),
		Pair("P_DEF", ReactionSkillType.MeleeDefense),
		Pair("M_ATK", ReactionSkillType.RangedAttack),
		Pair("M_DEF", ReactionSkillType.RangedDefense)
	)) {
		val rawTypeSkills = parseActionScriptObjectList(reactionTypeMap[key]!!)
		for (rawSkill in rawTypeSkills) {
			content.skills.reactionSkills.add(parseReactionSkill(content, rawSkill, type))
		}
	}

	for (rawSkill in parseActionScriptObjectList(reactionTypeMap["PASSIVE"]!!)) {
		content.skills.passiveSkills.add(parsePassiveSkill(content.stats, content.skills, rawSkill))
	}
}

private fun parseReactionSkill(
	content: Content, rawSkill: Map<String, String>, type: ReactionSkillType
): ReactionSkill {

	var skillClass: SkillClass? = null
	val rawSkillClass = rawSkill["only"]
	if (rawSkillClass != null) {
		val onlyMap = parseActionScriptObject(rawSkillClass)
		if (onlyMap.size != 1) throw SkillParseException("Unexpected only: $onlyMap in $rawSkill")

		val classKey = onlyMap.keys.first()
		if (onlyMap[classKey]!! != "true") throw SkillParseException("Unexpected only: $onlyMap in $rawSkill")
		skillClass = content.skills.classes.find { it.key == classKey}!!
	}

	val soulstrike = rawSkill["elem"] == "\"_USERS\""
	val element = if (soulstrike) content.stats.elements.find {
		it.properName == "AETHER"
	}!! else content.stats.elements.find {
		it.rawName == parseFlashString(rawSkill["elem"]!!, "reaction skill element")
	}!!

	val rawEffect = rawSkill["effect"]
	val effect = if (rawEffect != null) parseActionScriptObject(rawEffect) else emptyMap()

	var addFlatDamage = 0
	var addDamageFraction = 0f
	var addCritChance = 0
	var addAccuracy = 0
	var drainHp = 0f
	var absorbMp = 0f

	if (effect.containsKey("dmgadd")) addFlatDamage = parseInt(effect["dmgadd"])
	if (effect.containsKey("dmgmult")) addDamageFraction = parseFloat(effect["dmgmult"])
	if (effect.containsKey("critical")) addCritChance += parseInt(effect["critical"])
	if (effect.containsKey("accuracy")) addAccuracy = parseInt(effect["accuracy"])
	if (effect.containsKey("evade")) addAccuracy -= parseInt(effect["evade"])
	if (effect.containsKey("drainHP")) drainHp = parseFloat(effect["drainHP"])
	if (effect.containsKey("absorbMP")) absorbMp = parseFloat(effect["absorbMP"])

	val elementalBonuses = ArrayList<ElementalDamageBonus>()
	val addStatusEffects = ArrayList<PossibleStatusEffect>()
	val removeStatusEffects = ArrayList<PossibleStatusEffect>()
	val effectiveAgainst = ArrayList<CreatureTypeBonus>()

	fun parseStatusEffects(raw: String?, dest: MutableList<PossibleStatusEffect>) {
		if (raw != null) {
			val statusMap = parseActionScriptObject(raw)
			for ((name, rawChance) in statusMap) {
				val statusEffect = content.stats.statusEffects.find { it.flashName == name }!!
				val chance = clamp(-100, 100, (100 * parseFloat(rawChance)).roundToInt())
				dest.add(PossibleStatusEffect(statusEffect, chance))
			}
		}
	}

	parseStatusEffects(effect["stfx_inflict"], addStatusEffects)
	parseStatusEffects(effect["stfx_remove"], removeStatusEffects)

	val rawRaceBonus = effect["typeBonus"]
	if (rawRaceBonus != null) {
		val raceMap = parseActionScriptObject(rawRaceBonus)
		for ((raceName, rawModifier) in raceMap) {
			val race = content.stats.creatureTypes.find { it.flashName == raceName }!!
			effectiveAgainst.add(CreatureTypeBonus(race, parseFloat(rawModifier)))
		}
	}

	val rawElementalResistances = effect["RESIST"]
	if (rawElementalResistances != null) {
		val resistanceMap = parseActionScriptObject(rawElementalResistances)
		for ((elementName, resistance) in resistanceMap) {
			val resistedElement = content.stats.elements.find { it.rawName == elementName }!!
			elementalBonuses.add(ElementalDamageBonus(resistedElement, parseInt(resistance) / -100f))
		}
	}

	val rawElementalBoosts = effect["EMPOWER"]
	if (rawElementalBoosts != null) {
		val boostMap = parseActionScriptObject(rawElementalBoosts)
		for ((elementName, boost) in boostMap) {
			val resistedElement = content.stats.elements.find { it.rawName == elementName }!!
			elementalBonuses.add(ElementalDamageBonus(resistedElement, parseInt(boost) / 100f))
		}
	}

	val name = parseFlashString(rawSkill["skill"]!!, "reaction skill name")!!
	val description = parseFlashString(rawSkill["desc"]!!, "reaction skill description")!!
	return ReactionSkill(
		name = name,
		description = description,
		type = type,
		element = element,
		masteryPoints = parseInt(rawSkill["AP"]!!),
		enablePoints = parseInt(rawSkill["RP"]!!),
		skillClass = skillClass,
		addFlatDamage = addFlatDamage,
		addDamageFraction = addDamageFraction,
		addCritChance = addCritChance,
		addAccuracy = addAccuracy,
		drainHp = drainHp,
		absorbMp = absorbMp,
		elementalBonuses = elementalBonuses,
		addStatusEffects = addStatusEffects,
		removeStatusEffects = removeStatusEffects,
		effectiveAgainst = effectiveAgainst,
		smitePlus = effect.containsKey("smitePLUS"),
		soulStrike = soulstrike,
		survivor = effect.containsKey("survivor"),
		id = UUID.nameUUIDFromBytes("ReactionSkillImporter$name$description".encodeToByteArray()),
	)
}
