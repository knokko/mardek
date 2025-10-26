package mardek.importer.stats

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import mardek.content.Content
import mardek.content.sprite.BcSprite
import mardek.content.stats.ElementalResistance
import mardek.content.stats.Resistances
import mardek.content.stats.StatusEffect
import mardek.importer.ui.BcPacker
import mardek.importer.util.loadBc7Sprite
import java.awt.image.BufferedImage
import java.util.*
import javax.imageio.ImageIO
import kotlin.collections.ArrayList

private fun icon(name: String) = loadBc7Sprite("mardek/importer/stats/effects/$name.png")

fun addStatusEffects(content: Content) {
	val stats = content.stats
	fun element(properName: String) = stats.elements.find { it.properName == properName }!!

	val sheet16 = ImageIO.read(BcPacker::class.java.classLoader.getResource(
		"mardek/importer/particle/sheet16.png"
	))
	val sheet32 = ImageIO.read(BcPacker::class.java.classLoader.getResource(
		"mardek/importer/particle/sheet32.png"
	))

	fun passiveSprites(atlas: BufferedImage, indices: IntArray) = indices.map { index ->
		val size = if (atlas === sheet16) 16 else 32
		val sprite = BcSprite(size, size, 7)
		sprite.bufferedImage = atlas.getSubimage(size * (index % 10), size * (index / 10), size, size)
		sprite
	}.toTypedArray()

	stats.statusEffects.add(StatusEffect(
		flashName = "PSN",
		niceName = "Poison",
		isPositive = false,
		disappearsAfterCombat = false,
		damagePerTurn = StatusEffect.TurnDamage(
			0.05f, element("EARTH"),
			content.battle.particles.find { it.name == "poison_oef" }!!,
			rgb(125, 220, 80)
		),
		damageWhileWalking = StatusEffect.WalkDamage(
			5, 0.02f, rgb(128, 221, 70)
		),
		icon = icon("Poison"),
		textColor = rgb(128, 221, 70),
		passiveParticleSprites = passiveSprites(sheet16, intArrayOf(4)),
		id = UUID.fromString("7dbcf062-ab1c-45bb-850d-4b4b155be2ca"),
	))
	stats.statusEffects.add(StatusEffect(
		flashName = "CNF",
		niceName = "Confusion",
		isPositive = false,
		disappearsAfterCombat = true,
		disappearAfterHitChance = 100,
		isConfusing = true,
		icon = icon("Confusion"),
		textColor = rgb(162, 139, 196),
		passiveParticleSprites = emptyArray(),
		id = UUID.fromString("fee0b22f-b960-4029-8508-24bbb355af3a"),
	))
	stats.statusEffects.add(StatusEffect(
		flashName = "CRS",
		niceName = "Curse",
		isPositive = false,
		disappearsAfterCombat = false,
		blocksRangedSkills = true,
		blocksMeleeSkills = true,
		icon = icon("Curse"),
		textColor = rgb(110, 65, 173),
		passiveParticleSprites = emptyArray(),
		id = UUID.fromString("917c2cd1-f59d-4d85-9c21-f321feb3ad07"),
	))
	stats.statusEffects.add(StatusEffect(
		flashName = "DRK",
		niceName = "Blindness",
		shortName = "Blind",
		isPositive = false,
		disappearsAfterCombat = false,
		missChance = 50,
		icon = icon("Blind"),
		textColor = rgb(25, 20, 133),
		passiveParticleSprites = passiveSprites(sheet32, intArrayOf(14)),
		id = UUID.fromString("c3d5b0e1-2bac-4497-8762-9dbd098104cb"),
	))
	stats.statusEffects.add(StatusEffect(
		flashName = "SIL",
		niceName = "Silence",
		isPositive = false,
		disappearsAfterCombat = false,
		blocksRangedSkills = true,
		icon = icon("Silence"),
		textColor = rgb(198, 222, 223),
		passiveParticleSprites = emptyArray(),
		id = UUID.fromString("ad763dcd-4f09-4745-a770-96a0c25ccbe7"),
	))
	stats.statusEffects.add(StatusEffect(
		flashName = "SLP",
		niceName = "Sleep",
		isPositive = false,
		disappearsAfterCombat = true,
		disappearAfterHitChance = 100,
		skipTurn = StatusEffect.SkipTurn(100, 0, null),
		disappearChancePerTurn = 25,
		icon = icon("Sleep"),
		textColor = rgb(195, 195, 195),
		passiveParticleSprites = passiveSprites(sheet32, intArrayOf(16)),
		id = UUID.fromString("f2a13009-8436-486c-a2d6-72a510d0d5f7"),
	))
	stats.statusEffects.add(StatusEffect(
		flashName = "PAR",
		niceName = "Paralysis",
		shortName = "Stun",
		isPositive = false,
		disappearsAfterCombat = false,
		skipTurn = StatusEffect.SkipTurn(
			40, rgb(255, 255, 0),
			content.battle.particles.find { it.name == "paralysis_jolt" }!!
		),
		icon = icon("Paralysis"),
		textColor = rgb(223, 221, 42),
		passiveParticleSprites = passiveSprites(sheet16, intArrayOf(5)),
		id = UUID.fromString("b041e68f-1bd5-44d1-bd3b-dd04c4ee3885"),
	))
	stats.statusEffects.add(StatusEffect(
		flashName = "NUM",
		niceName = "Numbness",
		shortName = "Numb",
		isPositive = false,
		disappearsAfterCombat = false,
		blocksMeleeSkills = true,
		blocksBasicAttacks = true,
		icon = icon("Numb"),
		textColor = rgb(187, 75, 34),
		passiveParticleSprites = emptyArray(),
		id = UUID.fromString("ff0f0e3e-12de-4b9f-bb5c-520dbefacf26"),
	))
	stats.statusEffects.add(StatusEffect(
		flashName = "RGN",
		niceName = "Regeneration",
		shortName = "Regen",
		isPositive = true,
		disappearsAfterCombat = true,
		damagePerTurn = StatusEffect.TurnDamage(
			-0.1f, element("LIGHT"),
			content.battle.particles.find { it.name == "regen_oef" }!!,
			rgb(18, 231, 241)
		),
		icon = icon("Regen"),
		textColor = rgb(120, 223, 150),
		passiveParticleSprites = emptyArray(),
		id = UUID.fromString("3f52d1ef-73a2-4f92-ac07-fd6526439968"),
	))
	stats.statusEffects.add(StatusEffect(
		flashName = "PSH",
		niceName = "Shield",
		isPositive = true,
		disappearsAfterCombat = true,
		meleeDamageReduction = 0.5f,
		icon = icon("Shield"),
		textColor = rgb(21, 223, 200),
		passiveParticleSprites = emptyArray(),
		id = UUID.fromString("40fee21c-df5d-4f17-9c81-89cd56bb9a02"),
	))
	stats.statusEffects.add(StatusEffect(
		flashName = "MSH",
		niceName = "M.Shield",
		isPositive = true,
		disappearsAfterCombat = true,
		rangedDamageReduction = 0.5f,
		icon = icon("MagicShield"),
		textColor = rgb(165, 32, 189),
		passiveParticleSprites = emptyArray(),
		id = UUID.fromString("fb56cc76-25ff-4283-8520-45caa6d8b34d"),
	))
	stats.statusEffects.add(StatusEffect(
		flashName = "BSK",
		niceName = "Berserk",
		isPositive = true,
		disappearsAfterCombat = true,
		meleeDamageModifier = 1f,
		isReckless = true,
		icon = icon("Berserk"),
		textColor = rgb(136, 0, 0),
		passiveParticleSprites = emptyArray(),
		id = UUID.fromString("0dada7f4-39d1-4d7d-8e71-0f5e7c32ab40"),
	))
	stats.statusEffects.add(StatusEffect(
		flashName = "HST",
		niceName = "Haste",
		isPositive = true,
		disappearsAfterCombat = true,
		extraTurns = 1,
		icon = icon("Haste"),
		textColor = rgb(214, 162, 65),
		passiveParticleSprites = emptyArray(),
		id = UUID.fromString("d8dfcb47-8213-45ef-af28-a95673dcf050"),
	))
	stats.statusEffects.add(StatusEffect(
		flashName = "UWB",
		niceName = "Aqualong",
		isPositive = true,
		disappearsAfterCombat = false,
		canWaterBreathe = true,
		icon = icon("Aqualung"),
		textColor = rgb(153, 255, 255),
		passiveParticleSprites = emptyArray(),
		id = UUID.fromString("4f30fef7-4569-4a53-be6b-b5a959569a77"),
	))
	stats.statusEffects.add(StatusEffect(
		flashName = "ZOM",
		niceName = "Zombie",
		isPositive = false,
		disappearsAfterCombat = false,
		isZombie = true,
		icon = icon("Zombie"),
		textColor = rgb(0, 102, 102),
		passiveParticleSprites = emptyArray(),
		id = UUID.fromString("5e911a15-5ecc-450b-aaf7-622ae958f24d"),
	))
	stats.statusEffects.add(StatusEffect(
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
		),
		icon = icon("Astral"),
		textColor = rgb(68, 204, 119),
		passiveParticleSprites = emptyArray(),
		id = UUID.fromString("0282808b-bb21-4ead-b5aa-17248f2ed066"),
	))
	stats.statusEffects.add(StatusEffect(
		flashName = "BRK",
		niceName = "Barskin",
		isPositive = true,
		disappearsAfterCombat = true,
		hasBarskin = true,
		icon = icon("Barskin"),
		textColor = rgb(136, 102, 68),
		passiveParticleSprites = emptyArray(),
		id = UUID.fromString("a93e2ab9-f650-4071-bea4-dae823e303b1"),
	))
	stats.statusEffects.add(StatusEffect(
		flashName = "BLD",
		niceName = "Bleed",
		isPositive = false,
		damagePerTurn = StatusEffect.TurnDamage(
			0.1f, element("DARK"),
			content.battle.particles.find { it.name == "bleed_oef" }!!,
			rgb(150, 0, 0)
		),
		disappearsAfterCombat = false,
		icon = icon("Bleed"),
		textColor = rgb(153, 0, 0),
		passiveParticleSprites = emptyArray(),
		id = UUID.fromString("cbfd96f5-a4e1-43cb-855a-1576f0b5232e"),
	))

	for (element in stats.elements) {
		if (element.weakAgainst == null) continue // Skip physical and thauma
		stats.statusEffects.add(StatusEffect(
			flashName = "${element.primaryChar}N1",
			niceName = "Null ${element.properName.lowercase(Locale.ROOT)}",
			isPositive = true,
			disappearsAfterCombat = true,
			nullifiesElement = element,
			icon = icon("null/${element.properName}"),
			textColor = element.color,
			passiveParticleSprites = emptyArray(),
			id = UUID.nameUUIDFromBytes("HardcodedNull${element.rawName}".encodeToByteArray())
		))
	}

	stats.statusEffects.add(StatusEffect(
		flashName = "SHF",
		niceName = "Pyro Shell",
		isPositive = true,
		disappearsAfterCombat = true,
		elementShell = element("FIRE"),
		icon = icon("shell/Pyro"),
		textColor = rgb(204, 102, 0),
		passiveParticleSprites = emptyArray(),
		id = UUID.fromString("e023ca58-bfce-45b2-a005-638e5bbf07ea"),
	))
	stats.statusEffects.add(StatusEffect(
		flashName = "SHW",
		niceName = "Hydro Shell",
		isPositive = true,
		disappearsAfterCombat = true,
		elementShell = element("WATER"),
		icon = icon("shell/Aero"),
		textColor = rgb(0, 170, 204),
		passiveParticleSprites = emptyArray(),
		id = UUID.fromString("29a52d0e-e8fa-41e1-860b-9eba1788a30d"),
	))
	stats.statusEffects.add(StatusEffect(
		flashName = "SHE",
		niceName = "Geo Shell",
		isPositive = true,
		disappearsAfterCombat = true,
		elementShell = element("EARTH"),
		icon = icon("shell/Aero"),
		textColor = rgb(51, 204, 0),
		passiveParticleSprites = emptyArray(),
		id = UUID.fromString("7deded10-364b-4d14-9e29-16796435da8b"),
	))
	stats.statusEffects.add(StatusEffect(
		flashName = "SHA",
		niceName = "Aero Shell",
		isPositive = true,
		disappearsAfterCombat = true,
		elementShell = element("AIR"),
		icon = icon("shell/Aero"),
		textColor = rgb(204, 204, 34),
		passiveParticleSprites = emptyArray(),
		id = UUID.fromString("2a6c42e7-f779-498a-b813-7a90d819c781"),
	))

	stats.statusEffects.add(StatusEffect(
		flashName = "BRN",
		niceName = "Burn?",
		isPositive = false,
		disappearsAfterCombat = true,
		icon = icon("shell/Pyro"),
		textColor = rgb(204, 51, 0),
		passiveParticleSprites = emptyArray(),
		id = UUID.fromString("2e21fe11-7aef-48be-ae20-e37b719612aa"),
	))
}
