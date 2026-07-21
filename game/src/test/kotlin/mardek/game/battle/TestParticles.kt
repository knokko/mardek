package mardek.game.battle

import mardek.content.util.Time
import mardek.game.TestingInstance
import mardek.state.ingame.battle.ParticleEmitterState
import mardek.state.util.RenderTiming
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object TestParticles {

	private const val MAGIC_SCALE = 0.0032f

	fun testPoison(instance: TestingInstance) {
		instance.apply {
			val poisonEmitters = content.stats.statusEffects.find { it.flashName == "PSN" }!!.particleEmitters
			assertEquals(1, poisonEmitters.size)

			val emitter = poisonEmitters[0]
			val emitterState = ParticleEmitterState(emitter)
			emitterState.update(Time(123.seconds), RenderTiming(
				Time(123.seconds), System.nanoTime(), Duration.ZERO
			))
			assertEquals(1, emitterState.particles.size)
			val poison = emitterState.particles[0]

			assertTrue(poison.initialWidth >= 4f * MAGIC_SCALE)
			assertTrue(poison.initialHeight >= 4f * MAGIC_SCALE)

			// Simulate 60 flash frames = 2 seconds:
			var flashY = poison.initialY
			var flashGrowth = 1.3f
			var flashWidth = poison.initialWidth
			for (frame in 0 until 60) {
				val timing = RenderTiming(
					Time(123.seconds + frame.seconds / 30),
					System.nanoTime(), Duration.ZERO,
				)
				assertEquals(flashY, poison.computeY(timing), 0.01f)
				assertEquals(flashWidth, poison.computeWidth(timing), 0.01f)
				assertEquals(flashWidth, poison.computeHeight(timing), 0.01f)
				assertEquals(poison.initialX, poison.computeX(timing))
				assertEquals(1f, emitter.opacity.compute(frame.seconds / 30))

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
			emitterState.update(Time(123.seconds), RenderTiming(
				Time(123.seconds), System.nanoTime(), Duration.ZERO
			))
			assertEquals(1, emitterState.particles.size)
			val blind = emitterState.particles[0]

			for (frame in 0 .. 60) {
				val timing = RenderTiming(
					Time(123.seconds + frame.seconds / 30),
					System.nanoTime(), Duration.ZERO,
				)
				assertEquals(blind.initialX, blind.computeX(timing))
				assertEquals(blind.initialY, blind.computeY(timing))
				assertEquals(blind.initialWidth, blind.computeWidth(timing))
				assertEquals(blind.initialHeight, blind.computeHeight(timing))
			}

			assertEquals(0.8f, emitter.opacity.compute(Duration.ZERO), 0.01f)
			assertEquals(0.72f, emitter.opacity.compute(1.seconds / 30), 0.01f)
			assertEquals(0.64f, emitter.opacity.compute(2.seconds / 30), 0.01f)
			assertEquals(0.08f, emitter.opacity.compute(9.seconds / 30), 0.01f)
		}
	}

	fun testSleep(instance: TestingInstance) {
		instance.apply {
			val sleepEmitters = content.stats.statusEffects.find { it.flashName == "SLP" }!!.particleEmitters
			assertEquals(1, sleepEmitters.size)

			val emitter = sleepEmitters[0]
			val emitterState = ParticleEmitterState(emitter)
			emitterState.update(Time(123.seconds), RenderTiming(
				Time(123.seconds), System.nanoTime(), Duration.ZERO
			))
			assertEquals(1, emitterState.particles.size)
			val sleep = emitterState.particles[0]

			assertTrue(sleep.initialRotation <= 0f)
			assertTrue(sleep.initialRotation < 31f)
			assertTrue(sleep.initialWidth >= 4f * MAGIC_SCALE)
			assertTrue(sleep.initialHeight >= 4f * MAGIC_SCALE)

			var size = sleep.initialWidth.toDouble()
			for (frame in 0 until 50) {
				val timing = RenderTiming(
					Time(123.seconds + frame.seconds / 30),
					System.nanoTime(), Duration.ZERO,
				)
				assertEquals(size.toFloat(), sleep.computeWidth(timing), 0.015f)
				assertEquals(size.toFloat(), sleep.computeHeight(timing), 0.015f)
				size *= 1.04

				if (frame == 1) {
					assertEquals(sleep.initialX - 0.5f, sleep.computeX(timing), 0.01f)
					assertEquals(sleep.initialY - 0.6f, sleep.computeY(timing), 0.01f)
					assertEquals(sleep.initialWidth * 1.04f, sleep.computeWidth(timing), 0.01f)
					assertEquals(sleep.initialHeight * 1.04f, sleep.computeHeight(timing), 0.01f)
					assertEquals(0.98f, emitter.opacity.compute(1.seconds / 30), 0.01f)
				}

				if (frame == 10) {
					assertEquals(sleep.initialX - 5f, sleep.computeX(timing), 0.01f)
					assertEquals(sleep.initialY - 6f, sleep.computeY(timing), 0.01f)
					assertEquals(0.8f, emitter.opacity.compute(10.seconds / 30), 0.01f)
				}
			}
		}
	}

	fun testParalysis(instance: TestingInstance) {
		instance.apply {
			val paralysisEmitters = content.stats.statusEffects.find { it.flashName == "PAR" }!!.particleEmitters
			assertEquals(1, paralysisEmitters.size)

			val emitter = paralysisEmitters[0]
			val emitterState = ParticleEmitterState(emitter)
			emitterState.update(Time(123.seconds), RenderTiming(
				Time(123.seconds), System.nanoTime(), Duration.ZERO
			))
			assertEquals(1, emitterState.particles.size)
			val paralysis = emitterState.particles[0]

			assertTrue(paralysis.initialWidth >= 4f * MAGIC_SCALE)
			assertTrue(paralysis.initialHeight >= 4f * MAGIC_SCALE)

			for (frame in 0 until 50) {
				val timing = RenderTiming(
					Time(123.seconds + frame.seconds / 30),
					System.nanoTime(), Duration.ZERO,
				)
				assertEquals(paralysis.initialX, paralysis.computeX(timing))
				assertEquals(paralysis.initialY, paralysis.computeY(timing))
				assertEquals(paralysis.initialWidth, paralysis.computeWidth(timing))
				assertEquals(paralysis.initialHeight, paralysis.computeHeight(timing))
				assertEquals(frame > 1, paralysis.hasExpired(timing))
			}
		}
	}
}
