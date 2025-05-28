package mardek.importer.particle

import mardek.content.Content
import mardek.importer.audio.importAudioContent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestParticleEffectImporter {

	private val content = Content()

	@BeforeAll
	fun importParticles() {
		importAudioContent(content.audio)
		importParticleEffects(content)
	}

	@Test
	fun testImportQuake() {
		val earthQuake = content.battle.particles.find { it.name == "earthquake" }!!
		assertEquals("earthquake", earthQuake.initialSound!!.flashName)

		val quakeEffect = earthQuake.quake!!
		assertEquals(20, quakeEffect.strength)
		assertEquals(4.0, quakeEffect.duration.toDouble(DurationUnit.SECONDS), 0.01)

		// 45 flash frames (1.5 seconds) with 0.15f decay per frame
		assertEquals(20f - 45 * 0.15f, quakeEffect.strength - 1.5f * quakeEffect.decay, 0.01f)
	} // TODO derive: newTypes, sfx, newSheets

	// TODO test pfx_firewave:{derive:"mindwave",newTypes:[48],sfx:"dragon_roar"}
}
