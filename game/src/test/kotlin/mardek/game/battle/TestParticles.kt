package mardek.game.battle

import mardek.game.TestingInstance
import mardek.state.ingame.battle.ParticleEmitterState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

object TestParticles {

	fun testPoison(instance: TestingInstance) {
		instance.apply {
			val poisonEmitters = content.stats.statusEffects.find { it.flashName == "PSN" }!!.particleEmitters
			assertEquals(1, poisonEmitters.size)

			val emitter = poisonEmitters[0]
			val emitterState = ParticleEmitterState(emitter)
			emitterState.update(123L, 123L)
			assertEquals(1, emitterState.particles.size)
			val poison = emitterState.particles[0]

			assertTrue(poison.initialWidth >= 4f)
			assertTrue(poison.initialHeight >= 4f)

			// Simulate 60 flash frames = 2 seconds:
			var flashY = poison.initialY
			var flashGrowth = 1.3f
			var flashWidth = poison.initialWidth
			for (frame in 0 until 60) {
				val passedNanoTime = frame * 1000_000_000L / 30
				val time = poison.spawnTime + passedNanoTime
				assertEquals(flashY, poison.computeY(time), 0.01f)
				assertEquals(flashWidth, poison.computeWidth(time), 0.01f)
				assertEquals(flashWidth, poison.computeHeight(time), 0.01f)
				assertEquals(poison.initialX, poison.computeX(time))
				assertEquals(1f, emitter.opacity.compute(frame / 30f))

				flashWidth *= flashGrowth
				flashGrowth -= 0.05f
				flashY -= 1f
			}
		}
	}

	fun testBlindness(instance: TestingInstance) {
		instance.apply {
			val blindnessEmitters = content.stats.statusEffects.find { it.flashName == "DRK" }!!.particleEmitters
			assertEquals(1, blindnessEmitters.size)

			val emitter = blindnessEmitters[0]
			val emitterState = ParticleEmitterState(emitter)
			emitterState.update(123L, 123L)
			assertEquals(1, emitterState.particles.size)
			val blind = emitterState.particles[0]

			val frameLength = 33_333_333L
			for (frame in 0 .. 60) {
				val time = System.nanoTime() + frame * frameLength
				assertEquals(blind.initialX, blind.computeX(time))
				assertEquals(blind.initialY, blind.computeY(time))
				assertEquals(blind.initialWidth, blind.computeWidth(time))
				assertEquals(blind.initialHeight, blind.computeHeight(time))
			}

			assertEquals(0.8f, emitter.opacity.compute(0f), 0.01f)
			assertEquals(0.72f, emitter.opacity.compute(1f / 30f), 0.01f)
			assertEquals(0.64f, emitter.opacity.compute(2f / 30f), 0.01f)
			assertEquals(0.08f, emitter.opacity.compute(9f / 30f), 0.01f)
		}
	}

	fun testSleep(instance: TestingInstance) {
		instance.apply {
			val sleepEmitters = content.stats.statusEffects.find { it.flashName == "SLP" }!!.particleEmitters
			assertEquals(1, sleepEmitters.size)

			val emitter = sleepEmitters[0]
			val emitterState = ParticleEmitterState(emitter)
			emitterState.update(123L, 123L)
			assertEquals(1, emitterState.particles.size)
			val sleep = emitterState.particles[0]

			assertTrue(sleep.initialRotation >= 0f)
			assertTrue(sleep.initialRotation < 31f)
			assertTrue(sleep.initialWidth >= 4f)
			assertTrue(sleep.initialHeight >= 4f)

			val frameLength = 33_333_333L
			val frame1 = sleep.spawnTime + frameLength
			assertEquals(sleep.initialX + 0.5f, sleep.computeX(frame1), 0.01f)
			assertEquals(sleep.initialY - 0.6f, sleep.computeY(frame1), 0.01f)
			assertEquals(sleep.initialWidth * 1.04f, sleep.computeWidth(frame1), 0.01f)
			assertEquals(sleep.initialHeight * 1.04f, sleep.computeHeight(frame1), 0.01f)
			assertEquals(0.98f, emitter.opacity.compute(1f / 30f), 0.01f)

			val frame10 = sleep.spawnTime + 10 * frameLength
			assertEquals(sleep.initialX + 5f, sleep.computeX(frame10), 0.01f)
			assertEquals(sleep.initialY - 6f, sleep.computeY(frame10), 0.01f)
			assertEquals(0.8f, emitter.opacity.compute(10f / 30f), 0.01f)

			var size = sleep.initialWidth.toDouble()
			for (frame in 0 until 50) {
				val time = sleep.spawnTime + frame * frameLength
				assertEquals(size.toFloat(), sleep.computeWidth(time), 0.015f)
				assertEquals(size.toFloat(), sleep.computeHeight(time), 0.015f)
				size *= 1.04
			}
		}
	}

	fun testParalysis(instance: TestingInstance) {
		instance.apply {
			val paralysisEmitters = content.stats.statusEffects.find { it.flashName == "PAR" }!!.particleEmitters
			assertEquals(1, paralysisEmitters.size)

			val emitter = paralysisEmitters[0]
			val emitterState = ParticleEmitterState(emitter)
			emitterState.update(123L, 123L)
			assertEquals(1, emitterState.particles.size)
			val paralysis = emitterState.particles[0]

			assertTrue(paralysis.initialWidth >= 4f)
			assertTrue(paralysis.initialHeight >= 4f)

			val frameLength = 33_333_333L
			for (frame in 0 until 50) {
				val time = paralysis.spawnTime + frame * frameLength
				assertEquals(paralysis.initialX, paralysis.computeX(time))
				assertEquals(paralysis.initialY, paralysis.computeY(time))
				assertEquals(paralysis.initialWidth, paralysis.computeWidth(time))
				assertEquals(paralysis.initialHeight, paralysis.computeHeight(time))
				assertEquals(frame > 1, paralysis.hasExpired(time))
			}
		}
	}
}
