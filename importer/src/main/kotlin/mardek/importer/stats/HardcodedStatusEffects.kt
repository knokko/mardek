package mardek.importer.stats

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import mardek.content.Content
import mardek.content.particle.EmissionWaves
import mardek.content.particle.EmitterTransform
import mardek.content.particle.LinearParticleSpawnProperties
import mardek.content.particle.ParticleDynamics
import mardek.content.particle.ParticleEmitter
import mardek.content.particle.ParticleOpacity
import mardek.content.particle.ParticleSize
import mardek.content.particle.ParticleSpawnProperties
import mardek.content.particle.ParticleSprite
import mardek.content.sprite.BcSprite
import mardek.content.stats.ElementalResistance
import mardek.content.stats.Resistances
import mardek.content.stats.StatusEffect
import mardek.importer.util.classLoader
import mardek.importer.util.loadBc7Sprite
import java.awt.image.BufferedImage
import java.util.*
import javax.imageio.ImageIO
import kotlin.collections.ArrayList
import kotlin.math.pow

private fun icon(name: String) = loadBc7Sprite("mardek/importer/stats/effects/$name.png")

fun addStatusEffects(content: Content) {
	val stats = content.stats
	fun element(properName: String) = stats.elements.find { it.properName == properName }!!

	val sheet16 = ImageIO.read(classLoader.getResource(
		"mardek/importer/particle/sheet16.png"
	))
	val sheet32 = ImageIO.read(classLoader.getResource(
		"mardek/importer/particle/sheet32.png"
	))

	fun passiveSprites(atlas: BufferedImage, indices: IntArray) = indices.map { index ->
		val oldSize = if (atlas === sheet16) 16 else 32
		val newSize = oldSize + 2
		val sprite = BcSprite(newSize, newSize, 0)
		sprite.bufferedImage = BufferedImage(newSize, newSize, BufferedImage.TYPE_INT_ARGB)
		val oldImage = atlas.getSubimage(oldSize * (index % 10), oldSize * (index / 10), oldSize, oldSize)
		for (oldY in 0 until oldSize) {
			for (oldX in 0 until oldSize) {
				(sprite.bufferedImage as BufferedImage).setRGB(
					oldX + 1, oldY + 1, oldImage.getRGB(oldX, oldY)
				)
			}
		}

		sprite
	}.toTypedArray()

	val poisonParticleSprite = ParticleSprite(
		"Poison", passiveSprites(sheet16, intArrayOf(4))[0]
	)
	content.battle.particleSprites.add(poisonParticleSprite)
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
		innerTextColor = rgb(128, 221, 70),
		outerTextColor = rgb(96, 189, 38),
		particleEmitters = arrayOf(
			ParticleEmitter(
				transform = EmitterTransform(),
				sprite = poisonParticleSprite,
				waves = EmissionWaves(
					delay = 0f, delayedSound = null, period = 0.2f,
					particlesPerWave = 1, numRounds = null,
				),
				spawn = ParticleSpawnProperties(
					baseX = 4f, baseY = -2f,
					shiftX = 0f, shiftY = 0f,
					variationX = 8f, variationY = 8f,
					shiftVariationX = 0f, shiftVariationY = 0f,
					rotation = 0f, rotationVariation = 0f, rotationMultiplier = 1f,
					linear = LinearParticleSpawnProperties(
						minVelocityX = 0f, shiftMinVelocityX = 0f,
						maxVelocityX = 0f, shiftMaxVelocityX = 0f,
						minVelocityY = -30f, shiftMinVelocityY = 0f,
						maxVelocityY = -30f, shiftMaxVelocityY = 0f,
					),
					radial = null,
				),
				dynamics = ParticleDynamics(),
				size = ParticleSize(
					baseWidth = 4f, baseHeight = 4f,
					shiftWidth = 0f, shiftHeight = 0f,
					minSizeMultiplier = 1f, maxSizeMultiplier = 9f / 4f,
					growX = 1.3f, growY = 1.3f,
					dynamicGrowX = -1.5f, dynamicGrowY = -1.5f,
				),
				opacity = ParticleOpacity(),
				lifeTime = 5f,
				mirror = false,
			)
		),
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
		innerTextColor = rgb(194, 171, 228),
		outerTextColor = rgb(162, 139, 196),
		particleEmitters = emptyArray(),
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
		innerTextColor = rgb(142, 97, 205),
		outerTextColor = rgb(110, 65, 173),
		particleEmitters = emptyArray(),
		id = UUID.fromString("917c2cd1-f59d-4d85-9c21-f321feb3ad07"),
	))

	val blindParticleSprite = ParticleSprite(
		"Blindness", passiveSprites(sheet32, intArrayOf(14))[0]
	)
	content.battle.particleSprites.add(blindParticleSprite)
	stats.statusEffects.add(StatusEffect(
		flashName = "DRK",
		niceName = "Blindness",
		shortName = "Blind",
		isPositive = false,
		disappearsAfterCombat = false,
		missChance = 50,
		icon = icon("Blind"),
		innerTextColor = rgb(57, 52, 165),
		outerTextColor = rgb(25, 20, 133),
		particleEmitters = arrayOf(
			ParticleEmitter(
				transform = EmitterTransform(),
				sprite = blindParticleSprite,
				waves = EmissionWaves(
					delay = 0f, delayedSound = null, period = 0.13f,
					particlesPerWave = 1, numRounds = null,
				),
				spawn = ParticleSpawnProperties(
					baseX = 0f, baseY = 2f,
					shiftX = 0f, shiftY = 0f,
					variationX = 8f, variationY = 6f,
					shiftVariationX = 0f, shiftVariationY = 0f,
					rotation = 0f, rotationVariation = 0f, rotationMultiplier = 1f,
					linear = LinearParticleSpawnProperties(),
					radial = null,
				),
				dynamics = ParticleDynamics(),
				size = ParticleSize(
					baseWidth = 8f, baseHeight = 8f,
					shiftWidth = 0f, shiftHeight = 0f,
					minSizeMultiplier = 1f, maxSizeMultiplier = 3f,
					growX = 1f, growY = 1f,
					dynamicGrowX = 0f, dynamicGrowY = 0f,
				),
				opacity = ParticleOpacity(initial = 0.8f, grow = -2.4f, limit = null),
				lifeTime = 1f,
				mirror = false,
			)
		),
		id = UUID.fromString("c3d5b0e1-2bac-4497-8762-9dbd098104cb"),
	))
	stats.statusEffects.add(StatusEffect(
		flashName = "SIL",
		niceName = "Silence",
		isPositive = false,
		disappearsAfterCombat = false,
		blocksRangedSkills = true,
		icon = icon("Silence"),
		innerTextColor = rgb(230, 254, 255),
		outerTextColor = rgb(198, 222, 223),
		particleEmitters = emptyArray(),
		id = UUID.fromString("ad763dcd-4f09-4745-a770-96a0c25ccbe7"),
	))

	val sleepParticleSprite = ParticleSprite(
		"Sleep", passiveSprites(sheet32, intArrayOf(16))[0]
	)
	content.battle.particleSprites.add(sleepParticleSprite)
	stats.statusEffects.add(StatusEffect(
		flashName = "SLP",
		niceName = "Sleep",
		isPositive = false,
		disappearsAfterCombat = true,
		disappearAfterHitChance = 100,
		skipTurn = StatusEffect.SkipTurn(100, 0, null),
		disappearChancePerTurn = 25,
		icon = icon("Sleep"),
		innerTextColor = rgb(227, 227, 227),
		outerTextColor = rgb(195, 195, 195),
		particleEmitters = arrayOf(
			ParticleEmitter(
				transform = EmitterTransform(),
				sprite = sleepParticleSprite,
				waves = EmissionWaves(
					delay = 0f, delayedSound = null, period = 1f,
					particlesPerWave = 1, numRounds = null,
				),
				spawn = ParticleSpawnProperties(
					baseX = 6f, baseY = -6f,
					shiftX = 0f, shiftY = 0f,
					variationX = 4f, variationY = 4f,
					shiftVariationX = 0f, shiftVariationY = 0f,
					rotation = 15f, rotationVariation = 30f, rotationMultiplier = 1f,
					linear = LinearParticleSpawnProperties(
						minVelocityX = 15f, minVelocityY = -18f,
						maxVelocityX = 15f, maxVelocityY = -18f,
						shiftMinVelocityX = 0f, shiftMinVelocityY = 0f,
						shiftMaxVelocityX = 0f, shiftMaxVelocityY = 0f,
					),
					radial = null,
				),
				dynamics = ParticleDynamics(),
				size = ParticleSize(
					baseWidth = 4f, baseHeight = 4f,
					shiftWidth = 0f, shiftHeight = 0f,
					minSizeMultiplier = 1f, maxSizeMultiplier = 2f,
					growX = 1.04.pow(30).toFloat(), growY = 1.04.pow(30).toFloat(),
					dynamicGrowX = 0f, dynamicGrowY = 0f,
				),
				opacity = ParticleOpacity(initial = 1f, grow = -0.6f, limit = null),
				lifeTime = 2f,
				mirror = false,
			)
		),
		id = UUID.fromString("f2a13009-8436-486c-a2d6-72a510d0d5f7"),
	))

	val paralysisParticleSprite = ParticleSprite(
		"Paralysis", passiveSprites(sheet16, intArrayOf(5))[0]
	)
	content.battle.particleSprites.add(paralysisParticleSprite)
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
		innerTextColor = rgb(255, 253, 74),
		outerTextColor = rgb(223, 221, 42),
		particleEmitters = arrayOf(ParticleEmitter(
			transform = EmitterTransform(),
			sprite = paralysisParticleSprite,
			waves = EmissionWaves(
				delay = 0f, delayedSound = null, period = 0.9f,
				particlesPerWave = 1, numRounds = null,
			),
			spawn = ParticleSpawnProperties(
				baseX = 0f, baseY = 4f,
				shiftX = 0f, shiftY = 0f,
				variationX = 32f, variationY = 32f,
				shiftVariationX = 0f, shiftVariationY = 0f,
				rotation = 180f, rotationVariation = 360f, rotationMultiplier = 1f,
				linear = LinearParticleSpawnProperties(),
				radial = null,
			),
			dynamics = ParticleDynamics(),
			size = ParticleSize(
				baseWidth = 4f, baseHeight = 4f,
				shiftWidth = 0f, shiftHeight = 0f,
				minSizeMultiplier = 1f, maxSizeMultiplier = 4f,
				growX = 1f, growY = 1f,
				dynamicGrowX = 0f, dynamicGrowY = 0f,
			),
			opacity = ParticleOpacity(),
			lifeTime = 0.035f,
			mirror = false,
		)),
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
		innerTextColor = rgb(219, 107, 66),
		outerTextColor = rgb(187, 75, 34),
		particleEmitters = emptyArray(),
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
		innerTextColor = rgb(152, 255, 182),
		outerTextColor = rgb(120, 223, 150),
		particleEmitters = emptyArray(),
		id = UUID.fromString("3f52d1ef-73a2-4f92-ac07-fd6526439968"),
	))
	stats.statusEffects.add(StatusEffect(
		flashName = "PSH",
		niceName = "Shield",
		isPositive = true,
		disappearsAfterCombat = true,
		meleeDamageReduction = 0.5f,
		icon = icon("Shield"),
		innerTextColor = rgb(52, 223, 232),
		outerTextColor = rgb(21, 223, 200),
		particleEmitters = emptyArray(),
		id = UUID.fromString("40fee21c-df5d-4f17-9c81-89cd56bb9a02"),
	))
	stats.statusEffects.add(StatusEffect(
		flashName = "MSH",
		niceName = "M.Shield",
		isPositive = true,
		disappearsAfterCombat = true,
		rangedDamageReduction = 0.5f,
		icon = icon("MagicShield"),
		innerTextColor = rgb(197, 64, 221),
		outerTextColor = rgb(165, 32, 189),
		particleEmitters = emptyArray(),
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
		innerTextColor = rgb(170, 0, 0),
		outerTextColor = rgb(136, 0, 0),
		particleEmitters = emptyArray(),
		id = UUID.fromString("0dada7f4-39d1-4d7d-8e71-0f5e7c32ab40"),
	))
	stats.statusEffects.add(StatusEffect(
		flashName = "HST",
		niceName = "Haste",
		isPositive = true,
		disappearsAfterCombat = true,
		extraTurns = 1,
		icon = icon("Haste"),
		innerTextColor = rgb(246, 194, 97),
		outerTextColor = rgb(214, 162, 65),
		particleEmitters = emptyArray(),
		id = UUID.fromString("d8dfcb47-8213-45ef-af28-a95673dcf050"),
	))
	stats.statusEffects.add(StatusEffect(
		flashName = "UWB",
		niceName = "Aqualong",
		isPositive = true,
		disappearsAfterCombat = false,
		canWaterBreathe = true,
		icon = icon("Aqualung"),
		innerTextColor = rgb(153, 255, 255),
		outerTextColor = rgb(120, 200, 200),
		particleEmitters = emptyArray(),
		id = UUID.fromString("4f30fef7-4569-4a53-be6b-b5a959569a77"),
	))
	stats.statusEffects.add(StatusEffect(
		flashName = "ZOM",
		niceName = "Zombie",
		isPositive = false,
		disappearsAfterCombat = false,
		isZombie = true,
		icon = icon("Zombie"),
		innerTextColor = rgb(0, 136, 136),
		outerTextColor = rgb(0, 102, 102),
		particleEmitters = emptyArray(),
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
		innerTextColor = rgb(102, 255, 153),
		outerTextColor = rgb(68, 204, 119),
		particleEmitters = emptyArray(),
		id = UUID.fromString("0282808b-bb21-4ead-b5aa-17248f2ed066"),
	))
	stats.statusEffects.add(StatusEffect(
		flashName = "BRK",
		niceName = "Barskin",
		isPositive = true,
		disappearsAfterCombat = true,
		hasBarskin = true,
		icon = icon("Barskin"),
		innerTextColor = rgb(170, 136, 102),
		outerTextColor = rgb(136, 102, 68),
		particleEmitters = emptyArray(),
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
		innerTextColor = rgb(204, 0, 0),
		outerTextColor = rgb(153, 0, 0),
		particleEmitters = emptyArray(),
		id = UUID.fromString("cbfd96f5-a4e1-43cb-855a-1576f0b5232e"),
	))

	val colorMap = mapOf(
		Pair("FIRE", Pair(rgb(255, 153, 0), rgb(204, 102, 0))),
		Pair("WATER", Pair(rgb(0, 221, 255), rgb(0, 170, 204))),
		Pair("AIR", Pair(rgb(255, 255, 85), rgb(204, 204, 34))),
		Pair("EARTH", Pair(rgb(102, 255, 0), rgb(51, 204, 0))),
		Pair("LIGHT", Pair(rgb(255, 255, 255), rgb(204, 84, 204))),
		Pair("DARK", Pair(rgb(85, 0, 95), rgb(0, 0, 0))),
		Pair("ETHER", Pair(rgb(51, 255, 153), rgb(0, 204, 102))),
		Pair("FIG", Pair(rgb(207, 0, 204), rgb(159, 0, 153))),
	)

	for (element in stats.elements) {
		if (element.weakAgainst == null) continue // Skip physical and thauma
		val (innerColor, outerColor) = colorMap[element.rawName]!!
		stats.statusEffects.add(StatusEffect(
			flashName = "${element.primaryChar}N1",
			niceName = "Null ${element.properName.lowercase(Locale.ROOT)}",
			isPositive = true,
			disappearsAfterCombat = true,
			nullifiesElement = element,
			icon = icon("null/${element.properName}"),
			innerTextColor = innerColor,
			outerTextColor = outerColor,
			particleEmitters = emptyArray(),
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
		outerTextColor = rgb(255, 153, 0),
		innerTextColor = rgb(204, 102, 0),
		particleEmitters = emptyArray(),
		id = UUID.fromString("e023ca58-bfce-45b2-a005-638e5bbf07ea"),
	))
	stats.statusEffects.add(StatusEffect(
		flashName = "SHW",
		niceName = "Hydro Shell",
		isPositive = true,
		disappearsAfterCombat = true,
		elementShell = element("WATER"),
		icon = icon("shell/Aero"),
		innerTextColor = rgb(0, 221, 255),
		outerTextColor = rgb(0, 170, 204),
		particleEmitters = emptyArray(),
		id = UUID.fromString("29a52d0e-e8fa-41e1-860b-9eba1788a30d"),
	))
	stats.statusEffects.add(StatusEffect(
		flashName = "SHE",
		niceName = "Geo Shell",
		isPositive = true,
		disappearsAfterCombat = true,
		elementShell = element("EARTH"),
		icon = icon("shell/Aero"),
		innerTextColor = rgb(102, 255, 0),
		outerTextColor = rgb(51, 204, 0),
		particleEmitters = emptyArray(),
		id = UUID.fromString("7deded10-364b-4d14-9e29-16796435da8b"),
	))
	stats.statusEffects.add(StatusEffect(
		flashName = "SHA",
		niceName = "Aero Shell",
		isPositive = true,
		disappearsAfterCombat = true,
		elementShell = element("AIR"),
		icon = icon("shell/Aero"),
		innerTextColor = rgb(255, 255, 85),
		outerTextColor = rgb(204, 204, 34),
		particleEmitters = emptyArray(),
		id = UUID.fromString("2a6c42e7-f779-498a-b813-7a90d819c781"),
	))
}
