package mardek.importer.stats

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import mardek.content.Content
import mardek.content.sprite.BcSprite
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

private fun getBcSprite(name: String): BcSprite {
	val resource = BcPacker::class.java.classLoader.getResource("mardek/importer/stats/elements/${name}.png")!!
	val image = ImageIO.read(resource)
	val sprite = BcSprite(image.width, image.height, 7)
	sprite.bufferedImage = image
	return sprite
}

fun addElements(content: Content) {
	val fire = Element(
		rawName = "FIRE",
		bonusStat = CombatStat.Strength,
		primaryChar = "F",
		color = rgb(255, 204, 0),
		sprite = getSprite2("Fire", 4),
		bcSprite = getBcSprite("Fire"),
		swingEffect = getBcSprite("SwingFire"),
		spellCastEffect = content.battle.particles.find { it.name == "one_sparkle_FIRE" }!!,
		spellCastBackground = getBcSprite("CastBackgroundFire"),
	)
	val water = Element(
		rawName = "WATER",
		bonusStat = CombatStat.Spirit,
		primaryChar = "W",
		color = rgb(0, 204, 255),
		sprite = getSprite2("Water", 4),
		bcSprite = getBcSprite("Water"),
		swingEffect = getBcSprite("SwingWater"),
		spellCastEffect = content.battle.particles.find { it.name == "one_sparkle_WATER" }!!,
		spellCastBackground = getBcSprite("CastBackgroundWater"),
	)
	val earth = Element(
		rawName = "EARTH",
		bonusStat = CombatStat.Vitality,
		primaryChar = "E",
		color = rgb(0, 255, 0),
		sprite = getSprite2("Earth",4),
		bcSprite = getBcSprite("Earth"),
		swingEffect = getBcSprite("SwingEarth"),
		spellCastEffect = content.battle.particles.find { it.name == "one_sparkle_EARTH" }!!,
		spellCastBackground = getBcSprite("CastBackgroundEarth"),
	)
	val air = Element(
		rawName = "AIR",
		bonusStat = CombatStat.Agility,
		primaryChar = "A",
		color = rgb(255, 255, 204),
		sprite = getSprite2("Air", 4),
		bcSprite = getBcSprite("Air"),
		swingEffect = getBcSprite("SwingAir"),
		spellCastEffect = content.battle.particles.find { it.name == "one_sparkle_AIR" }!!,
		spellCastBackground = getBcSprite("CastBackgroundAir"),
	)
	fire.setWeakAgainst(water)
	water.setWeakAgainst(earth)
	earth.setWeakAgainst(air)
	air.setWeakAgainst(fire)

	content.stats.elements.addAll(listOf(fire, water, earth, air))

	val dark = Element(
		rawName = "DARK",
		bonusStat = CombatStat.MeleeDefense,
		primaryChar = "D",
		color = rgb(0, 0, 0),
		sprite = getSprite2("Dark", 4),
		bcSprite = getBcSprite("Dark"),
		swingEffect = getBcSprite("SwingDark"),
		spellCastEffect = content.battle.particles.find { it.name == "one_sparkle_DARK" }!!,
		spellCastBackground = getBcSprite("CastBackgroundDark"),
	)
	val light = Element(
		rawName = "LIGHT",
		bonusStat = CombatStat.RangedDefense,
		primaryChar = "L",
		color = rgb(255, 255, 255),
		sprite = getSprite2("Light", 8),
		bcSprite = getBcSprite("Light"),
		swingEffect = getBcSprite("SwingLight"),
		spellCastEffect = content.battle.particles.find { it.name == "one_sparkle_LIGHT" }!!,
		spellCastBackground = getBcSprite("CastBackgroundLight"),
	)
	dark.setWeakAgainst(light)
	light.setWeakAgainst(dark)

	content.stats.elements.addAll(listOf(dark, light))

	val fig = Element(
		rawName = "FIG",
		bonusStat = CombatStat.MaxHealth,
		primaryChar = "M",
		color = rgb(191, 68, 205),
		sprite = getSprite2("Fig", 4),
		bcSprite = getBcSprite("Fig"),
		swingEffect = getBcSprite("SwingFig"),
		spellCastEffect = content.battle.particles.find { it.name == "one_sparkle_FIG" }!!,
		spellCastBackground = getBcSprite("CastBackgroundFig"),
	)
	val aether = Element(
		rawName = "ETHER",
		bonusStat = CombatStat.MaxMana,
		primaryChar = "S",
		properName = "AETHER",
		color = rgb(0, 255, 204),
		sprite = getSprite2("Aether", 4),
		bcSprite = getBcSprite("Aether"),
		swingEffect = getBcSprite("SwingAether"),
		spellCastEffect = content.battle.particles.find { it.name == "one_sparkle_ETHER" }!!,
		spellCastBackground = getBcSprite("CastBackgroundAether"),
	)
	fig.setWeakAgainst(aether)
	aether.setWeakAgainst(fig)

	content.stats.elements.addAll(listOf(fig, aether))

	content.stats.elements.add(Element(
		rawName = "NONE",
		bonusStat = null,
		primaryChar = "N",
		properName = "PHYSICAL",
		color = rgb(204, 204, 204),
		sprite = getSprite2("Physical", 4),
		bcSprite = getBcSprite("Physical"),
		swingEffect = getBcSprite("SwingPhysical"),
		spellCastEffect = null,
		spellCastBackground = null,
	))
	content.stats.elements.add(Element(
		rawName = "THAUMA",
		bonusStat = null,
		primaryChar = "T",
		color = rgb(120, 120, 254),
		sprite = getSprite2("Thauma", 4),
		bcSprite = getBcSprite("Thauma"),
		swingEffect = null,
		spellCastEffect = null,
		spellCastBackground = getBcSprite("CastBackgroundThauma"),
	))
	content.stats.elements.add(Element(
		rawName = "DIVINE",
		bonusStat = null,
		primaryChar = "D",
		color = rgb(252, 207, 207),
		sprite = getSprite2("Divine", 4),
		bcSprite = getBcSprite("Divine"),
		swingEffect = null,
		spellCastEffect = null,
		spellCastBackground = null,
	))
}
