package mardek.state.settings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class TestAudioSettings {

	@Test
	fun testSaveAndLoad() {
		val settings = AudioSettings.load(File("/does/not/exist"))
		assertEquals(50, settings.masterVolume)
		assertEquals(100, settings.musicVolume)
		assertEquals(100, settings.soundEffectVolume)
		assertFalse(settings.playReactionMasteryJingle)

		settings.masterVolume = 70
		settings.musicVolume = 40
		settings.soundEffectVolume = 80
		settings.playReactionMasteryJingle = true

		val settingsFile = Files.createTempFile("", "").toFile()
		settingsFile.deleteOnExit()

		settings.save(settingsFile)

		val settings2 = AudioSettings.load(settingsFile)
		assertEquals(70, settings2.masterVolume)
		assertEquals(40, settings2.musicVolume)
		assertEquals(80, settings2.soundEffectVolume)
		assertTrue(settings2.playReactionMasteryJingle)
	}
}