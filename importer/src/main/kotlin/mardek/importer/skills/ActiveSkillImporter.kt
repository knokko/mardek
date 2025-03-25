package mardek.importer.skills

import mardek.content.skill.*
import mardek.content.stats.*
import mardek.importer.area.parseFlashString
import mardek.importer.area.parseOptionalFlashString
import mardek.importer.util.parseActionScriptNestedList
import mardek.importer.util.parseActionScriptObject
import java.lang.Float.parseFloat
import java.lang.Integer.parseInt

fun parseActiveSkills(
	statsContent: StatsContent, rawSkills: List<Map<String, String>>
) = rawSkills.map { rawSkill ->
	val mode = when (rawSkill["MODE"]!!) {
		"\"P\"" -> ActiveSkillMode.Melee
		"\"M\"" -> ActiveSkillMode.Ranged
		"\"S\"" -> ActiveSkillMode.Self
		else -> throw SkillParseException("Unknown skill MODE ${rawSkill["MODE"]}")
	}
	val targetType = when (rawSkill["TT"]) {
		"\"SINGLE\"" -> SkillTargetType.Single
		"\"ANY\"" -> SkillTargetType.Any
		"\"ALL_e\"" -> SkillTargetType.AllEnemies
		"\"ALL\"" -> SkillTargetType.AllEnemies
		"\"SELF\"" -> SkillTargetType.Self
		"\"ALL_p\"" -> SkillTargetType.AllAllies
		else -> throw SkillParseException("Unknown skill TT ${rawSkill["TT"]}")
	}

	val rawSpecial = rawSkill["special"]
	val isHealing = rawSpecial != null && rawSpecial.contains("HEALING")
	var addEffects = rawSkill["stfx"]
	var removeEffects = rawSkill["remove_stfx"]
	if (isHealing && !rawSkill.containsKey("ADD_STFX")) {
		removeEffects = addEffects
		addEffects = null
	}

	var statModifiers = parseStatModifiers(rawSkill["stat_mod"])
	val isBuff = rawSkill.containsKey("buff")
	if (!isBuff && !isHealing) statModifiers = ArrayList(statModifiers.map { -it })

	val rawDelay = rawSkill["dmgdelay"]
	val rawAP = rawSkill["AP"]
	val rawElementalShift = rawSkill["elementalShift"]

	var isElementalShift = false
	if (rawElementalShift != null) {
		val elementList = parseActionScriptNestedList(rawElementalShift)
		isElementalShift = elementList is ArrayList<*> && elementList.isNotEmpty()
	}

	ActiveSkill(
		name = parseFlashString(rawSkill["skill"]!!, "skill name")!!,
		description = if (rawSkill["desc"] != null) parseFlashString(rawSkill["desc"]!!, "skill description")!! else "",
		mode = mode,
		targetType = targetType,
		element = statsContent.elements.find { it.rawName == parseFlashString(rawSkill["elem"]!!, "element") }!!,
		damage = parseSkillDamage(statsContent, rawSkill),
		masteryPoints = if (rawAP != null) parseInt(rawAP) else 0,
		accuracy = if (rawSkill.containsKey("cannot_miss")) 255 else parseInt(rawSkill["accuracy"] ?: "255"),
		manaCost = parseInt(rawSkill["MP"]),
		isHealing = isHealing,
		isBreath = rawSkill.containsKey("BREATH"),
		isBuff = isBuff,
		drainsBlood = rawSpecial != null && rawSpecial.contains("DRAIN"),
		statModifiers = statModifiers,
		addStatusEffects = parseStatusEffectList(statsContent, addEffects),
		removeStatusEffects = parseStatusEffectList(statsContent, removeEffects),
		revive = parseRevive(rawSkill["special"]),
		particleEffect = parseOptionalFlashString(rawSkill["pfx"], "skill particle effect"),
		soundEffect = parseOptionalFlashString(rawSkill["sfx"] ?: rawSkill["delayedSfx"], "skill sound effect"),
		animation = parseOptionalFlashString(rawSkill["anim"], "skill animation"),
		combatRequirement = parseCombatRequirement(rawSkill["menuse"]),
		delay = if (rawDelay == null) 0 else parseInt(rawDelay),
		allParticleEffects = rawSkill.containsKey("ALL_PFX"),
		centered = rawSkill.containsKey("CENTRED"),
		arena = !rawSkill.containsKey("ARENA"),
		rawSongPower = rawSkill["Song"],
		changeElement = isElementalShift,
	)
}

private fun parseCombatRequirement(rawMenuUse: String?): SkillCombatRequirement {
	if (rawMenuUse == null || rawMenuUse == "0") return SkillCombatRequirement.InCombat

	if (rawMenuUse == "1" || rawMenuUse == "true") return SkillCombatRequirement.Always
	if (rawMenuUse == "\"ONLY\"") return SkillCombatRequirement.OutsideCombat

	throw SkillParseException("Unexpected menuse: $rawMenuUse")
}

private fun parseRevive(rawSpecial: String?): Float {
	if (rawSpecial == null) return 0f

	val prefix = "REVIVE:"
	var startIndex = rawSpecial.indexOf(prefix)
	if (startIndex == -1) return 0f
	startIndex += prefix.length

	val endIndex1 = rawSpecial.indexOf(",", startIndex)
	val endIndex2 = rawSpecial.indexOf("}", startIndex)
	val endIndex = if (endIndex1 != -1 && endIndex1 < endIndex2) endIndex1 else endIndex2
	return parseFloat(rawSpecial.substring(startIndex, endIndex))
}

private fun parseStatusEffectList(statsContent: StatsContent, rawEffects: String?): ArrayList<PossibleStatusEffect> {
	if (rawEffects == null) return ArrayList(0)
	if (!rawEffects.startsWith("{") || !rawEffects.endsWith("}")) {
		throw SkillParseException("Expected $rawEffects to start with { and end with }")
	}

	return ArrayList(rawEffects.substring(1 until rawEffects.length - 1).split(",").map { rawEffect ->
		val possiblePair = rawEffect.split(":")
		if (possiblePair.size != 2) throw SkillParseException("Expected $rawEffect to have exactly 1 colon")
		val effect = statsContent.statusEffects.find { it.flashName == possiblePair[0] }!!
		val chance = parseInt(possiblePair[1])
		PossibleStatusEffect(effect, chance)
	})
}

private fun parseStatModifiers(rawModifiers: String?): ArrayList<StatModifierRange> {
	if (rawModifiers == null) return ArrayList(0)
	val modifierMap = parseActionScriptObject(rawModifiers)
	return ArrayList(modifierMap.entries.map { modifierEntry ->
		val stat = CombatStat.entries.find { it.flashName == modifierEntry.key }!!
		val valueList = parseActionScriptNestedList(modifierEntry.value)
		if (valueList is ArrayList<*>) {
			StatModifierRange(stat, parseInt(valueList[0].toString()), parseInt(valueList[1].toString()))
		} else {
			val adder = parseInt(valueList.toString())
			StatModifierRange(stat, adder, adder)
		}
	})
}

private fun parseSkillDamage(statsContent: StatsContent, rawSkill: Map<String, String>): SkillDamage? {
	val rawDamage = rawSkill["DMG"] ?: return null

	var flatAttackValue = 0
	var weaponModifier = 0f
	var levelModifier = 0
	var spiritModifier = SkillSpiritModifier.None
	var crescendoModifier = 0f
	if (rawSkill.containsKey("USES_SPR")) spiritModifier = SkillSpiritModifier.SpiritBlade

	val nestedDamage = parseActionScriptNestedList(rawDamage)
	if (nestedDamage is String) flatAttackValue = parseInt(nestedDamage)
	if (nestedDamage is List<*>) {
		if (nestedDamage.size <= 1) throw SkillParseException("Unexpected DMG $rawDamage")
		if (nestedDamage[0] == "\"SPECIAL\"") {
			if (nestedDamage[1] == "\"SPR_CALC\"") {
				if (nestedDamage.size != 3) throw SkillParseException("Unexpected SPR DMG $rawDamage")
				if (nestedDamage[2] == "1") spiritModifier = SkillSpiritModifier.DivineGlory
				if (nestedDamage[2] == "-2") spiritModifier = SkillSpiritModifier.LayOnHands
			}
			if (nestedDamage[1] == "\"SPR_AS_DAMAGE\"") spiritModifier = SkillSpiritModifier.GreenLightning
		} else {
			if (nestedDamage[0] == "\"m\"") weaponModifier = parseFloat(nestedDamage[1] as String)
			else if (nestedDamage[0] == "\"c\"") flatAttackValue = parseInt(nestedDamage[1] as String)
			else throw SkillParseException("Unexpected first DMG $rawDamage")

			if (nestedDamage.size >= 3 && nestedDamage[2] is String && nestedDamage[2] != "null") {
				if (nestedDamage[0] == "\"c\"") {
					var rawLevelModifier = nestedDamage[2] as String
					if (rawLevelModifier != "null") {
						if (!rawLevelModifier.contains("L")) throw SkillParseException("Unexpected level DMG $rawDamage")
						if (!rawLevelModifier.startsWith("\"") || !rawLevelModifier.endsWith("\"")) {
							throw SkillParseException("Expected level modifier to be surrounded by double quotes, but got $rawDamage")
						}
						rawLevelModifier = rawLevelModifier.substring(1 until rawLevelModifier.length - 1)

						levelModifier = if (rawLevelModifier == "L") 1
						else {
							if (!rawLevelModifier.startsWith("L*")) {
								throw SkillParseException("Unexpected level DMG expression $rawDamage")
							}
							parseInt(rawLevelModifier.substring(2))
						}
					}
				}
				if (nestedDamage[0] == "\"m\"") {
					flatAttackValue += parseInt(nestedDamage[2].toString())
				}
			}

			if (nestedDamage.size >= 4) {
				if (nestedDamage[3] !is ArrayList<*>) throw SkillParseException("Unexpected DMG $rawDamage")
				val crescendoList = nestedDamage[3] as ArrayList<*>
				if (crescendoList.size != 2 || crescendoList[0] != "\"crescendo\"") {
					throw SkillParseException("Unexpected fourth element in DMG $rawDamage")
				}
				crescendoModifier = parseFloat(crescendoList[1] as String)
			}
		}
	}

	val elementalBonus = ArrayList<ElementalDamageBonus>(0)
	val rawElementalBonus = rawSkill["elemBonus"]
	if (rawElementalBonus != null) {
		val nestedBonus = parseActionScriptNestedList(rawElementalBonus)
		for (rawBonus in nestedBonus as ArrayList<*>) {
			val rawBonusList = rawBonus as ArrayList<*>
			if (rawBonusList.size != 2) throw SkillParseException("Unexpected raw bonus in $nestedBonus")
			val element = statsContent.elements.find {
				it.rawName == parseFlashString(rawBonusList[0] as String, "bonus element")
			}!!
			val damageList = rawBonusList[1] as ArrayList<*>
			if (damageList.size != 2) throw SkillParseException("Unexpected damage modifier in raw bonus $rawBonusList")
			if (damageList[0] != "\"m\"") throw SkillParseException("Unexpected damage modifier type in raw bonus $rawBonusList")
			elementalBonus.add(ElementalDamageBonus(element = element, modifier = parseFloat(damageList[1] as String)))
		}
	}

	val rawSpecial = rawSkill["special"]
	val rawCritical = rawSkill["critical"]
	return SkillDamage(
		flatAttackValue = flatAttackValue,
		weaponModifier = weaponModifier,
		levelModifier = levelModifier,
		splitDamage = !rawSkill.containsKey("no_dmg_split"),
		ignoresDefense = rawSkill.containsKey("ignore_def"),
		ignoresShield = rawSkill.containsKey("ignore_shield"),
		spiritModifier = spiritModifier,
		bonusAgainstElements = elementalBonus,
		moneyModifier = if (rawDamage == "[\"SPECIAL\",\"money\"]") 1f else 0f,
		gemModifier = if (rawDamage == "[\"SPECIAL\",\"gem\"]") 1f else 0f,
		lostHealthModifier = if (rawDamage == "[\"SPECIAL\",\"hplost\"]") 1f else 0f,
		statusEffectModifier = if (rawSpecial != null && rawSpecial.contains("COUP_DE_GRACE")) 1f else 0f,
		killCountModifier = if (rawDamage == "[\"SPECIAL\",\"num_kills\"]") 1f else 0f,
		hardcodedDamage = if (rawDamage == "[\"SPECIAL\",\"set_dmg\",1000]") 1000 else 0,
		remainingTargetHpModifier = if (rawDamage == "[\"SPECIAL\",\"HP%\",0.5]") 0.5f else 0f,
		potionModifier = if (rawDamage == "[\"SPECIAL\",\"potion\"]") 0.5f else 0f,
		crescendoModifier = crescendoModifier,
		critChance = if (rawCritical != null) parseInt(rawCritical) else null
	)
}
