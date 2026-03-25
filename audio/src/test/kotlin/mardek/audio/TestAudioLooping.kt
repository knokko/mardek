package mardek.audio

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestAudioLooping {

	@Test
	fun testParse() {
		val map = AudioLooping.parse()
		assertEquals(0.35f, map["battle"]!!, 0.001f)
		assertEquals(3.562f, map["SecretBoss"]!!, 0.001f)
	}
}
