package mardek.game.save

import com.github.knokko.bitser.Bitser
import mardek.game.TestingInstance
import mardek.state.saves.SaveFile
import mardek.state.saves.SaveInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File
import java.nio.file.Files
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

object TestListSaves {

	fun test(instance: TestingInstance) {
		instance.run {
			val savesManager = dummySaveManager()

			assertEquals(0, savesManager.getSaves("").size)
			assertEquals(0, savesManager.getSaves("nothing").size)

			val campaignDirectory = File("${savesManager.root}/knokko")
			assertTrue(campaignDirectory.mkdir())
			campaignDirectory.deleteOnExit()

			fun createSaveFile(name: String, content: ByteArray?) {
				val file = File("$campaignDirectory/$name")
				if (content == null) file.createNewFile()
				else Files.write(file.toPath(), byteArrayOf(content.size.toByte()) + content)
				file.deleteOnExit()
			}

			val bitser = Bitser(false)
			val info1 = SaveInfo(
				areaName = "",
				party = arrayOf(null, heroDeugan.id, null, heroMardek.id),
				playTime = 123.seconds,
				partyLevel = 34,
				chapter = 1,
			)
			val validContent1 = bitser.toBytes(info1, content, Bitser.BACKWARD_COMPATIBLE)
			val info2 = SaveInfo(
				areaName = content.areas.areas.find { it.properties.rawName == "soothwood" }!!.properties.displayName,
				party = arrayOf(heroMardek.id, null, null, null),
				playTime = 123.hours,
				partyLevel = 88,
				chapter = 2,
			)
			val validContent2 = bitser.toBytes(info2, content, Bitser.BACKWARD_COMPATIBLE)
			assertEquals(0, savesManager.getSaves("knokko").size)

			createSaveFile("nope.txt", validContent2)
			createSaveFile("invalid name1.bits", validContent1)
			createSaveFile("invalid-name2.bits", validContent2)
			createSaveFile("crystal-invalid3.bits", validContent1)
			createSaveFile("auto-12345.bits", null)
			createSaveFile("-123456.bits", validContent1)
			createSaveFile("auto--123456.bits", validContent1)
			createSaveFile("crystal-8765.bits", validContent2)

			assertEquals(0, savesManager.getSaves("nope").size)

			val saves = savesManager.getSaves("knokko").sortedBy { it.timestamp }
			assertEquals(2, saves.size)

			assertEquals(-123456L, saves[0].timestamp)
			assertEquals(SaveFile.Type.Auto, saves[0].type)
			assertTrue(bitser.deepEquals(info1, saves[0].info))

			assertEquals(8765L, saves[1].timestamp)
			assertEquals(SaveFile.Type.Crystal, saves[1].type)
			assertTrue(bitser.deepEquals(info2, saves[1].info))

			campaignDirectory.deleteRecursively()
		}
	}
}
