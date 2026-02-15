package mardek.importer.story.expressions

import mardek.content.Content
import mardek.content.story.AndTimelineCondition
import mardek.content.story.DefinedVariableTimelineCondition
import mardek.content.story.NegateTimelineCondition

internal fun hardcodeChapter1Expressions(
	content: Content, hardcoded: MutableMap<String, MutableList<HardcodedExpression>>
) {
	val heroQuest = content.story.quests.find { it.tabName == "Hero Quest!" }!!
	val fallenStarQuest = content.story.quests.find { it.tabName == "The Fallen Star" }!!
	hardcoded[""]!!.add(
		HardcodedExpression(name = "RightAfterDragonLair", expression = AndTimelineCondition(arrayOf(
			DefinedVariableTimelineCondition(heroQuest.wasCompleted),
			NegateTimelineCondition(DefinedVariableTimelineCondition(fallenStarQuest.isActive)),
			NegateTimelineCondition(DefinedVariableTimelineCondition(fallenStarQuest.wasCompleted)),
		)))
	)
}
