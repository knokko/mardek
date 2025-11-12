package mardek.importer.particle

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
import mardek.content.particle.RadialParticleSpawnProperties
import mardek.content.sprite.BcSprite
import mardek.importer.area.parseFlashString
import mardek.importer.util.classLoader
import mardek.importer.util.parseActionScriptNestedList
import mardek.importer.util.parseActionScriptObject
import java.lang.Float.parseFloat
import java.lang.Integer.parseInt
import java.util.ArrayList
import javax.imageio.ImageIO
import kotlin.math.pow
import kotlin.math.roundToInt

internal fun importParticleEmitter(content: Content, rawEmitter: Map<String, String>): ParticleEmitter {
	val flashLifeTime = parseInt(rawEmitter["life"]!!)
	return ParticleEmitter(
		transform = parseEmitterTransform(rawEmitter),
		sprite = getParticleSprite(content, rawEmitter),
		waves = parseEmissionWaves(content, rawEmitter),
		spawn = parseSpawnProperties(rawEmitter),
		dynamics = parseParticleDynamics(rawEmitter),
		size = parseParticleSize(rawEmitter),
		opacity = parseParticleOpacity(rawEmitter),
		lifeTime = FLASH_FRAME * flashLifeTime,
		mirror = rawEmitter.containsKey("flip")
	)
}

private fun parseEmitterTransform(rawEmitter: Map<String, String>): EmitterTransform {
	val spawnList = parseActionScriptNestedList(rawEmitter["spawn"]!!)
	if (spawnList !is ArrayList<*>) throw IllegalArgumentException("Unexpected spawn $spawnList")
	val x = parseFloat(spawnList[0].toString())
	val y = parseFloat(spawnList[1].toString())
	var rotation = 0f
	if (spawnList.size >= 3) rotation = parseFloat(spawnList[2].toString())

	return EmitterTransform(x = x, y = y, rotation = rotation)
}

internal fun getParticleSprite(content: Content, rawEmitter: Map<String, String>): ParticleSprite {
	val spriteSize = parseInt(rawEmitter["sheet"])
	val spriteElement = parseInt(rawEmitter["type"])
	val spriteName = "sheet$spriteSize-element$spriteElement"

	val existing = content.battle.particleSprites.find { it.name == spriteName }
	if (existing != null) return existing

	val sheet = ImageIO.read(classLoader.getResource(
		"mardek/importer/particle/sheet$spriteSize.png"
	))
	val numColumns = sheet.width / spriteSize
	val x = spriteSize * (spriteElement % numColumns)
	val y = spriteSize * (spriteElement / numColumns)
	val image = sheet.getSubimage(x, y, spriteSize, spriteSize)

	val bcSprite = BcSprite(spriteSize, spriteSize, 7)
	bcSprite.bufferedImage = image
	val sprite = ParticleSprite(spriteName, bcSprite)
	content.battle.particleSprites.add(sprite)
	return sprite
}

private fun parseEmissionWaves(content: Content, rawEmitter: Map<String, String>): EmissionWaves {
	val flashDelay = parseInt(rawEmitter["delay"]!!)
	val rawSoundName = rawEmitter["delayedSFX"]
	val delayedSound = if (rawSoundName != null) {
		val soundName = parseFlashString(rawSoundName, "delayed emitter sound name")!!
		content.audio.effects.find { it.flashName == soundName }!!
	} else null
	val rawSkipFrames = rawEmitter["skipframes"]
	val flashPeriod = if (rawSkipFrames != null) 1 + parseInt(rawSkipFrames) else 1
	var period = FLASH_FRAME * flashPeriod
	val particlesPerFrame = parseFloat(rawEmitter["ppf"]!!)
	val particlesPerWave = if (particlesPerFrame < 1f) {
		period /= particlesPerFrame
		1
	} else particlesPerFrame.roundToInt()
	val flashDuration = parseInt(rawEmitter["dur"]!!)

	return EmissionWaves(
		delay = FLASH_FRAME * flashDelay,
		delayedSound = delayedSound,
		period = period,
		particlesPerWave = particlesPerWave,
		numRounds = flashDuration / flashPeriod
	)
}

private fun parseSpawnProperties(rawEmitter: Map<String, String>): ParticleSpawnProperties {
	val rawBase = rawEmitter["Coffset"]
	val (baseX, baseY) = if (rawBase != null) {
		val offsetList = parseActionScriptNestedList(rawBase)
		if (offsetList !is ArrayList<*> || offsetList.size != 2) {
			throw IllegalArgumentException("Unexpected Coffset $offsetList")
		}
		Pair(parseFloat(offsetList[0].toString()), parseFloat(offsetList[1].toString()))
	} else Pair(0f, 0f)

	val rawVariation = rawEmitter["area"]
	val (variationX, variationY) = if (rawVariation != null) {
		val areaList = parseActionScriptNestedList(rawVariation)
		if (areaList !is ArrayList<*> || areaList.size != 2) {
			throw IllegalArgumentException("Unexpected area $areaList")
		}
		Pair(parseFloat(areaList[0].toString()), parseFloat(areaList[1].toString()))
	} else Pair(0f, 0f)

	var shiftX = 0f
	var shiftY = 0f
	var shiftVariationX = 0f
	var shiftVariationY = 0f
	val rawModifiers = rawEmitter["OEFmod"]
	if (rawModifiers != null) {
		val modifierProperties = parseActionScriptObject(rawModifiers)
		val rawModifiedBase = modifierProperties["Coffset"]
		if (rawModifiedBase != null) {
			val offsetList = parseActionScriptNestedList(rawModifiedBase)
			if (offsetList !is ArrayList<*> || offsetList.size != 2) {
				throw IllegalArgumentException("Unexpected OEFmod.Coffset $offsetList")
			}
			shiftX = FLASH_FRAMES_PER_SECOND * parseFloat(offsetList[0].toString())
			shiftY = FLASH_FRAMES_PER_SECOND * parseFloat(offsetList[1].toString())
		}

		val rawModifiedVariation = modifierProperties["area"]
		if (rawModifiedVariation != null) {
			val areaList = parseActionScriptNestedList(rawModifiedVariation)
			if (areaList !is ArrayList<*> || areaList.size != 2) {
				throw IllegalArgumentException("Unexpected OEFmod.area $areaList")
			}
			shiftVariationX = FLASH_FRAMES_PER_SECOND * parseFloat(areaList[0].toString())
			shiftVariationY = FLASH_FRAMES_PER_SECOND * parseFloat(areaList[1].toString())
		}
	}

	val rawMultiplier = rawEmitter["rotmod"]
	val rotationMultiplier = if (rawMultiplier != null) {
		val flashMultiplier = parseFloat(rawMultiplier)
		flashMultiplier.pow(FLASH_FRAMES_PER_SECOND)
	} else 1f

	val rawRotation = rawEmitter["rot"]!!
	val rotation = if (rawRotation == "\"R\"" || rawRotation == "\"theta\"") null else parseFloat(rawRotation)

	val (linear, radial) = if (rawEmitter["mode"] == "RADIAL") {
		Pair(null, parseRadialProperties(rawEmitter))
	} else {
		Pair(parseLinearProperties(rawEmitter), null)
	}

	return ParticleSpawnProperties(
		baseX = baseX, baseY = baseY, shiftX = shiftX, shiftY = shiftY,
		variationX = variationX, variationY = variationY, shiftVariationX = shiftVariationX, shiftVariationY = shiftVariationY,
		rotation = rotation, rotationMultiplier = rotationMultiplier, linear = linear, radial = radial
	)
}

private fun parseLinearProperties(rawEmitter: Map<String, String>): LinearParticleSpawnProperties {
	val velocityList = parseActionScriptNestedList(rawEmitter["vel"]!!)
	if (velocityList !is ArrayList<*> || (velocityList.size != 4 && velocityList.size != 2)) {
		throw IllegalArgumentException("Unexpected linear vel $velocityList")
	}
	var minVelocityY = 0f
	var maxVelocityY = 0f
	if (velocityList.size > 2) {
		minVelocityY = parseFloat(velocityList[2].toString())
		maxVelocityY = parseFloat(velocityList[3].toString())
	}

	var shiftMinVelocityX = 0f
	var shiftMaxVelocityX = 0f
	var shiftMinVelocityY = 0f
	var shiftMaxVelocityY = 0f
	val rawModifiers = rawEmitter["OEFmod"]
	if (rawModifiers != null) {
		val modifiers = parseActionScriptObject(rawModifiers)
		val rawModifiedList = modifiers["vel"]
		if (rawModifiedList != null) {
			val modifiedList = parseActionScriptNestedList(rawModifiedList)
			if (modifiedList !is ArrayList<*> || modifiedList.size != 4) {
				throw IllegalArgumentException("Unexpected linear OEFmod.vel $modifiedList")
			}
			shiftMinVelocityX = parseFloat(modifiedList[0].toString())
			shiftMaxVelocityX = parseFloat(modifiedList[1].toString())
			shiftMinVelocityY = parseFloat(modifiedList[2].toString())
			shiftMaxVelocityY = parseFloat(modifiedList[3].toString())
		}
	}

	return LinearParticleSpawnProperties(
		minVelocityX = FLASH_FRAMES_PER_SECOND * parseFloat(velocityList[0].toString()),
		shiftMinVelocityX = FLASH_FRAMES_PER_SECOND * shiftMinVelocityX,
		maxVelocityX = FLASH_FRAMES_PER_SECOND * parseFloat(velocityList[1].toString()),
		shiftMaxVelocityX = FLASH_FRAMES_PER_SECOND * shiftMaxVelocityX,
		minVelocityY = FLASH_FRAMES_PER_SECOND * minVelocityY,
		shiftMinVelocityY = FLASH_FRAMES_PER_SECOND * shiftMinVelocityY,
		maxVelocityY = FLASH_FRAMES_PER_SECOND * maxVelocityY,
		shiftMaxVelocityY = FLASH_FRAMES_PER_SECOND * shiftMaxVelocityY,
	)
}

private fun parseRadialProperties(rawEmitter: Map<String, String>): RadialParticleSpawnProperties {
	val rawOffset = rawEmitter["offset"]
	val (minRadius, maxRadius) = if (rawOffset != null) {
		val offsetList = parseActionScriptNestedList(rawOffset)
		if (offsetList is ArrayList<*>) {
			if (offsetList.size != 2) {
				throw IllegalArgumentException("Unexpected offset $offsetList")
			}
			Pair(parseFloat(offsetList[0].toString()), parseFloat(offsetList[1].toString()))
		} else {
			val radius = parseFloat(offsetList.toString())
			Pair(radius, radius)
		}

	} else Pair(0f, 0f)

	val rawEvenSpacing = rawEmitter["even_spacing"]
	val rawStartAngle = rawEmitter["start_angle"]
	val evenlySpaced = if (rawStartAngle != null) parseFloat(rawStartAngle)
	else if (rawEvenSpacing == "true") 0f
	else if (rawEvenSpacing != null) parseFloat(rawEvenSpacing)
	else null

	val velocityList = parseActionScriptNestedList(rawEmitter["vel"]!!)
	if (velocityList !is ArrayList<*> || velocityList.size < 2) {
		throw IllegalArgumentException("Unexpected radial vel $velocityList")
	}

	var shiftMinRadius = 0f
	var shiftMaxRadius = 0f
	var shiftEvenlySpaced = 0f
	val rawModifiers = rawEmitter["OEFmod"]
	if (rawModifiers != null) {
		val modifiers = parseActionScriptObject(rawModifiers)
		val rawModifierList = modifiers["offset"]
		if (rawModifierList != null) {
			val offsetList = parseActionScriptNestedList(rawModifierList)
			if (offsetList is ArrayList<*>) {
				if (offsetList.size != 2) {
					throw IllegalArgumentException("Unexpected radial OEFmod.vel $offsetList")
				}
				shiftMinRadius = parseFloat(offsetList[0].toString())
				shiftMaxRadius = parseFloat(offsetList[1].toString())
			} else {
				val shiftRadius = parseFloat(offsetList.toString())
				shiftMinRadius = shiftRadius
				shiftMaxRadius = shiftRadius
			}
		}

		val rawStartAngle = modifiers["start_angle"]
		if (rawStartAngle != null) shiftEvenlySpaced = parseFloat(rawStartAngle)
	}

	return RadialParticleSpawnProperties(
		minRadius = minRadius, maxRadius = maxRadius,
		shiftMinRadius = FLASH_FRAMES_PER_SECOND * shiftMinRadius,
		shiftMaxRadius = FLASH_FRAMES_PER_SECOND * shiftMaxRadius,
		evenlySpaced = evenlySpaced, shiftEvenlySpaced = FLASH_FRAMES_PER_SECOND * shiftEvenlySpaced,
		rotateToMoveDirection = rawEmitter["rot"] == "\"theta\"",
		minVelocity = FLASH_FRAMES_PER_SECOND * parseFloat(velocityList[0].toString()),
		maxVelocity = FLASH_FRAMES_PER_SECOND * parseFloat(velocityList[1].toString()),
	)
}

private fun parseParticleDynamics(rawEmitter: Map<String, String>): ParticleDynamics {
	val rawMod = rawEmitter["mod"]
	val (accelerationX, accelerationY) = if (rawMod != null) {
		val modList = parseActionScriptNestedList(rawMod)
		if (modList !is ArrayList<*> || modList.size != 2) throw IllegalArgumentException("Unexpected mod $modList")
		Pair(parseFloat(modList[0].toString()), parseFloat(modList[1].toString()))
	} else Pair(0f, 0f)

	val rawRadial = rawEmitter["rvelmod"]
	val radialAcceleration = if (rawRadial != null) parseFloat(rawRadial) else 0f

	val rawAcceleration = rawEmitter["accel"]
	val (velocityMultiplierX, velocityMultiplierY) = if (rawAcceleration != null) {
		val accelerationList = parseActionScriptNestedList(rawAcceleration)
		if (accelerationList !is ArrayList<*> || accelerationList.size != 2) {
			throw IllegalArgumentException("Unexpected accel $accelerationList")
		}
		Pair(parseFloat(accelerationList[0].toString()), parseFloat(accelerationList[1].toString()))
	} else Pair(1f, 1f)

	val rawSpin = rawEmitter["spin"]
	val spin = if (rawSpin != null) parseFloat(rawSpin) else 0f

	var shiftAccelerationX = 0f
	var shiftAccelerationY = 0f
	val rawModifiers = rawEmitter["OEFmod"]
	if (rawModifiers != null) {
		val rawShiftList = parseActionScriptObject(rawModifiers)["mod"]
		if (rawShiftList != null) {
			val accelerationList = parseActionScriptNestedList(rawShiftList)
			if (accelerationList !is ArrayList<*> || accelerationList.size != 2) {
				throw IllegalArgumentException("Unexpected OEFmod.mod $accelerationList")
			}
			shiftAccelerationX = parseFloat(accelerationList[0].toString())
			shiftAccelerationY = parseFloat(accelerationList[1].toString())
		}
	}

	/*
	 * Unlike most other quantities, converting the acceleration from flash frames to seconds is non-trivial.
	 *
	 * Flash dynamics: v_f[t+1] = m_f * (v_f[t] + a_f)
	 *
	 * Where
	 * - v_f[t] is the velocity after t frames
	 * - m_f is the velocityMultiplier
	 * - a_f is the acceleration
	 *
	 * To compute the steady-state velocity s_f, we need to solve: s_f = m_f * (s_f + a_f)
	 * -> s_f = m_f * s_f + m_f * a_f
	 * -> s_f - m_f * s_f = m_f * a_f
	 * -> (1 - m_f)s_f = m_f * a_f
	 * -> s_f = m_f * a_f / (1 - m_f)
	 *
	 * In our own format, we want to express t in seconds rather than in flash frames, which requires us to use
	 * different constants m_n and a_n, where m_n must be m_f^30 (1 second = 30 flash frames).
	 *
	 * Normalized dynamics: v_n[t+1] = m_n * (v_n[t] + a_n)
	 * Normalized steady-state velocity: s_n = m_n * (s_n + a_n)
	 * -> s_n = m_n * a_n / (1 - m_n)
	 *
	 * We want the normalized steady-state velocity s_n to be equal to the flash steady-state velocity s_f,
	 * although its numerical value should be 30 times as large. The following derivation shows how we can
	 * compute the correct value of a_n to accomplish this:
	 * s_n = 30 * s_f
	 * -> m_n * a_n / (1 - m_n) = 30 * m_f * a_f / (1 - m_f)
	 * -> a_n = 30 * (1 - m_n) * m_f * a_f / m_n(1 - m_f)
	 * -> a_n = a_f * 30 * (1 - m_n) * m_f / m_n(1 - m_f)
	 */
	var accelerationFactorX = FLASH_FRAMES_PER_SECOND.toFloat() * FLASH_FRAMES_PER_SECOND
	if (velocityMultiplierX != 1f) {
		val normalVelocityMultiplierX = velocityMultiplierX.pow(FLASH_FRAMES_PER_SECOND)
		accelerationFactorX = FLASH_FRAMES_PER_SECOND * (1f - normalVelocityMultiplierX) * velocityMultiplierX /
				(1f - velocityMultiplierX) / normalVelocityMultiplierX
	}
	var accelerationFactorY = FLASH_FRAMES_PER_SECOND.toFloat() * FLASH_FRAMES_PER_SECOND
	if (velocityMultiplierY != 1f) {
		val normalVelocityMultiplierY = velocityMultiplierY.pow(FLASH_FRAMES_PER_SECOND)
		accelerationFactorY = FLASH_FRAMES_PER_SECOND * (1f - normalVelocityMultiplierY) * velocityMultiplierY /
				(1f - velocityMultiplierY) / normalVelocityMultiplierY
	}

	return ParticleDynamics(
		accelerationX = accelerationFactorX * accelerationX,
		accelerationY = accelerationFactorY * accelerationY,
		shiftAccelerationX = accelerationFactorX * shiftAccelerationX,
		shiftAccelerationY = accelerationFactorY * shiftAccelerationY,

		// Fortunately, for all particles such that radialAcceleration != 0,
		// it holds that velocityMultiplierX == velocityMultiplierY
		radialAcceleration = accelerationFactorX * radialAcceleration,
		velocityMultiplierX = velocityMultiplierX.pow(FLASH_FRAMES_PER_SECOND),
		velocityMultiplierY = velocityMultiplierY.pow(FLASH_FRAMES_PER_SECOND),
		spin = FLASH_FRAMES_PER_SECOND * spin,
	)
}

private fun parseParticleSize(rawEmitter: Map<String, String>): ParticleSize {
	val sizeList = parseActionScriptNestedList(rawEmitter["size"]!!)
	if (sizeList !is ArrayList<*> || sizeList.size != 2) throw IllegalArgumentException("Unexpected size $sizeList")

	val rawRandomSize = rawEmitter["rsize"]
	val (minMultiplier, maxMultiplier) = if (rawRandomSize != null) {
		val randomList = parseActionScriptNestedList(rawRandomSize)
		if (randomList !is ArrayList<*> || randomList.size != 2) {
			throw IllegalArgumentException("Unexpected rsize $randomList")
		}
		Pair(parseFloat(randomList[0].toString()), parseFloat(randomList[1].toString()))
	} else Pair(1f, 1f)

	val growList = parseActionScriptNestedList(rawEmitter["grow"]!!)
	if (growList !is ArrayList<*> || growList.size != 2) throw IllegalArgumentException("Unexpected grow $growList")
	val flashGrow = growList.map { parseFloat(it.toString()) }

	var shiftWidth = 0f
	var shiftHeight = 0f
	val rawModifiers = rawEmitter["OEFmod"]
	if (rawModifiers != null) {
		val rawSizeList = parseActionScriptObject(rawModifiers)["size"]
		if (rawSizeList != null) {
			val sizeList = parseActionScriptNestedList(rawSizeList)
			if (sizeList !is ArrayList<*> || sizeList.size != 2) {
				throw IllegalArgumentException("Unexpected OEFmod.size $sizeList")
			}
			shiftWidth = parseFloat(sizeList[0].toString())
			shiftHeight = parseFloat(sizeList[1].toString())
		}
	}

	return ParticleSize(
		baseWidth = parseFloat(sizeList[0].toString()),
		baseHeight = parseFloat(sizeList[1].toString()),
		shiftWidth = FLASH_FRAMES_PER_SECOND * shiftWidth,
		shiftHeight = FLASH_FRAMES_PER_SECOND * shiftHeight,
		minSizeMultiplier = minMultiplier,
		maxSizeMultiplier = maxMultiplier,
		growX = flashGrow[0].pow(FLASH_FRAMES_PER_SECOND),
		growY = flashGrow[1].pow(FLASH_FRAMES_PER_SECOND)
	)
}

private fun parseParticleOpacity(rawEmitter: Map<String, String>): ParticleOpacity {
	val flashAlpha = parseInt(rawEmitter["alpha"]!!)
	val flashFade = parseFloat(rawEmitter["fade"]!!)
	val rawLimit = rawEmitter["maxalpha"]
	val limit = if (rawLimit != null) 0.01f * parseInt(rawLimit) else null
	return ParticleOpacity(initial = 0.01f * flashAlpha, grow = -0.01f * flashFade * FLASH_FRAMES_PER_SECOND, limit = limit)
}
