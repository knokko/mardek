package mardek.importer.story

import mardek.content.story.StoryContent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestQuestImporter {

	@Test
	fun testLeadPipeQuest() {
		val content = StoryContent()
		importQuests(content)

		val quest = content.quests.find { it.tabName == "LeadPipes" }!!
		assertEquals("Collect 5 LeadPipes", quest.title)
		assertEquals(
			"Meraeador, the inventor, has asked you to bring him five LeadPipes, " +
					"which he needs for his latest invention. " +
					"You can get LeadPipes from fumerats in the Goznor sewers.",
			quest.description
		)
	}
}