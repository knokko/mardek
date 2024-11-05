package mardek.importer.area

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class TestAreaSprites {

	@Suppress("unused")
	fun fixNames() {
		val folder = File("src/main/resources/mardek/importer/area/sheets/objects")
		val oldFiles = folder.listFiles()
		println("oldFiles are ${oldFiles.contentToString()}")

		for (file in oldFiles) {
			val oldName = file.name
			val target = "_obj_"
			var index = oldName.indexOf(target)
			if (index != -1) {
				index += target.length
				Files.move(file.toPath(), File("${file.parentFile}/${oldName.substring(index)}").toPath())
			}
		}
	}

	@Test
	fun testGetDeugan() {
		val areaSprites = AreaSprites()
		val deugan = areaSprites.getCharacter("deugan_hero")
		assertEquals("deugan_hero", deugan.flashName)
		assertEquals(8, deugan.sprites!!.size)
		assertNull(deugan.indices)

		for (sprite in deugan.sprites!!) {
			assertEquals(16, sprite.width)
			assertEquals(16, sprite.height)
		}
	}

	@Test
	fun testGetSolaar() {
		val areaSprites = AreaSprites()
		val solaar = areaSprites.getCharacter("solaar")
		assertEquals("solaar", solaar.flashName)
		assertEquals(9, solaar.sprites!!.size)
		assertNull(solaar.indices)

		for (sprite in solaar.sprites!!) {
			assertEquals(16, sprite.width)
			assertEquals(16, sprite.height)
		}
	}

	@Test
	fun testGetSaveCrystal() {
		// TODO
	}

	@Test
	fun testGetBigDoor() {

	}
}
