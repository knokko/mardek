package mardek.importer.story

import mardek.content.story.Quest
import mardek.content.story.StoryContent
import mardek.importer.area.parseFlashString
import mardek.importer.util.parseActionScriptObjectList
import mardek.importer.util.parseActionScriptResource

internal fun importQuests(content: StoryContent) {
	val questsCode = parseActionScriptResource("mardek/importer/story/quests.txt")
	val rawQuestList = parseActionScriptObjectList(questsCode.variableAssignments["QuestData"]!!)

	for (rawQuest in rawQuestList) {
		content.quests.add(Quest(
			tabName = parseFlashString(rawQuest["title1"]!!, "quest tab name")!!,
			title = parseFlashString(rawQuest["title"]!!, "quest title")!!,
			description = parseFlashString(rawQuest["body"]!!, "quest description")!!,
		))
	}
}
