package mardek.game.portrait

import mardek.game.TestingInstance
import org.junit.jupiter.api.Assertions.assertSame

object TestPortraitImporting {

	fun testAreaCharacterPortraits(instance: TestingInstance) {
		instance.apply {
			val airTemple = content.areas.areas.find { it.properties.rawName == "aeropolis_N_TAIR" }!!
			val priestess = airTemple.objects.characters.find { it.name == "Priestess Gail" }!!
			assertSame(content.stats.elements.find { it.rawName == "AIR" }!!, priestess.element)
			assertSame(content.portraits.info.find { it.flashName == "priestess" }!!, priestess.portrait)

			val astral = content.areas.areas.find { it.properties.rawName == "astral4" }!!
			val qualna = astral.objects.characters.find { it.name == "Qualna" }!!
			assertSame(content.stats.elements.find { it.rawName == "ETHER" }!!, qualna.element)
			assertSame(content.portraits.info.find { it.flashName == "qualna" }!!, qualna.portrait)
		}
	}
}
