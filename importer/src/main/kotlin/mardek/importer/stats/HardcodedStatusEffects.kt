package mardek.importer.stats

import mardek.content.stats.ElementalResistance
import mardek.content.stats.Resistances
import mardek.content.stats.StatsContent
import mardek.content.stats.StatusEffect
import java.util.*
import kotlin.collections.ArrayList

fun addStatusEffects(content: StatsContent) {
	fun element(properName: String) = content.elements.find { it.properName == properName }!!

	content.statusEffects.add(StatusEffect(
		flashName = "PSN",
		niceName = "Poison",
		isPositive = false,
		disappearsAfterCombat = false,
		damageFractionPerTurn = 0.1f,
		damageOutsideCombat = 1
	))
	content.statusEffects.add(StatusEffect(
		flashName = "CNF",
		niceName = "Confusion",
		isPositive = false,
		disappearsAfterCombat = true,
		disappearAfterHitChance = 100,
		isConfusing = true
	))
	content.statusEffects.add(StatusEffect(
		flashName = "CRS",
		niceName = "Curse",
		isPositive = false,
		disappearsAfterCombat = false,
		blocksRangedSkills = true,
		blocksMeleeSkills = true
	))
	content.statusEffects.add(StatusEffect(
		flashName = "DRK",
		niceName = "Blindness",
		isPositive = false,
		disappearsAfterCombat = false,
		missChance = 50
	))
	content.statusEffects.add(StatusEffect(
		flashName = "SIL",
		niceName = "Silence",
		isPositive = false,
		disappearsAfterCombat = false,
		blocksRangedSkills = true
	))
	content.statusEffects.add(StatusEffect(
		flashName = "SLP",
		niceName = "Sleep",
		isPositive = false,
		disappearsAfterCombat = true,
		disappearAfterHitChance = 100,
		skipTurnChance = 100,
		disappearChancePerTurn = 25
	))
	content.statusEffects.add(StatusEffect(
		flashName = "PAR",
		niceName = "Paralysis",
		isPositive = false,
		disappearsAfterCombat = false,
		skipTurnChance = 50
	))
	content.statusEffects.add(StatusEffect(
		flashName = "NUM",
		niceName = "Numbness",
		isPositive = false,
		disappearsAfterCombat = false,
		blocksMeleeSkills = true,
		blocksBasicAttacks = true
	))
	content.statusEffects.add(StatusEffect(
		flashName = "RGN",
		niceName = "Regen",
		isPositive = true,
		disappearsAfterCombat = true,
		damageFractionPerTurn = -0.1f
	))
	content.statusEffects.add(StatusEffect(
		flashName = "PSH",
		niceName = "Shield",
		isPositive = true,
		disappearsAfterCombat = true,
		meleeDamageReduction = 0.5f
	))
	content.statusEffects.add(StatusEffect(
		flashName = "MSH",
		niceName = "M.Shield",
		isPositive = true,
		disappearsAfterCombat = true,
		rangedDamageReduction = 0.5f
	))
	content.statusEffects.add(StatusEffect(
		flashName = "BSK",
		niceName = "Berserk",
		isPositive = true,
		disappearsAfterCombat = true,
		meleeDamageModifier = 1f,
		isReckless = true
	))
	content.statusEffects.add(StatusEffect(
		flashName = "HST",
		niceName = "Haste",
		isPositive = true,
		disappearsAfterCombat = true,
		extraTurns = 1
	))
	content.statusEffects.add(StatusEffect(
		flashName = "UWB",
		niceName = "Aqualong",
		isPositive = true,
		disappearsAfterCombat = false,
		canWaterBreathe = true
	))
	content.statusEffects.add(StatusEffect(
		flashName = "ZOM",
		niceName = "Zombie",
		isPositive = false,
		disappearsAfterCombat = false,
		isZombie = true
	))
	content.statusEffects.add(StatusEffect(
		flashName = "GST",
		niceName = "Astral",
		isPositive = true,
		disappearsAfterCombat = true,
		isAstralForm = true,
		resistances = Resistances(
			elements = arrayListOf(
				ElementalResistance(element("WATER"), 0.5f),
				ElementalResistance(element("FIRE"), 0.5f),
				ElementalResistance(element("AIR"), 0.5f),
				ElementalResistance(element("EARTH"), 0.5f),
				ElementalResistance(element("LIGHT"), -1f),
				ElementalResistance(element("DARK"), -1f),
				ElementalResistance(element("AETHER"), 2f),
				ElementalResistance(element("FIG"), -1f),
				ElementalResistance(element("THAUMA"), -1f)
			),
			effects = ArrayList(0)
		)
	))
	content.statusEffects.add(StatusEffect(
		flashName = "BRK",
		niceName = "Barskin",
		isPositive = true,
		disappearsAfterCombat = true,
		hasBarskin = true,
	))
	content.statusEffects.add(StatusEffect(
		flashName = "BLD",
		niceName = "Bleed",
		isPositive = false,
		disappearsAfterCombat = false,
		damageFractionPerTurn = 0.2f
	))

	for (element in content.elements) {
		if (element.weakAgainst == null) continue // Skip physical and thauma
		content.statusEffects.add(StatusEffect(
			flashName = "${element.primaryChar}N1",
			niceName = "Null ${element.properName.lowercase(Locale.ROOT)}",
			isPositive = true,
			disappearsAfterCombat = true,
			nullifiesElement = element
		))
	}

	content.statusEffects.add(StatusEffect(
		flashName = "SHF",
		niceName = "Pyro Shell",
		isPositive = true,
		disappearsAfterCombat = true,
		elementShell = content.elements.find { it.rawName == "FIRE" }!!
	))
	content.statusEffects.add(StatusEffect(
		flashName = "SHW",
		niceName = "Hydro Shell",
		isPositive = true,
		disappearsAfterCombat = true,
		elementShell = content.elements.find { it.rawName == "WATER" }!!
	))
	content.statusEffects.add(StatusEffect(
		flashName = "SHE",
		niceName = "Geo Shell",
		isPositive = true,
		disappearsAfterCombat = true,
		elementShell = content.elements.find { it.rawName == "EARTH" }!!
	))
	content.statusEffects.add(StatusEffect(
		flashName = "SHA",
		niceName = "Aero Shell",
		isPositive = true,
		disappearsAfterCombat = true,
		elementShell = content.elements.find { it.rawName == "AIR" }!!
	))

	content.statusEffects.add(StatusEffect(
		flashName = "BRN",
		niceName = "Burn?",
		isPositive = false,
		disappearsAfterCombat = true
	))
}
