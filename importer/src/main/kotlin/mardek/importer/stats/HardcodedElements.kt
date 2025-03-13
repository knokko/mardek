package mardek.importer.stats

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import mardek.content.combat.StatsContent
import mardek.content.combat.Element
import mardek.content.sprite.KimSprite
import mardek.importer.ui.BcPacker
import mardek.importer.util.compressKimSprite2
import javax.imageio.ImageIO

private fun getSprite2(name: String, bitsPerPixel: Int): KimSprite {
	val resource = BcPacker::class.java.classLoader.getResource("mardek/importer/stats/elements/${name}.png")!!
	return compressKimSprite2(ImageIO.read(resource), bitsPerPixel)
}

fun addElements(assets: StatsContent) {
	val fire = Element(
		rawName = "FIRE",
		bonusStat = assets.stats.find { it.flashName == "STR" }!!,
		primaryChar = "F",
		color = rgb(255, 204, 0),
		sprite = getSprite2("Fire", 4),
	)
	val water = Element(
		rawName = "WATER",
		bonusStat = assets.stats.find { it.flashName == "SPR" }!!,
		primaryChar = "W",
		color = rgb(0, 204, 255),
		sprite = getSprite2("Water", 4),
	)
	val earth = Element(
		rawName = "EARTH",
		bonusStat = assets.stats.find { it.flashName == "VIT" }!!,
		primaryChar = "E",
		color = rgb(0, 255, 0),
		sprite = getSprite2("Earth",4),
	)
	val air = Element(
		rawName = "AIR",
		bonusStat = assets.stats.find { it.flashName == "AGL" }!!,
		primaryChar = "A",
		color = rgb(255, 255, 204),
		sprite = getSprite2("Air", 4),
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
		sprite = getSprite2("Dark", 4)
	)
	val light = Element(
		rawName = "LIGHT",
		bonusStat = assets.stats.find { it.flashName == "MDEF" }!!,
		primaryChar = "L",
		color = rgb(255, 255, 255),
		sprite = getSprite2("Light", 4)
	)
	dark.setWeakAgainst(light)
	light.setWeakAgainst(dark)

	assets.elements.addAll(listOf(dark, light))

	val fig = Element(
		rawName = "FIG",
		bonusStat = assets.stats.find { it.flashName == "hp" }!!,
		primaryChar = "M",
		color = rgb(191, 68, 205),
		sprite = getSprite2("Fig", 4)
	)
	val aether = Element(
		rawName = "ETHER",
		bonusStat = assets.stats.find { it.flashName == "mp" }!!,
		primaryChar = "S",
		properName = "AETHER",
		color = rgb(0, 255, 204),
		sprite = getSprite2("Aether", 4)
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
		sprite = getSprite2("Physical", 4)
	))
	assets.elements.add(Element(
		rawName = "THAUMA",
		bonusStat = null,
		primaryChar = "T",
		color = rgb(120, 120, 254),
		sprite = getSprite2("Thauma", 4)
	))
	assets.elements.add(Element(
		rawName = "DIVINE",
		bonusStat = null,
		primaryChar = "D",
		color = rgb(252, 207, 207),
		sprite = getSprite2("Divine", 4)
	))
}
