package mardek.state.settings

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class TestVideoSettings {

	@Test
	fun testSaveAndLoad() {
		val settings = VideoSettings.load(File("/does/not/exist"))
		Assertions.assertEquals(0, settings.preferredDevice)
		Assertions.assertTrue(settings.capFps)
		Assertions.assertFalse(settings.showFps)
		Assertions.assertEquals(1, settings.framesInFlight)
		Assertions.assertTrue(settings.delayRendering)

		settings.preferredDevice = 123
		settings.capFps = false
		settings.showFps = true
		settings.framesInFlight = 3
		settings.delayRendering = true

		val settingsFile = Files.createTempFile("", "").toFile()
		settingsFile.deleteOnExit()

		settings.save(settingsFile)

		val settings2 = VideoSettings.load(settingsFile)
		Assertions.assertEquals(123, settings2.preferredDevice)
		Assertions.assertFalse(settings2.capFps)
		Assertions.assertTrue(settings2.showFps)
		Assertions.assertEquals(3, settings.framesInFlight)
		Assertions.assertTrue(settings.delayRendering)
	}
}