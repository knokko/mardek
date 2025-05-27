package mardek.importer.battle

import mardek.content.Content
import mardek.importer.audio.importAudioContent
import mardek.importer.inventory.importItemsContent
import mardek.importer.particle.importParticleEffects
import mardek.importer.stats.importStatsContent
import mardek.importer.skills.importSkillsContent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestBattleBackgrounds {

	@Test
	fun testBattleBackgrounds() {
		val content = Content()
		importAudioContent(content.audio)
		importStatsContent(content)
		importParticleEffects(content)
		importSkillsContent(content)
		importItemsContent(content)
		importBattleContent(content, null)
		assertEquals(43, content.battle.backgrounds.size)
	}
}
