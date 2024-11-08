package mardek.importer.area

import com.github.knokko.boiler.utilities.ColorPacker.*
import com.github.knokko.compressor.Kim1Decompressor
import mardek.assets.area.objects.AreaObject
import mardek.assets.area.sprites.KimImage
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.awt.Color
import java.io.File
import java.nio.ByteBuffer
import java.nio.file.Files
import javax.imageio.ImageIO

class TestAreaSprites {

	private fun assertImageEquals(
		expectedPath: String, expectedX: Int, expectedY: Int, expectedWidth: Int, expectedHeight: Int, actual: KimImage
	) {
		assertEquals(expectedWidth, actual.width)
		assertEquals(expectedHeight, actual.height)

		val expectedInput = TestAreaSprites::class.java.getResourceAsStream(expectedPath)!!
		val fullImage = ImageIO.read(expectedInput)
		expectedInput.close()

		val actualBuffer = ByteBuffer.allocate(4 * actual.data.size)
		actualBuffer.asIntBuffer().put(actual.data)

		val sampler = Kim1Decompressor(actualBuffer)
		assertEquals(expectedWidth, sampler.width)
		assertEquals(expectedHeight, sampler.height)

		val expectedImage = fullImage.getSubimage(expectedX, expectedY, expectedWidth, expectedHeight)
		for (y in 0 until expectedHeight) {
			for (x in 0 until expectedWidth) {
				val expectedColor = Color(expectedImage.getRGB(x, y), true)
				val actualColor = sampler.getColor(x, y)
				assertEquals(expectedColor.red, unsigned(red(actualColor)))
				assertEquals(expectedColor.green, unsigned(green(actualColor)))
				assertEquals(expectedColor.blue, unsigned(blue(actualColor)))
				assertEquals(expectedColor.alpha, unsigned(alpha(actualColor)))
			}
		}
	}

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
		val areaSprites = AreaSprites()
		val crystal = areaSprites.getObject("obj_Crystal")
		assertEquals(4, crystal.frames!!.size)
		assertImageEquals(
			"sheets/objects/Crystal.png", 0, 0,
			16, 16, crystal.frames!![0]
		)
		assertImageEquals(
			"sheets/objects/Crystal.png", 48, 0,
			16, 16, crystal.frames!![3]
		)
	}

	@Test
	fun testGetBigDoor() {
		val areaSprites = AreaSprites()
		val door = areaSprites.getObject("BIGDOORSHEET", offsetY = 3 * 32, height = 32)
		assertEquals(4, door.frames!!.size) // TODO Assert 3 instead
		assertImageEquals(
			"sheets/objects/BIGDOORSHEET.png", 0, 3 * 32,
			16, 32, door.frames!![0]
		)
		assertImageEquals(
			"sheets/objects/BIGDOORSHEET.png", 32, 3 * 32,
			16, 32, door.frames!![2]
		)
	}

	@Test
	fun testGetZombieDragon() {
		val areaSprites = AreaSprites()
		val dragon = areaSprites.getObject("spritesheet_dragon", frameIndex = 2, numFrames = 2)
		assertEquals(2, dragon.frames!!.size)
		assertImageEquals(
			"sheets/objects/dragon.png", 96, 0,
			48, 48, dragon.frames!![0]
		)
		assertImageEquals(
			"sheets/objects/dragon.png", 96 + 48, 0,
			48, 48, dragon.frames!![1]
		)
	}

	@Test
	fun testGetMolestor() {
		val rawMolester = parseAreaEntity2(parseAreaEntities1("[{name:\"Molestor\",model:\"ch3bosses\",x:16,y:37,walkspeed:-1,dir:\"e\",Static:true,elem:\"DARK\",BOSSCODE:\"Molestor2\",conv:[[\"norm\",\"<<demon>>Neeeeeeeheeheeheehee... Children... You do not belooooong heeeererererere...!\"],Do = function()\n" +
				"{\n" +
				"   BATTLE([[\"Molestor\",null,null,null],[\"Molestor\",null,null,null],[20,null,null,null],\"SOLO\"],\"battle\",true,true);\n" +
				"   return 1;\n" +
				"}]}]")[0])

		assertTrue(rawMolester is AreaObject, "Expected $rawMolester to be an AreaObject")
		val molestor = rawMolester as AreaObject
		assertEquals("spritesheet_ch3bosses", molestor.spritesheetName)
		assertEquals(2, molestor.numFrames)
		assertEquals(4, molestor.firstFrameIndex)

		val areaSprites = AreaSprites()
		val sheet = areaSprites.getObject(
			molestor.spritesheetName, frameIndex = molestor.firstFrameIndex!!, numFrames = molestor.numFrames!!
		)

		assertNotNull(sheet.frames)
		assertEquals(2, sheet.frames!!.size)
		assertImageEquals(
			"sheets/objects/ch3bosses.png", 192, 0,
			48, 48, sheet.frames!![0]
		)
		assertImageEquals(
			"sheets/objects/ch3bosses.png", 192 + 48, 0,
			48, 48, sheet.frames!![1]
		)
	}

	@Test
	fun testGetCyanBrazier() {
		val areaSprites = AreaSprites()
		val hex = HexObject.map[rgb(38, 79, 74)]!!
		assertEquals("CyanBrazier", hex.sheetName)
		val brazier = areaSprites.getObject(
			hex.sheetName, offsetY = hex.sheetRow * hex.height, height = hex.height
		)
		assertNotNull(brazier.frames)
		assertEquals(4, brazier.frames!!.size)
		assertImageEquals(
			"sheets/objects/CyanBrazier.png", 0, 0,
			16, 32, brazier.frames!![0]
		)
		assertImageEquals(
			"sheets/objects/CyanBrazier.png", 48, 0,
			16, 32, brazier.frames!![3]
		)
	}
}
