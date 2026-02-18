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
	val timeOfDayVariable = content.story.customVariables.find { it.name == "TimeOfDay" }!!
	hardcoded[""]!!.add(
		HardcodedExpression(name = "RightAfterDragonLair", expression = AndTimelineCondition(arrayOf(
			DefinedVariableTimelineCondition(heroQuest.wasCompleted),
			NegateTimelineCondition(DefinedVariableTimelineCondition(fallenStarQuest.isActive)),
			NegateTimelineCondition(DefinedVariableTimelineCondition(fallenStarQuest.wasCompleted)),
		)))
	)
	hardcoded[""]!!.add(
		HardcodedExpression(name = "Chapter1Night", expression = AndTimelineCondition(arrayOf(
			DefinedVariableTimelineCondition(timeOfDayVariable),
			// TODO CHAP2 Also check the chapter
		)))
	)
	hardcoded[""]!!.add(HardcodedExpression(
		name = "Chapter1Day", expression = DefinedVariableTimelineCondition(fallenStarQuest.isActive))
	)
}
