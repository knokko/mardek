package mardek.importer.combat

import mardek.assets.combat.CombatAssets
import mardek.assets.combat.Element

fun addElements(assets: CombatAssets) {
	val fire = Element(
		rawName = "FIRE",
		bonusStat = assets.stats.find { it.flashName == "STR" }!!,
		primaryChar = "F",
	)
	val water = Element(
		rawName = "WATER",
		bonusStat = assets.stats.find { it.flashName == "SPR" }!!,
		primaryChar = "W",
	)
	val earth = Element(
		rawName = "EARTH",
		bonusStat = assets.stats.find { it.flashName == "VIT" }!!,
		primaryChar = "E"
	)
	val air = Element(
		rawName = "AIR",
		bonusStat = assets.stats.find { it.flashName == "AGL" }!!,
		primaryChar = "A"
	)
	fire.setWeakAgainst(water)
	water.setWeakAgainst(earth)
	earth.setWeakAgainst(air)
	air.setWeakAgainst(fire)

	assets.elements.addAll(listOf(fire, water, earth, air))

	val dark = Element(
		rawName = "DARK",
		bonusStat = assets.stats.find { it.flashName == "DEF" }!!,
		primaryChar = "D"
	)
	val light = Element(
		rawName = "LIGHT",
		bonusStat = assets.stats.find { it.flashName == "MDEF" }!!,
		primaryChar = "L"
	)
	dark.setWeakAgainst(light)
	light.setWeakAgainst(dark)

	assets.elements.addAll(listOf(dark, light))

	val fig = Element(
		rawName = "FIG",
		bonusStat = assets.stats.find { it.flashName == "hp" }!!,
		primaryChar = "M"
	)
	val aether = Element(
		rawName = "ETHER",
		bonusStat = assets.stats.find { it.flashName == "mp" }!!,
		primaryChar = "S",
		properName = "AETHER"
	)
	fig.setWeakAgainst(aether)
	aether.setWeakAgainst(fig)

	assets.elements.addAll(listOf(fig, aether))

	assets.elements.add(Element(
		rawName = "NONE",
		bonusStat = null,
		primaryChar = "N",
		properName = "PHYSICAL"
	))
	assets.elements.add(Element(
		rawName = "THAUMA",
		bonusStat = null,
		primaryChar = "T"
	))
	assets.elements.add(Element(
		rawName = "DIVINE",
		bonusStat = null,
		primaryChar = "D"
	))
}
