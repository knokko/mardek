package mardek.importer.stats

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import mardek.content.Content
import mardek.content.sprite.BcSprite
import mardek.content.stats.Element
import mardek.content.stats.CombatStat
import mardek.importer.util.classLoader
import java.util.UUID
import javax.imageio.ImageIO

private fun getBcSprite(name: String): BcSprite {
	val resource = classLoader.getResource("mardek/importer/stats/elements/${name}.png")!!
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
		thickSprite = getBcSprite("Fire"),
		thinSprite = getBcSprite("FireThin"),
		swingEffect = getBcSprite("SwingFire"),
		spellCastEffect = content.battle.particles.find { it.name == "one_sparkle_FIRE" }!!,
		spellCastBackground = getBcSprite("CastBackgroundFire"),
		id = UUID.fromString("1bfd73f6-c05c-4ae5-9257-6a8471cbdbba"),
	)
	val water = Element(
		rawName = "WATER",
		bonusStat = CombatStat.Spirit,
		primaryChar = "W",
		color = rgb(0, 204, 255),
		thickSprite = getBcSprite("Water"),
		thinSprite = getBcSprite("WaterThin"),
		swingEffect = getBcSprite("SwingWater"),
		spellCastEffect = content.battle.particles.find { it.name == "one_sparkle_WATER" }!!,
		spellCastBackground = getBcSprite("CastBackgroundWater"),
		id = UUID.fromString("e5ef0cd6-8e44-4c3f-8d63-7e38a0e96e95"),
	)
	val earth = Element(
		rawName = "EARTH",
		bonusStat = CombatStat.Vitality,
		primaryChar = "E",
		color = rgb(0, 255, 0),
		thickSprite = getBcSprite("Earth"),
		thinSprite = getBcSprite("EarthThin"),
		swingEffect = getBcSprite("SwingEarth"),
		spellCastEffect = content.battle.particles.find { it.name == "one_sparkle_EARTH" }!!,
		spellCastBackground = getBcSprite("CastBackgroundEarth"),
		id = UUID.fromString("b520c012-fcd9-4f7e-a176-1bc59ff705ad"),
	)
	val air = Element(
		rawName = "AIR",
		bonusStat = CombatStat.Agility,
		primaryChar = "A",
		color = rgb(255, 255, 204),
		thickSprite = getBcSprite("Air"),
		thinSprite = getBcSprite("AirThin"),
		swingEffect = getBcSprite("SwingAir"),
		spellCastEffect = content.battle.particles.find { it.name == "one_sparkle_AIR" }!!,
		spellCastBackground = getBcSprite("CastBackgroundAir"),
		id = UUID.fromString("75195446-e263-485c-84c0-6a60141dcbc8"),
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
		thickSprite = getBcSprite("Dark"),
		thinSprite = getBcSprite("DarkThin"),
		swingEffect = getBcSprite("SwingDark"),
		spellCastEffect = content.battle.particles.find { it.name == "one_sparkle_DARK" }!!,
		spellCastBackground = getBcSprite("CastBackgroundDark"),
		id = UUID.fromString("e5a2760a-1264-4c0d-9ca4-f1bcba29d613"),
	)
	val light = Element(
		rawName = "LIGHT",
		bonusStat = CombatStat.RangedDefense,
		primaryChar = "L",
		color = rgb(255, 255, 255),
		thickSprite = getBcSprite("Light"),
		thinSprite = getBcSprite("LightThin"),
		swingEffect = getBcSprite("SwingLight"),
		spellCastEffect = content.battle.particles.find { it.name == "one_sparkle_LIGHT" }!!,
		spellCastBackground = getBcSprite("CastBackgroundLight"),
		id = UUID.fromString("b7f41764-8d53-4383-8bf3-6a194d01a5a0"),
	)
	dark.setWeakAgainst(light)
	light.setWeakAgainst(dark)

	content.stats.elements.addAll(listOf(dark, light))

	val fig = Element(
		rawName = "FIG",
		bonusStat = CombatStat.MaxHealth,
		primaryChar = "M",
		color = rgb(191, 68, 205),
		thickSprite = getBcSprite("Fig"),
		thinSprite = getBcSprite("FigThin"),
		swingEffect = getBcSprite("SwingFig"),
		spellCastEffect = content.battle.particles.find { it.name == "one_sparkle_FIG" }!!,
		spellCastBackground = getBcSprite("CastBackgroundFig"),
		id = UUID.fromString("d4429d7d-3608-4faa-b2a9-0e93eaeea28b"),
	)
	val aether = Element(
		rawName = "ETHER",
		bonusStat = CombatStat.MaxMana,
		primaryChar = "S",
		properName = "AETHER",
		color = rgb(0, 255, 204),
		thickSprite = getBcSprite("Aether"),
		thinSprite = getBcSprite("AetherThin"),
		swingEffect = getBcSprite("SwingAether"),
		spellCastEffect = content.battle.particles.find { it.name == "one_sparkle_ETHER" }!!,
		spellCastBackground = getBcSprite("CastBackgroundAether"),
		id = UUID.fromString("d171c63f-0e4b-4a43-953e-4580269b4e43"),
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
		thickSprite = getBcSprite("Physical"),
		thinSprite = getBcSprite("PhysicalThin"),
		swingEffect = getBcSprite("SwingPhysical"),
		spellCastEffect = null,
		spellCastBackground = null,
		id = UUID.fromString("3d1dbc94-dc04-4ec3-a2b7-0269747afb2d"),
	))
	content.stats.defaultWeaponElement = content.stats.elements.last()
	content.stats.elements.add(Element(
		rawName = "THAUMA",
		bonusStat = null,
		primaryChar = "T",
		color = rgb(120, 120, 254),
		thickSprite = getBcSprite("Thauma"),
		thinSprite = getBcSprite("ThaumaThin"),
		swingEffect = null,
		spellCastEffect = null,
		spellCastBackground = getBcSprite("CastBackgroundThauma"),
		id = UUID.fromString("6a859620-744c-4588-9b60-d7a643fcd4d6"),
	))
	content.stats.elements.add(Element(
		rawName = "DIVINE",
		bonusStat = null,
		primaryChar = "D",
		color = rgb(252, 207, 207),
		thickSprite = getBcSprite("Divine"),
		thinSprite = getBcSprite("DivineThin"),
		swingEffect = null,
		spellCastEffect = null,
		spellCastBackground = null,
		id = UUID.fromString("da8e4ad0-5c03-4f1a-9890-d6cb4a4510f7"),
	))
}
