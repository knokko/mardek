package mardek.importer.skills

import mardek.assets.combat.CombatAssets
import mardek.assets.combat.PossibleStatusEffect
import mardek.assets.combat.StatModifier
import mardek.assets.combat.StatusEffect
import mardek.assets.skill.ElementalDamageBonus
import mardek.assets.skill.PassiveSkill
import mardek.assets.skill.SkillAssets
import mardek.assets.skill.SkillClass
import mardek.importer.area.parseFlashString
import mardek.importer.util.parseActionScriptObject
import java.lang.Float.parseFloat
import java.lang.Integer.parseInt

fun parsePassiveSkill(
		combatAssets: CombatAssets, skillAssets: SkillAssets, rawSkill: Map<String, String>
): PassiveSkill {

	val rawEffect = rawSkill["effect"]
	val effect = if (rawEffect != null) parseActionScriptObject(rawEffect) else null

	var hpModifier = 0f
	var mpModifier = 0f

	if (effect != null) {
		val rawHp = effect["hpmult"]
		val rawMp = effect["mpmult"]
		if (rawHp != null) hpModifier = parseFloat(rawHp)
		if (rawMp != null) mpModifier = parseFloat(rawMp)
	}

	val statModifiers = ArrayList<StatModifier>()
	val elementalResistances = ArrayList<ElementalDamageBonus>()
	val statusResistances = ArrayList<PossibleStatusEffect>()
	val autoEffects = HashSet<StatusEffect>()
	val sosEffects = HashSet<StatusEffect>()

	if (effect != null) {
		val rawStatModifiers = effect["statmod"]
		if (rawStatModifiers != null) {
			for ((statName, rawAdder) in parseActionScriptObject(rawStatModifiers)) {
				val stat = combatAssets.stats.find { it.flashName == statName }!!
				statModifiers.add(StatModifier(stat, parseInt(rawAdder)))
			}
		}

		val rawResistances = effect["RESIST"]
		if (rawResistances != null) {
			for ((resistName, rawModifier) in parseActionScriptObject(rawResistances)) {
				val element = combatAssets.elements.find { it.rawName == resistName }
				if (element != null) {
					elementalResistances.add(ElementalDamageBonus(element, parseInt(rawModifier) / 100f))
				} else {
					val statusEffect = combatAssets.statusEffects.find { it.flashName == resistName }!!
					statusResistances.add(PossibleStatusEffect(statusEffect, parseInt(rawModifier)))
				}
			}
		}

		val rawAutoEffects = effect["autoSTFX"]
		if (rawAutoEffects != null) {
			for (effectName in parseActionScriptObject(rawAutoEffects).keys) {
				autoEffects.add(combatAssets.statusEffects.find { it.flashName == effectName }!!)
			}
		}

		val rawSosEffects = effect["SOS"]
		if (rawSosEffects != null) {
			for (effectName in parseActionScriptObject(rawSosEffects).keys) {
				sosEffects.add(combatAssets.statusEffects.find { it.flashName == effectName }!!)
			}
		}
	}

	var experienceModifier = 0f
	var masteryModifier = 0
	var goldModifier = 0
	var addLootChance = 0
	var skillClass: SkillClass? = null

	if (effect != null) {
		val rawExperience = effect["expmult"]
		val rawMastery = effect["apmult"]
		val rawGold = effect["goldmult"]
		val rawLoot = effect["lootmod"]

		if (rawExperience != null) experienceModifier = parseFloat(rawExperience)
		if (rawMastery != null) masteryModifier = parseInt(rawMastery) - 1
		if (rawGold != null) goldModifier = parseInt(rawGold)
		if (rawLoot != null) addLootChance = parseInt(rawLoot)
	}

	val rawOnly = rawSkill["only"]
	if (rawOnly != null) {
		val onlyMap = parseActionScriptObject(rawOnly)
		if (onlyMap.size != 1) throw SkillParseException("Unexpected only: $onlyMap")
		val key = onlyMap.keys.first()
		if (onlyMap[key] != "true") throw SkillParseException("Unexpected only: $onlyMap")
		skillClass = skillAssets.classes.find { it.key == key }!!
	}

	return PassiveSkill(
			name = parseFlashString(rawSkill["skill"]!!, "passive skill name")!!,
			description = parseFlashString(rawSkill["desc"]!!, "passive skill description")!!,
			element = combatAssets.elements.find {
				it.rawName == parseFlashString(rawSkill["elem"]!!, "passive skill element")
			}!!,
			masteryPoints = parseInt(rawSkill["AP"]),
			enablePoints = parseInt(rawSkill["RP"]),
			hpModifier = hpModifier,
			mpModifier = mpModifier,
			statModifiers = statModifiers,
			elementalResistances = elementalResistances,
			statusResistances = statusResistances,
			autoEffects = autoEffects,
			sosEffects = sosEffects,
			experienceModifier = experienceModifier,
			masteryModifier = masteryModifier,
			goldModifier = goldModifier,
			addLootChance = addLootChance,
			skillClass = skillClass,
	)
}
