package mardek.importer.inventory

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import mardek.content.Content
import mardek.content.stats.*
import mardek.content.inventory.*
import mardek.content.skill.*
import mardek.importer.area.parseFlashString
import mardek.importer.util.parseActionScriptNestedList
import mardek.importer.util.parseActionScriptObject
import mardek.importer.util.parseActionScriptObjectList
import java.lang.Float.parseFloat
import java.lang.Integer.parseInt
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

internal fun importItems(content: Content, rawItems: String) {
	for (rawItem in parseActionScriptObjectList(rawItems)) {
		val rawElement = rawItem["elem"]
		val typeName = parseFlashString(rawItem["type"]!!, "item type")!!
		if (typeName == "rods" || typeName == "bait" || typeName == "fish") continue
		if (typeName == "gold" || typeName == "none") continue

		val rawCost = parseInt(rawItem["cost"])
		val cost = if (rawCost >= 0) rawCost else null
		val flashName = parseFlashString(rawItem["name"]!!, "item name")!!
		val description = parseFlashString(rawItem["desc"]!!, "item description")!!
		val element = if (rawElement != null) content.stats.elements.find {
			it.rawName == parseFlashString(rawElement, "item element")!!
		}!! else null

		if (typeName == "plot") {
			content.items.plotItems.add(PlotItem(
				name = flashName, description = description, element = element, cost = cost
			))
			continue
		}
		content.items.items.add(Item(
			flashName = flashName,
			description = description,
			type = content.items.itemTypes.find { it.flashName == typeName }!!,
			element = element,
			cost = cost,
			equipment = parseEquipment(content, rawItem),
			consumable = parseConsumable(content, rawItem),
		))
	}
}

private fun parseEquipment(content: Content, rawItem: Map<String, String>): EquipmentProperties? {
	val type = rawItem["type"]
	if (!rawItem.containsKey("wpnType") && !rawItem.containsKey("amrType") && type != "\"gems\"" && type != "\"accs\"") {
		return null
	}

	val stats = ArrayList<StatModifier>(0)
	val autoEffects = ArrayList<StatusEffect>(0)
	val elementalBonuses = ArrayList<ElementalDamageBonus>(0)
	val elementalResistances = ArrayList<ElementalResistance>(0)
	val statusResistances = ArrayList<EffectResistance>(0)
	var charismaticPerformanceChance = 0

	val rawEffects = rawItem["effects"]
	if (rawEffects != null) {
		val nestedEffects = parseActionScriptNestedList(rawEffects)
		if (nestedEffects !is ArrayList<*>) throw ItemParseException("Unexpected item effects $nestedEffects")
		for (effectPair in nestedEffects) {
			if (effectPair !is ArrayList<*> || effectPair.size < 2 || effectPair.size > 3) {
				throw ItemParseException("Unexpected item effects $rawEffects")
			}

			val rawName = parseFlashString(effectPair[0] as String, "effect key")!!

			val stat = CombatStat.entries.find { it.flashName == rawName }
			if (stat != null) {
				stats.add(StatModifier(stat, parseInt(effectPair[1] as String)))
				continue
			}

			if (rawName == "AUTO_STFX") {
				val effectName = parseFlashString(effectPair[1] as String, "auto-effect name")!!
				autoEffects.add(content.stats.statusEffects.find { it.flashName == effectName }!!)
				continue
			}

			if (rawName == "R_ELEM" || rawName == "EMPOWER") {
				val elementName = parseFlashString(effectPair[1] as String, "element name")!!
				val element = content.stats.elements.find { it.rawName == elementName }!!
				val modifier = parseInt(effectPair[2] as String) / 100f

				if (rawName == "R_ELEM") {
					elementalResistances.add(ElementalResistance(element, modifier))
				} else {
					elementalBonuses.add(ElementalDamageBonus(element, modifier))
				}
				continue
			}

			if (rawName == "R_STATUS") {
				val statusName = parseFlashString(effectPair[1] as String, "status resist name")!!
				val statusEffect = content.stats.statusEffects.find { it.flashName == statusName }!!
				statusResistances.add(EffectResistance(statusEffect, parseInt(effectPair[2] as String)))
			}

			if (rawName == "CHARISMATIC") {
				val modifier = parseFloat(effectPair[1] as String)
				charismaticPerformanceChance += (100f * modifier).roundToInt()
			}
		}
	}

	for (candidate in arrayOf("atk", "def", "mdef")) {
		val rawValue = rawItem[candidate]
		val stat = CombatStat.entries.find { it.flashName == candidate.uppercase(Locale.ROOT) }!!
		if (rawValue != null && rawValue != "0") stats.add(StatModifier(stat, parseInt(rawValue)))
	}

	var rawArmorType = rawItem["amrType"]
	if (rawArmorType == "3") rawArmorType = "\"Ar3\""
	val armorType = if (rawArmorType != null) content.items.armorTypes.find {
		it.key == parseFlashString(rawArmorType, "armor type")
	}!! else null

	val rawOnlyUser = rawItem["only_user"]

	return EquipmentProperties(
		skills = parseSkills(content.skills, rawItem["skills"]),
		stats = stats,
		elementalBonuses = elementalBonuses,
		resistances = Resistances(elementalResistances, statusResistances),
		autoEffects = autoEffects,
		weapon = parseWeaponProperties(content, rawItem),
		armorType = armorType,
		gem = parseGemProperties(content.stats, rawItem),
		onlyUser = if (rawOnlyUser != null) parseFlashString(rawOnlyUser, "only_user")!! else null,
		charismaticPerformanceChance = charismaticPerformanceChance,
	)
}

private fun parseSkills(skillsContent: SkillsContent, rawSkills: String?): ArrayList<Skill> {
	val skills = ArrayList<Skill>(0)
	if (rawSkills != null) {
		val nestedSkills = parseActionScriptNestedList(rawSkills)
		if (nestedSkills !is ArrayList<*>) throw ItemParseException("Unexpected item skills $rawSkills")
		for (skillPair in nestedSkills) {
			if (skillPair !is ArrayList<*> || skillPair.size != 2) throw ItemParseException("Unexpected item skills $rawSkills")
			val rawCategory = parseFlashString(skillPair[0] as String, "skill category")!!
			val skillName = parseFlashString(skillPair[1] as String, "skill name")!!
			if (rawCategory == "R:PASSIVE") {
				if (skillName == "Absorb MP") skills.add(skillsContent.reactionSkills.find { it.name == skillName }!!)
				else skills.add(skillsContent.passiveSkills.find { it.name == skillName }!!)
			} else if (rawCategory.startsWith("R:")) {
				val reactionType = ReactionSkillType.fromString(rawCategory.substring(2))
				skills.add(skillsContent.reactionSkills.find {
					it.type == reactionType && it.name == skillName
				}!!)
			} else {
				skills.add(skillsContent.classes.find {
					it.key == rawCategory
				}!!.actions.find { it.name == skillName }!!)
			}
		}
	}
	return skills
}

private fun parseWeaponProperties(content: Content, rawItem: Map<String, String>): WeaponProperties? {
	val rawWeaponType = rawItem["wpnType"] ?: return null
	val weaponType = content.items.weaponTypes.find { it.flashName == parseFlashString(rawWeaponType, "weapon type") }!!

	val creatureBonuses = ArrayList<CreatureTypeBonus>(0)
	val rawRaceBonuses = rawItem["typeBonus"]
	if (rawRaceBonuses != null) {
		for ((rawRace, rawBonus) in parseActionScriptObject(rawRaceBonuses)) {
			val race = content.stats.creatureTypes.find { it.flashName == rawRace }!!
			val bonusFraction = parseInt(rawBonus) - 1f
			creatureBonuses.add(CreatureTypeBonus(race, bonusFraction))
		}
	}

	val elementalBonuses = ArrayList<ElementalDamageBonus>(0)
	val rawElementalBonuses = rawItem["elemBonus"]
	if (rawElementalBonuses != null) {
		for ((rawElement, rawBonus) in parseActionScriptObject(rawElementalBonuses)) {
			val element = content.stats.elements.find { it.rawName == rawElement }!!
			val modifier = parseFloat(rawBonus) - 1f
			elementalBonuses.add(ElementalDamageBonus(element, modifier))
		}
	}

	val addEffects = ArrayList<PossibleStatusEffect>(0)
	val rawEffects = rawItem["stfx"]
	if (rawEffects != null) {
		for ((rawEffect, rawChance) in parseActionScriptObject(rawEffects)) {
			val effect = content.stats.statusEffects.find { it.flashName == rawEffect }!!
			addEffects.add(PossibleStatusEffect(effect, parseInt(rawChance)))
		}
	}

	val rawSound = rawItem["hit_sfx"]
	val hitSound = if (rawSound != null) {
		val soundName = parseFlashString(rawSound, "weapon hit sound")!!
		if (soundName == "punch") content.audio.fixedEffects.battle.punch
		else content.audio.effects.find { it.flashName == soundName }!!
	} else null

	return WeaponProperties(
		type = weaponType,
		critChance = parseInt(rawItem["critical"]),
		hitChance = parseInt(rawItem["hit"]),
		hpDrain = if (rawItem["HP_DRAIN"] == "true") 1f else 0f,
		mpDrain = if (rawItem["MP_DRAIN"] == "true") 1f else 0f,
		effectiveAgainstCreatureTypes = creatureBonuses,
		effectiveAgainstElements = elementalBonuses,
		addEffects = addEffects,
		hitSound = hitSound
	)
}

private fun parseGemProperties(statsContent: StatsContent, rawItem: Map<String, String>): GemProperties? {
	if (rawItem["type"] != "\"gems\"") return null

	val rawGem = parseActionScriptNestedList(rawItem["spell"]!!)
	if (rawGem !is ArrayList<*> || rawGem.size < 2 || rawGem.size > 4) throw ItemParseException("Unexpected gem $rawGem")

	val inflictStatusEffects = ArrayList<PossibleStatusEffect>(0)
	if (rawGem.size >= 3) {
		val rawEffects = rawGem[2]
		if (rawEffects is String && rawEffects != "null") {
			for ((effectName, rawChance) in parseActionScriptObject(rawEffects)) {
				val effect = statsContent.statusEffects.find { it.flashName == effectName }!!
				inflictStatusEffects.add(PossibleStatusEffect(effect, parseInt(rawChance)))
			}
		}
	}

	return GemProperties(
		power = parseInt(rawGem[1] as String),
		particleEffect = "gemsplosion" + parseFlashString(rawGem[0] as String, "raw gem name")!!,
		inflictStatusEffects = inflictStatusEffects,
		drainHp = if (rawGem.size == 4 && rawGem[3].toString().contains("DRAIN:1")) 1f else 0f
	)
}

private fun parseConsumable(content: Content, rawItem: Map<String, String>): ConsumableProperties? {
	val rawAction = rawItem["action"]
	val rawRgb = rawItem["rgb"]
	if (rawAction == null && rawRgb == null) return null
	if (rawAction == null || rawRgb == null || rawItem["type"] != "\"item\"") {
		throw ItemParseException("Unexpected consumable $rawItem")
	}

	val nestedRgb = parseActionScriptNestedList(rawRgb)
	if (nestedRgb !is ArrayList<*> || nestedRgb.size != 3) throw ItemParseException("Unexpected consumable RGB $rawRgb")

	var particleEffect = rawItem["pfx"]
	var restoreHealth = 0
	var restoreMana = 0
	val addStatusEffects = ArrayList<PossibleStatusEffect>(0)
	val removeStatusEffects = ArrayList<PossibleStatusEffect>(0)
	var removeNegativeStatusEffects = false
	val statModifiers = ArrayList<StatModifierRange>(0)
	var damage: ConsumableDamage? = null

	if (!rawAction.startsWith("[\"") || !rawAction.endsWith("]")) {
		throw ItemParseException("Unexpected raw action $rawAction")
	}
	val actionType = rawAction.substring(2, rawAction.indexOf('"', 2))

	if (actionType == "r_HP") restoreHealth = parseInt(rawAction.substring(8, rawAction.length - 1))
	if (actionType == "r_MP") restoreMana = parseInt(rawAction.substring(8, rawAction.length - 1))

	if (actionType == "h_status") {
		val rawStatusHeals = rawAction.substring(12, rawAction.length - 1)
		val nestedStatusHeals = parseActionScriptNestedList(rawStatusHeals)
		@Suppress("UNCHECKED_CAST")
		val nameList = if (nestedStatusHeals is String) {
			listOf(nestedStatusHeals)
		} else nestedStatusHeals as ArrayList<String>

		for (name in nameList) {
			if (name == "\"ALL_BAD\"") removeNegativeStatusEffects = true
			else {
				val effect = content.stats.statusEffects.find {
					it.flashName == parseFlashString(name, "heal status name")
				}!!
				removeStatusEffects.add(PossibleStatusEffect(effect, 100))
			}
		}
	}

	if (actionType == "spell") {
		val rawSpell = parseActionScriptObject(rawAction.substring(9, rawAction.length - 1))
		particleEffect = rawSpell["pfx"]

		val rawStatusEffects = rawSpell["stfx"]
		if (rawStatusEffects != null) {
			for ((rawEffect, rawChance) in parseActionScriptObject(rawStatusEffects)) {
				val effect = content.stats.statusEffects.find { it.flashName == rawEffect }!!
				addStatusEffects.add(PossibleStatusEffect(effect, parseInt(rawChance)))
			}
		}

		val rawStatModifiers = rawSpell["stat_mod"]
		if (rawStatModifiers != null) {
			for ((statName, rawAdder) in parseActionScriptObject(rawStatModifiers)) {
				val stat = CombatStat.entries.find { it.flashName == statName }!!
				val adder = parseInt(rawAdder)
				statModifiers.add(StatModifierRange(stat, adder, adder))
			}
		}

		if (rawItem["hurtful"] == "true") {
			val power = parseInt(rawSpell["pow"])
			val spirit = parseInt(rawSpell["SPR"])
			if (power != 0) {
				val element = content.stats.elements.find {
					it.rawName == parseFlashString(rawSpell["elem"]!!, "consumable damage element")
				}!!
				damage = ConsumableDamage(power, spirit, element)
			}
		}

		val hardcodedDamage = rawSpell["set_dmg"]
		if (hardcodedDamage != null) {
			if (rawSpell["special"] != "{HEALING:1}") throw ItemParseException("Unexpected $rawItem")
			if (rawSpell["affectMP"] == "true") restoreMana = -parseInt(hardcodedDamage)
			else restoreHealth = -parseInt(hardcodedDamage)
		}
	}

	var particleName = if (rawItem["name"] == "\"Oxyale\"") null
	else parseFlashString(particleEffect!!, "consumable pfx")!!
	if (particleName == "Remedy") particleName = "cleanse"
	if (particleName == "rainbow") particleName = null
	return ConsumableProperties(
			particleEffect = if (particleName != null) content.battle.particles.find { it.name == particleName }!! else null,
			blinkColor = rgb(
					parseInt(nestedRgb[0] as String),
					parseInt(nestedRgb[1] as String),
					parseInt(nestedRgb[2] as String)
			),
			isFullCure = actionType == "fullcure",
			restoreHealth = restoreHealth,
			restoreMana = restoreMana,
			revive = if (actionType == "life") parseFloat(rawAction.substring(8, rawAction.length - 1)) else 0f,
			addStatusEffects = addStatusEffects,
			removeStatusEffects = removeStatusEffects,
			removeNegativeStatusEffects = removeNegativeStatusEffects,
			statModifiers = statModifiers,
			damage = damage
	)
}
