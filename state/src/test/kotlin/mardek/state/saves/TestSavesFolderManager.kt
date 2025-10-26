package mardek.state.saves

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class TestSavesFolderManager {

	@Test
	fun testGetCampaignNames() {
		val savesDirectory = Files.createTempDirectory("").toFile()
		assertTrue(savesDirectory.isDirectory)
		savesDirectory.deleteOnExit()

		val childFile = File("$savesDirectory/file")
		assertTrue(childFile.createNewFile())
		childFile.deleteOnExit()

		val emptyChildDirectory = File("$savesDirectory/empty")
		assertTrue(emptyChildDirectory.mkdir())
		emptyChildDirectory.deleteOnExit()

		val textChildDirectory = File("$savesDirectory/text")
		assertTrue(textChildDirectory.mkdir())
		textChildDirectory.deleteOnExit()

		val textFile = File("$textChildDirectory/something.txt")
		assertTrue(textFile.createNewFile())
		textFile.deleteOnExit()

		val properChildDirectory = File("$savesDirectory/proper")
		assertTrue(properChildDirectory.mkdir())
		properChildDirectory.deleteOnExit()

		val saveFile = File("$properChildDirectory/crystal-1234.bits")
		assertTrue(saveFile.createNewFile())
		saveFile.deleteOnExit()

		assertArrayEquals(arrayOf("proper"), SavesFolderManager(savesDirectory).getCampaignNames())
		savesDirectory.deleteRecursively()
	}

	@Test
	fun testIsCampaignNameValid() {
		val dummyDirectory = Files.createTempDirectory("").toFile()
		dummyDirectory.deleteOnExit()
		val manager = SavesFolderManager(dummyDirectory)

		assertFalse(manager.isCampaignNameValid(""))
		assertFalse(manager.isCampaignNameValid("."))
		assertFalse(manager.isCampaignNameValid(".."))
		assertFalse(manager.isCampaignNameValid("k/n"))
		assertFalse(manager.isCampaignNameValid("k\\n"))

		assertTrue(manager.isCampaignNameValid("knokko"))
		assertEquals(0, dummyDirectory.list()!!.size)
	}
}
