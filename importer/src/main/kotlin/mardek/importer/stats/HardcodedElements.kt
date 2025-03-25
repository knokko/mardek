package mardek.importer.stats

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import mardek.content.stats.StatsContent
import mardek.content.stats.Element
import mardek.content.sprite.KimSprite
import mardek.content.stats.CombatStat
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
		bonusStat = CombatStat.Strength,
		primaryChar = "F",
		color = rgb(255, 204, 0),
		sprite = getSprite2("Fire", 4),
	)
	val water = Element(
		rawName = "WATER",
		bonusStat = CombatStat.Spirit,
		primaryChar = "W",
		color = rgb(0, 204, 255),
		sprite = getSprite2("Water", 4),
	)
	val earth = Element(
		rawName = "EARTH",
		bonusStat = CombatStat.Vitality,
		primaryChar = "E",
		color = rgb(0, 255, 0),
		sprite = getSprite2("Earth",4),
	)
	val air = Element(
		rawName = "AIR",
		bonusStat = CombatStat.Agility,
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
		bonusStat = CombatStat.MeleeDefense,
		primaryChar = "D",
		color = rgb(0, 0, 0),
		sprite = getSprite2("Dark", 4)
	)
	val light = Element(
		rawName = "LIGHT",
		bonusStat = CombatStat.RangedDefense,
		primaryChar = "L",
		color = rgb(255, 255, 255),
		sprite = getSprite2("Light", 4)
	)
	dark.setWeakAgainst(light)
	light.setWeakAgainst(dark)

	assets.elements.addAll(listOf(dark, light))

	val fig = Element(
		rawName = "FIG",
		bonusStat = CombatStat.MaxHealth,
		primaryChar = "M",
		color = rgb(191, 68, 205),
		sprite = getSprite2("Fig", 4)
	)
	val aether = Element(
		rawName = "ETHER",
		bonusStat = CombatStat.MaxMana,
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
