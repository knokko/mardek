package mardek.importer.inventory

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import mardek.assets.combat.*
import mardek.assets.inventory.*
import mardek.assets.skill.*
import mardek.importer.area.parseFlashString
import mardek.importer.util.parseActionScriptNestedList
import mardek.importer.util.parseActionScriptObject
import mardek.importer.util.parseActionScriptObjectList
import java.lang.Float.parseFloat
import java.lang.Integer.parseInt
import java.util.*
import kotlin.collections.ArrayList

internal fun importItems(
		combatAssets: CombatAssets, skillAssets: SkillAssets, assets: InventoryAssets, rawItems: String
) {
	for (rawItem in parseActionScriptObjectList(rawItems)) {
		val rawElement = rawItem["elem"]
		val typeName = parseFlashString(rawItem["type"]!!, "item type")!!
		if (typeName == "rods" || typeName == "bait" || typeName == "fish") continue
		if (typeName == "gold" || typeName == "none") continue
		val cost = parseInt(rawItem["cost"])
		assets.items.add(Item(
				flashName = parseFlashString(rawItem["name"]!!, "item name")!!,
				description = parseFlashString(rawItem["desc"]!!, "item description")!!,
				type = assets.itemTypes.find { it.flashName == typeName }!!,
				element = if (rawElement != null) combatAssets.elements.find {
					it.rawName == parseFlashString(rawElement, "item element")!!
				}!! else null,
				cost = if (cost >= 0) cost else null,
				equipment = parseEquipment(combatAssets, skillAssets, assets, rawItem),
				consumable = parseConsumable(combatAssets, rawItem),
		))
	}
}

private fun parseEquipment(
		combatAssets: CombatAssets, skillAssets: SkillAssets,
		assets: InventoryAssets, rawItem: Map<String, String>
): EquipmentProperties? {
	val type = rawItem["type"]
	if (!rawItem.containsKey("wpnType") && !rawItem.containsKey("amrType") && type != "\"gems\"" && type != "\"accs\"") {
		return null
	}

	val stats = ArrayList<StatModifier>(0)
	val autoEffects = ArrayList<StatusEffect>(0)
	val elementalBonuses = ArrayList<ElementalDamageBonus>(0)
	val elementalResistances = ArrayList<ElementalDamageBonus>(0)
	val statusResistances = ArrayList<PossibleStatusEffect>(0)

	val rawEffects = rawItem["effects"]
	if (rawEffects != null) {
		val nestedEffects = parseActionScriptNestedList(rawEffects)
		if (nestedEffects !is ArrayList<*>) throw ItemParseException("Unexpected item effects $nestedEffects")
		for (effectPair in nestedEffects) {
			if (effectPair !is ArrayList<*> || effectPair.size < 2 || effectPair.size > 3) {
				throw ItemParseException("Unexpected item effects $rawEffects")
			}

			val rawName = parseFlashString(effectPair[0] as String, "effect key")!!

			val stat = combatAssets.stats.find { it.flashName == rawName }
			if (stat != null) {
				stats.add(StatModifier(stat, parseInt(effectPair[1] as String)))
				continue
			}

			if (rawName == "AUTO_STFX") {
				val effectName = parseFlashString(effectPair[1] as String, "auto-effect name")!!
				autoEffects.add(combatAssets.statusEffects.find { it.flashName == effectName }!!)
				continue
			}

			if (rawName == "R_ELEM" || rawName == "EMPOWER") {
				val elementName = parseFlashString(effectPair[1] as String, "element name")!!
				val element = combatAssets.elements.find { it.rawName == elementName }!!
				val modifier = parseInt(effectPair[2] as String) / 100f

				if (rawName == "R_ELEM") {
					elementalResistances.add(ElementalDamageBonus(element, -modifier))
				} else {
					elementalBonuses.add(ElementalDamageBonus(element, modifier))
				}
				continue
			}

			if (rawName == "R_STATUS") {
				val statusName = parseFlashString(effectPair[1] as String, "status resist name")!!
				val statusEffect = combatAssets.statusEffects.find { it.flashName == statusName }!!
				statusResistances.add(PossibleStatusEffect(statusEffect, parseInt(effectPair[2] as String)))
			}
		}
	}

	for (candidate in arrayOf("atk", "def", "mdef")) {
		val rawValue = rawItem[candidate]
		val stat = combatAssets.stats.find { it.flashName == candidate.uppercase(Locale.ROOT) }!!
		if (rawValue != null && rawValue != "0") stats.add(StatModifier(stat, parseInt(rawValue)))
	}

	var rawArmorType = rawItem["amrType"]
	if (rawArmorType == "3") rawArmorType = "\"Ar3\""
	val armor = if (rawArmorType != null) ArmorProperties(
			type = assets.armorTypes.find { it.key == parseFlashString(rawArmorType, "armor type") }!!
	) else null

	val rawOnlyUser = rawItem["only_user"]

	return EquipmentProperties(
			skills = parseSkills(skillAssets, rawItem["skills"]),
			stats = stats,
			elementalBonuses = elementalBonuses,
			elementalResistances = elementalResistances,
			statusResistances = statusResistances,
			autoEffects = autoEffects,
			weapon = parseWeaponProperties(combatAssets, assets, rawItem),
			armor = armor,
			gem = parseGemProperties(combatAssets, rawItem),
			onlyUser = if (rawOnlyUser != null) parseFlashString(rawOnlyUser, "only_user")!! else null
	)
}

private fun parseSkills(skillAssets: SkillAssets, rawSkills: String?): ArrayList<Skill> {
	val skills = ArrayList<Skill>(0)
	if (rawSkills != null) {
		val nestedSkills = parseActionScriptNestedList(rawSkills)
		if (nestedSkills !is ArrayList<*>) throw ItemParseException("Unexpected item skills $rawSkills")
		for (skillPair in nestedSkills) {
			if (skillPair !is ArrayList<*> || skillPair.size != 2) throw ItemParseException("Unexpected item skills $rawSkills")
			val rawCategory = parseFlashString(skillPair[0] as String, "skill category")!!
			val skillName = parseFlashString(skillPair[1] as String, "skill name")!!
			if (rawCategory == "R:PASSIVE") {
				if (skillName == "Absorb MP") skills.add(skillAssets.reactionSkills.find { it.name == skillName }!!)
				else skills.add(skillAssets.passiveSkills.find { it.name == skillName }!!)
			} else if (rawCategory.startsWith("R:")) {
				val reactionType = when (rawCategory) {
					"R:P_ATK" -> ReactionSkillType.MeleeAttack
					"R:P_DEF" -> ReactionSkillType.MeleeDefense
					"R:M_ATK" -> ReactionSkillType.RangedAttack
					"R:M_DEF" -> ReactionSkillType.RangedDefense
					else -> throw ItemParseException("Unknown skill category $rawCategory")
				}
				skills.add(skillAssets.reactionSkills.find {
					it.type == reactionType && it.name == skillName
				}!!)
			} else {
				skills.add(skillAssets.classes.find {
					it.key == rawCategory
				}!!.actions.find { it.name == skillName }!!)
			}
		}
	}
	return skills
}

private fun parseWeaponProperties(
		combatAssets: CombatAssets, assets: InventoryAssets, rawItem: Map<String, String>
): WeaponProperties? {
	val rawWeaponType = rawItem["wpnType"] ?: return null
	val weaponType = assets.weaponTypes.find { it.flashName == parseFlashString(rawWeaponType, "weapon type") }!!

	val raceBonuses = ArrayList<RaceDamageBonus>(0)
	val rawRaceBonuses = rawItem["typeBonus"]
	if (rawRaceBonuses != null) {
		for ((rawRace, rawBonus) in parseActionScriptObject(rawRaceBonuses)) {
			val race = combatAssets.races.find { it.flashName == rawRace }!!
			val bonusFraction = parseInt(rawBonus) - 1f
			raceBonuses.add(RaceDamageBonus(race, bonusFraction))
		}
	}

	val addEffects = ArrayList<PossibleStatusEffect>(0)
	val rawEffects = rawItem["stfx"]
	if (rawEffects != null) {
		for ((rawEffect, rawChance) in parseActionScriptObject(rawEffects)) {
			val effect = combatAssets.statusEffects.find { it.flashName == rawEffect }!!
			addEffects.add(PossibleStatusEffect(effect, parseInt(rawChance)))
		}
	}

	val rawSound = rawItem["hit_sfx"]

	return WeaponProperties(
			type = weaponType,
			critChance = parseInt(rawItem["critical"]),
			hitChance = parseInt(rawItem["hit"]),
			hpDrain = if (rawItem["HP_DRAIN"] == "true") 1f else 0f,
			raceBonuses = raceBonuses,
			addEffects = addEffects,
			hitSound = if (rawSound != null) parseFlashString(rawSound, "weapon hit sound")!! else null,
	)
}

private fun parseGemProperties(combatAssets: CombatAssets, rawItem: Map<String, String>): GemProperties? {
	if (rawItem["type"] != "\"gems\"") return null

	val rawGem = parseActionScriptNestedList(rawItem["spell"]!!)
	if (rawGem !is ArrayList<*> || rawGem.size < 2 || rawGem.size > 4) throw ItemParseException("Unexpected gem $rawGem")

	val inflictStatusEffects = ArrayList<PossibleStatusEffect>(0)
	if (rawGem.size >= 3) {
		val rawEffects = rawGem[2]
		if (rawEffects is String && rawEffects != "null") {
			for ((effectName, rawChance) in parseActionScriptObject(rawEffects)) {
				val effect = combatAssets.statusEffects.find { it.flashName == effectName }!!
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

private fun parseConsumable(combatAssets: CombatAssets, rawItem: Map<String, String>): ConsumableProperties? {
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
				val effect = combatAssets.statusEffects.find {
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
				val effect = combatAssets.statusEffects.find { it.flashName == rawEffect }!!
				addStatusEffects.add(PossibleStatusEffect(effect, parseInt(rawChance)))
			}
		}

		val rawStatModifiers = rawSpell["stat_mod"]
		if (rawStatModifiers != null) {
			for ((statName, rawAdder) in parseActionScriptObject(rawStatModifiers)) {
				val stat = combatAssets.stats.find { it.flashName == statName }!!
				val adder = parseInt(rawAdder)
				statModifiers.add(StatModifierRange(stat, adder, adder))
			}
		}

		if (rawItem["hurtful"] == "true") {
			val power = parseInt(rawSpell["pow"])
			val spirit = parseInt(rawSpell["SPR"])
			if (power != 0) {
				val element = combatAssets.elements.find {
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

	return ConsumableProperties(
			particleEffect = if (rawItem["name"] == "\"Oxyale\"") null else
				parseFlashString(particleEffect!!, "consumable pfx")!!,
			particleColor = rgb(
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
