package mardek.state.settings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class TestVideoSettings {

	@Test
	fun testSaveAndLoad() {
		val settings = VideoSettings.load(File("/does/not/exist"))
		assertEquals(0, settings.preferredDevice)
		assertTrue(settings.capFps)
		assertFalse(settings.showFps)
		assertEquals(1, settings.framesInFlight)
		assertTrue(settings.delayRendering)
		assertFalse(settings.fullscreen)

		settings.preferredDevice = 123
		settings.capFps = false
		settings.showFps = true
		settings.framesInFlight = 3
		settings.delayRendering = true
		settings.fullscreen = true

		val settingsFile = Files.createTempFile("", "").toFile()
		settingsFile.deleteOnExit()

		settings.save(settingsFile)

		val settings2 = VideoSettings.load(settingsFile)
		assertEquals(123, settings2.preferredDevice)
		assertFalse(settings2.capFps)
		assertTrue(settings2.showFps)
		assertEquals(3, settings.framesInFlight)
		assertTrue(settings.delayRendering)
		assertTrue(settings.fullscreen)
	}
}