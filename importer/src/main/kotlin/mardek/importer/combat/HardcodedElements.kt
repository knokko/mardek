package mardek.importer.combat

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import mardek.assets.combat.CombatAssets
import mardek.assets.combat.Element
import mardek.assets.sprite.KimSprite
import mardek.importer.ui.UiPacker
import mardek.importer.util.compressKimSprite1
import javax.imageio.ImageIO

private fun getSprite(name: String): KimSprite {
	val input = UiPacker::class.java.classLoader.getResourceAsStream("mardek/importer/combat/elements/${name}Thick.png")!!
	val image = ImageIO.read(input)
	input.close()

	return compressKimSprite1(image)
}

fun addElements(assets: CombatAssets) {
	val fire = Element(
		rawName = "FIRE",
		bonusStat = assets.stats.find { it.flashName == "STR" }!!,
		primaryChar = "F",
		color = rgb(255, 204, 0),
		thickSprite = getSprite("Fire"),
	)
	val water = Element(
		rawName = "WATER",
		bonusStat = assets.stats.find { it.flashName == "SPR" }!!,
		primaryChar = "W",
		color = rgb(0, 204, 255),
		thickSprite = getSprite("Water"),
	)
	val earth = Element(
		rawName = "EARTH",
		bonusStat = assets.stats.find { it.flashName == "VIT" }!!,
		primaryChar = "E",
		color = rgb(0, 255, 0),
		thickSprite = getSprite("Earth"),
	)
	val air = Element(
		rawName = "AIR",
		bonusStat = assets.stats.find { it.flashName == "AGL" }!!,
		primaryChar = "A",
		color = rgb(255, 255, 204),
		thickSprite = getSprite("Air"),
	)
	fire.setWeakAgainst(water)
	water.setWeakAgainst(earth)
	earth.setWeakAgainst(air)
	air.setWeakAgainst(fire)

	assets.elements.addAll(listOf(fire, water, earth, air))

	val dark = Element(
		rawName = "DARK",
		bonusStat = assets.stats.find { it.flashName == "DEF" }!!,
		primaryChar = "D",
		color = rgb(0, 0, 0),
		thickSprite = getSprite("Dark")
	)
	val light = Element(
		rawName = "LIGHT",
		bonusStat = assets.stats.find { it.flashName == "MDEF" }!!,
		primaryChar = "L",
		color = rgb(255, 255, 255),
		thickSprite = getSprite("Light")
	)
	dark.setWeakAgainst(light)
	light.setWeakAgainst(dark)

	assets.elements.addAll(listOf(dark, light))

	val fig = Element(
		rawName = "FIG",
		bonusStat = assets.stats.find { it.flashName == "hp" }!!,
		primaryChar = "M",
		color = rgb(191, 68, 205),
		thickSprite = getSprite("Fig")
	)
	val aether = Element(
		rawName = "ETHER",
		bonusStat = assets.stats.find { it.flashName == "mp" }!!,
		primaryChar = "S",
		properName = "AETHER",
		color = rgb(0, 255, 204),
		thickSprite = getSprite("Aether")
	)
	fig.setWeakAgainst(aether)
	aether.setWeakAgainst(fig)

	assets.elements.addAll(listOf(fig, aether))

	assets.elements.add(Element(
		rawName = "NONE",
		bonusStat = null,
		primaryChar = "N",
		properName = "PHYSICAL",
		color = rgb(204, 204, 204),
		thickSprite = getSprite("Physical")
	))
	assets.elements.add(Element(
		rawName = "THAUMA",
		bonusStat = null,
		primaryChar = "T",
		color = rgb(120, 120, 254),
		thickSprite = getSprite("Thauma")
	))
	assets.elements.add(Element(
		rawName = "DIVINE",
		bonusStat = null,
		primaryChar = "D",
		color = rgb(252, 207, 207),
		thickSprite = getSprite("Divine")
	))
}
