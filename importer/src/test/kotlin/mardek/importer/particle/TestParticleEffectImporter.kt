package mardek.importer.particle

import mardek.content.Content
import mardek.importer.audio.importAudioContent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertNull
import kotlin.math.pow

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestParticleEffectImporter {

	private val content = Content()

	@BeforeAll
	fun importParticles() {
		importAudioContent(content.audio)
		importParticleEffects(content)
	}

	@Test
	fun testImportEarthQuake() {
		val earthQuake = content.battle.particles.find { it.name == "earthquake" }!!
		assertEquals("earthquake", earthQuake.initialSound!!.flashName)
		assertEquals(2.6666f, earthQuake.damageDelay, 0.01f)

		val quakeEffect = earthQuake.quake!!
		assertEquals(20, quakeEffect.strength)
		assertEquals(4f, quakeEffect.duration, 0.01f)

		// 45 flash frames (1.5 seconds) with 0.15f decay per frame
		assertEquals(20f - 45 * 0.15f, quakeEffect.strength - 1.5f * quakeEffect.decay, 0.01f)

		assertEquals(8, earthQuake.emitters.size)
		val emitter = earthQuake.emitters[0]
		assertEquals(180f, emitter.transform.x)
		assertEquals(-120f, emitter.transform.y)
		assertEquals(1f, emitter.dynamics.velocityMultiplierX)
		assertEquals(1f, emitter.dynamics.velocityMultiplierY)
		assertEquals(0f, emitter.dynamics.accelerationX)
		assertEquals(360f, emitter.dynamics.accelerationY, 0.01f)
		assertEquals(38f / 30f, emitter.waves.delay, 0.001f)
		assertEquals(8, emitter.waves.particlesPerWave)
		assertEquals(0.5f, emitter.size.minSizeMultiplier)
		assertEquals(1.5f, emitter.size.maxSizeMultiplier)
		assertNull(emitter.spawn.rotation)
		assertSame(content.audio.effects.find { it.flashName == "slash2" }!!, emitter.waves.delayedSound)
	}

	@Test
	fun testImportExtraSoundDelays() {
		val razorLeaf = content.battle.particles.find { it.name == "razorleaf" }!!
		assertEquals(3, razorLeaf.extraSoundDelays.size)
		assertEquals(8f / 30f, razorLeaf.extraSoundDelays[0], 0.001f)
	}

	@Test
	fun testImportDerivedParticle() {
		val fireWave = content.battle.particles.find { it.name == "firewave" }!!
		assertEquals("dragon_roar", fireWave.initialSound!!.flashName)
		assertEquals(0, fireWave.emitters.size)

		val inheritance = fireWave.inheritance!!
		assertEquals("mindwave", inheritance.parent.name)
		assertEquals(1, inheritance.overrideSprites!!.size)
		assertTrue(inheritance.overrideSprites!![0].name.contains("element48"))
	}

	@Test
	fun testImportHaste() {
		val haste = content.battle.particles.find { it.name == "haste" }!!
		assertNull(haste.initialSound)
		assertEquals(3, haste.emitters.size)

		val emitter = haste.emitters[1]
		assertEquals("sheet64-element9", emitter.sprite.name)
		assertEquals(3.3333f, emitter.lifeTime, 0.01f)
		assertEquals(0.03333f, emitter.waves.period, 0.001f)
		assertEquals(50, emitter.waves.numRounds)
		assertEquals(-12f, emitter.opacity.grow, 0.01f)
		assertEquals(1f, emitter.opacity.initial)
		assertNull(emitter.spawn.radial)
		assertEquals(80f, emitter.size.baseWidth)
		assertEquals(80f, emitter.size.baseHeight)
		assertEquals(-12f, emitter.spawn.rotation)
		assertEquals(-240f, emitter.dynamics.spin)
		assertEquals(1.07.pow(30), emitter.spawn.rotationMultiplier.toDouble(), 0.01)
	}

	@Test
	fun testSpritesAreShared() {
		val spriteNameList = content.battle.particleSprites.map { it.name }
		assertEquals(spriteNameList.size, spriteNameList.toSet().size)
	}

	@Test
	fun testImportPoisonSting() {
		val hit = content.battle.particles.find { it.name == "sting_psn" }!!
		assertEquals(2, hit.emitters.size)

		run {
			val emitter = hit.emitters[0]
			assertEquals(2f, emitter.spawn.variationX)
			assertEquals(2f, emitter.spawn.variationY)
			assertEquals(1.16.pow(30), emitter.size.growX.toDouble(), 0.01)
			assertEquals(1.16.pow(30), emitter.size.growY.toDouble(), 0.01)
		}

		run {
			val emitter = hit.emitters[1]
			assertEquals(0.9.pow(30), emitter.dynamics.velocityMultiplierX.toDouble(), 0.001)
			assertEquals(0.9.pow(30), emitter.dynamics.velocityMultiplierY.toDouble(), 0.001)

			assertEquals(12, emitter.waves.particlesPerWave)
			assertEquals(1, emitter.waves.numRounds)

			val radial = emitter.spawn.radial!!
			assertEquals(120f, radial.minVelocity)
			assertEquals(240f, radial.maxVelocity)
			assertNull(emitter.spawn.linear)
			assertFalse(radial.rotateToMoveDirection)
		}
	}

	@Test
	fun testImportTsunami() {
		val tsunami = content.battle.particles.find { it.name == "tsunami" }!!

		val emitter = tsunami.emitters[0]
		assertEquals(0f, emitter.spawn.baseX)
		assertEquals(-100f, emitter.spawn.baseY)
		assertEquals(0f, emitter.spawn.shiftX)
		assertEquals(3000f, emitter.spawn.shiftY, 0.01f)
		assertNull(emitter.spawn.radial)

		val linear = emitter.spawn.linear!!
		assertEquals(1320f, linear.minVelocityX, 0.01f)
		assertEquals(1500f, linear.maxVelocityX, 0.01f)
		assertEquals(0f, linear.minVelocityY)
		assertEquals(0f, linear.maxVelocityY)

		assertEquals(0f, linear.shiftMinVelocityX)
		assertEquals(0f, linear.shiftMaxVelocityX)
		assertEquals(0f, linear.shiftMinVelocityY)
		assertEquals(0f, linear.shiftMaxVelocityY)
	}

	@Test
	fun testImportGemsplosionEarth() {
		val gemsplosion = content.battle.particles.find { it.name == "gemsplosion" }!!
		assertEquals(4, gemsplosion.emitters.size)

		run {
			val emitter = gemsplosion.emitters[0]
			assertNull(emitter.spawn.linear)

			val radial = emitter.spawn.radial!!
			assertEquals(0f, radial.evenlySpaced)
			assertTrue(radial.rotateToMoveDirection)
		}

		run {
			val emitter = gemsplosion.emitters[2]
			assertNull(emitter.spawn.linear)

			val radial = emitter.spawn.radial!!
			assertEquals(30f, radial.evenlySpaced)
			assertTrue(radial.rotateToMoveDirection)
		}
	}

	@Test
	fun testImportSunFlare() {
		val flare = content.battle.particles.find { it.name == "sunflare" }!!
		val emitter = flare.emitters[0]
		assertEquals(4f / 30f, emitter.waves.period, 0.001f)
		assertEquals(8, emitter.waves.particlesPerWave)
		assertEquals(5, emitter.waves.numRounds)

		val radial = emitter.spawn.radial!!
		assertEquals(60f, radial.minRadius)
		assertEquals(60f, radial.maxRadius)
		assertEquals(0f, radial.evenlySpaced)
		assertTrue(radial.rotateToMoveDirection)
	}

	@Test
	fun testImportPyromagia() {
		val pyromagia = content.battle.particles.find { it.name == "pyromagia" }!!
		val emitter = pyromagia.emitters[0]
		assertEquals(-2100f, emitter.dynamics.radialAcceleration, 50f)
		assertEquals(1f, emitter.opacity.limit)
		assertEquals(0.9f, emitter.opacity.grow, 0.001f)
		assertEquals(0.1f, emitter.opacity.initial, 0.001f)

		val radial = emitter.spawn.radial!!
		assertEquals(0f, radial.evenlySpaced)
		assertTrue(radial.rotateToMoveDirection)
		assertEquals(360f, emitter.dynamics.spin, 0.01f)
	}

	@Test
	fun testImportCrescendoSlash() {
		val crescendo = content.battle.particles.find { it.name == "crescendoslash" }!!
		assertEquals(4, crescendo.emitters.size)
		assertFalse(crescendo.emitters[0].mirror)
		assertTrue(crescendo.emitters[1].mirror)
		assertFalse(crescendo.emitters[2].mirror)
		assertFalse(crescendo.emitters[3].mirror)
	}

	@Test
	fun testImportWildFire() {
		val fire = content.battle.particles.find { it.name == "wildfire" }!!

		run {
			val emitter = fire.emitters[0]
			assertEquals(150f, emitter.spawn.shiftX)
			assertEquals(0f, emitter.spawn.shiftY)
			assertEquals(60f, emitter.size.shiftWidth)
			assertEquals(75f, emitter.size.shiftHeight)
			assertEquals(0f, emitter.dynamics.shiftAccelerationX)
			assertEquals(-63f, emitter.dynamics.shiftAccelerationY, 0.01f)

			val linear = emitter.spawn.linear!!
			assertEquals(0f, linear.shiftMinVelocityX)
			assertEquals(0f, linear.shiftMaxVelocityX)
			assertEquals(6f, linear.shiftMinVelocityY)
			assertEquals(6f, linear.shiftMaxVelocityY)
		}

		run {
			val emitter = fire.emitters[2]
			assertEquals(150f, emitter.spawn.shiftX)
			assertEquals(0f, emitter.spawn.shiftY)
			assertEquals(60f, emitter.size.shiftWidth)
			assertEquals(60f, emitter.size.shiftHeight)
		}
	}

	@Test
	fun testImportNullFire() {
		val nullFire = content.battle.particles.find { it.name == "null_fire" }!!
		assertSame(content.audio.effects.find { it.flashName == "stone1" }!!, nullFire.damageSound)
		val emitter = nullFire.emitters[0]
		val radial = emitter.spawn.radial!!
		assertEquals(-90f, radial.shiftMinRadius)
		assertEquals(-90f, radial.shiftMaxRadius)
		assertEquals(180f, radial.shiftEvenlySpaced, 0.01f)
	}

	@Test
	fun testImportFireStorm() {
		val fireStorm = content.battle.particles.find { it.name == "firestorm" }!!
		val emitter = fireStorm.emitters[4]
		assertEquals(240f, emitter.spawn.shiftVariationX, 0.1f)
		assertEquals(300f, emitter.spawn.shiftVariationY, 0.1f)
	}

	@Test
	fun testImportSunder() {
		val sunder = content.battle.particles.find { it.name == "sunder" }!!
		val emitter = sunder.emitters[0]
		assertEquals(45f, emitter.transform.rotation)
	}
}
