package mardek.renderer

import mardek.state.VideoSettings
import java.nio.file.Files
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File

class TestVideoSettings {

	@Test
	fun testSaveAndLoad() {
		val settings = VideoSettings.load(File("/does/not/exist"))
		assertEquals(0, settings.preferredDevice)
		assertTrue(settings.capFps)
		assertFalse(settings.showFps)

		settings.preferredDevice = 123
		settings.capFps = false
		settings.showFps = true

		val settingsFile = Files.createTempFile("", "").toFile()
		settingsFile.deleteOnExit()

		settings.save(settingsFile)

		val settings2 = VideoSettings.load(settingsFile)
		assertEquals(123, settings2.preferredDevice)
		assertFalse(settings2.capFps)
		assertTrue(settings2.showFps)
	}
}
