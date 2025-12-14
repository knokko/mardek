package mardek.game.story

import mardek.content.story.Quest
import mardek.game.TestingInstance
import mardek.state.ingame.CampaignState
import org.junit.jupiter.api.Assertions.assertArrayEquals

object TestQuests {

	fun testHeroQuest(instance: TestingInstance) {
		instance.apply {
			val state = CampaignState.loadChapter(content, 1)
			val quests = state.story.getQuests(content.story)
			assertArrayEquals(emptyArray<Quest>(), quests.completed)
			assertArrayEquals(
				arrayOf(content.story.quests.find { it.tabName == "Hero Quest!" }!!),
				quests.active
			)
		}
	}

	// TODO CHAP1 Create more interesting test during childhood, which should explore the Quests UI
}
