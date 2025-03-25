package mardek.importer.stats

import mardek.content.stats.StatsContent
import mardek.content.stats.StatusEffect
import java.util.*

fun addStatusEffects(assets: StatsContent) {
	assets.statusEffects.add(StatusEffect(
		flashName = "PSN",
		niceName = "Poison",
		isPositive = false,
		disappearsAfterCombat = false,
		damageFractionPerTurn = 0.1f,
		damageOutsideCombat = 1
	))
	assets.statusEffects.add(StatusEffect(
		flashName = "CNF",
		niceName = "Confusion",
		isPositive = false,
		disappearsAfterCombat = true,
		isConfusing = true
	))
	assets.statusEffects.add(StatusEffect(
		flashName = "CRS",
		niceName = "Curse",
		isPositive = false,
		disappearsAfterCombat = false,
		blocksRangedSkills = true,
		blocksMeleeSkills = true
	))
	assets.statusEffects.add(StatusEffect(
		flashName = "DRK",
		niceName = "Blindness",
		isPositive = false,
		disappearsAfterCombat = false,
		missChance = 50
	))
	assets.statusEffects.add(StatusEffect(
		flashName = "SIL",
		niceName = "Silence",
		isPositive = false,
		disappearsAfterCombat = false,
		blocksRangedSkills = true
	))
	assets.statusEffects.add(StatusEffect(
		flashName = "SLP",
		niceName = "Sleep",
		isPositive = false,
		disappearsAfterCombat = true,
		skipTurnChance = 100,
		disappearChancePerTurn = 25
	))
	assets.statusEffects.add(StatusEffect(
		flashName = "PAR",
		niceName = "Paralysis",
		isPositive = false,
		disappearsAfterCombat = false,
		skipTurnChance = 50
	))
	assets.statusEffects.add(StatusEffect(
		flashName = "NUM",
		niceName = "Numbness",
		isPositive = false,
		disappearsAfterCombat = false,
		blocksMeleeSkills = true,
		blocksBasicAttacks = true
	))
	assets.statusEffects.add(StatusEffect(
		flashName = "RGN",
		niceName = "Regen",
		isPositive = true,
		disappearsAfterCombat = true,
		damageFractionPerTurn = -0.1f
	))
	assets.statusEffects.add(StatusEffect(
		flashName = "PSH",
		niceName = "Shield",
		isPositive = true,
		disappearsAfterCombat = true,
		meleeDamageReduction = 0.5f
	))
	assets.statusEffects.add(StatusEffect(
		flashName = "MSH",
		niceName = "M.Shield",
		isPositive = true,
		disappearsAfterCombat = true,
		rangedDamageReduction = 0.5f
	))
	assets.statusEffects.add(StatusEffect(
		flashName = "BSK",
		niceName = "Berserk",
		isPositive = true,
		disappearsAfterCombat = true,
		meleeDamageModifier = 2f,
		isReckless = true
	))
	assets.statusEffects.add(StatusEffect(
		flashName = "HST",
		niceName = "Haste",
		isPositive = true,
		disappearsAfterCombat = true,
		extraTurns = 1
	))
	assets.statusEffects.add(StatusEffect(
		flashName = "UWB",
		niceName = "Aqualong",
		isPositive = true,
		disappearsAfterCombat = false,
		canWaterBreathe = true
	))
	assets.statusEffects.add(StatusEffect(
		flashName = "ZOM",
		niceName = "Zombie",
		isPositive = false,
		disappearsAfterCombat = false,
		isZombie = true
	))
	assets.statusEffects.add(StatusEffect(
		flashName = "GST",
		niceName = "Astral",
		isPositive = true,
		disappearsAfterCombat = true,
		isAstralForm = true
	))
	assets.statusEffects.add(StatusEffect(
		flashName = "BRK",
		niceName = "Barskin",
		isPositive = true,
		disappearsAfterCombat = true,
		hasBarskin = true
	))
	assets.statusEffects.add(StatusEffect(
		flashName = "BLD",
		niceName = "Bleed",
		isPositive = false,
		disappearsAfterCombat = false,
		damageFractionPerTurn = 0.2f
	))

	for (element in assets.elements) {
		if (element.weakAgainst == null) continue // Skip physical and thauma
		assets.statusEffects.add(StatusEffect(
			flashName = "${element.primaryChar}N1",
			niceName = "Null ${element.properName.lowercase(Locale.ROOT)}",
			isPositive = true,
			disappearsAfterCombat = true,
			nullifiesElement = element
		))
	}

	assets.statusEffects.add(StatusEffect(
		flashName = "SHF",
		niceName = "Pyro Shell",
		isPositive = true,
		disappearsAfterCombat = true,
		elementShell = assets.elements.find { it.rawName == "FIRE" }!!
	))
	assets.statusEffects.add(StatusEffect(
		flashName = "SHW",
		niceName = "Hydro Shell",
		isPositive = true,
		disappearsAfterCombat = true,
		elementShell = assets.elements.find { it.rawName == "WATER" }!!
	))
	assets.statusEffects.add(StatusEffect(
		flashName = "SHE",
		niceName = "Geo Shell",
		isPositive = true,
		disappearsAfterCombat = true,
		elementShell = assets.elements.find { it.rawName == "EARTH" }!!
	))
	assets.statusEffects.add(StatusEffect(
		flashName = "SHA",
		niceName = "Aero Shell",
		isPositive = true,
		disappearsAfterCombat = true,
		elementShell = assets.elements.find { it.rawName == "AIR" }!!
	))

	assets.statusEffects.add(StatusEffect(
		flashName = "BRN",
		niceName = "Burn?",
		isPositive = false,
		disappearsAfterCombat = true
	))
}
