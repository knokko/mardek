package mardek.importer.stats

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import mardek.content.Content
import mardek.content.sprite.BcSprite
import mardek.content.stats.ElementalResistance
import mardek.content.stats.Resistances
import mardek.content.stats.StatusEffect
import mardek.importer.ui.BcPacker
import mardek.importer.util.compressKimSprite1
import java.awt.image.BufferedImage
import java.util.*
import javax.imageio.ImageIO
import kotlin.collections.ArrayList

private fun icon(name: String) = compressKimSprite1(ImageIO.read(BcPacker::class.java.classLoader.getResource(
	"mardek/importer/stats/effects/$name.png"
)))

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
	))

	stats.statusEffects.add(StatusEffect(
		flashName = "BRN",
		niceName = "Burn?",
		isPositive = false,
		disappearsAfterCombat = true,
		icon = icon("shell/Pyro"),
		textColor = rgb(204, 51, 0),
		passiveParticleSprites = emptyArray(),
	))
}
