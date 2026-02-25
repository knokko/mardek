package mardek.importer.story.expressions

import mardek.content.Content
import mardek.content.expression.AndStateCondition
import mardek.content.expression.DefinedVariableStateCondition
import mardek.content.expression.NegateStateCondition

internal fun hardcodeChapter1Expressions(
	content: Content, hardcoded: MutableMap<String, MutableList<HardcodedExpression>>
) {
	val heroQuest = content.story.quests.find { it.tabName == "Hero Quest!" }!!
	val fallenStarQuest = content.story.quests.find { it.tabName == "The Fallen Star" }!!
	val timeOfDayVariable = content.story.customVariables.find { it.name == "TimeOfDay" }!!
	hardcoded[""]!!.add(
		HardcodedExpression(name = "RightAfterDragonLair", expression = AndStateCondition(arrayOf(
			DefinedVariableStateCondition(heroQuest.wasCompleted),
			NegateStateCondition(DefinedVariableStateCondition(fallenStarQuest.isActive)),
			NegateStateCondition(DefinedVariableStateCondition(fallenStarQuest.wasCompleted)),
		)))
	)
	hardcoded[""]!!.add(
		HardcodedExpression(name = "Chapter1Night", expression = AndStateCondition(arrayOf(
			DefinedVariableStateCondition(timeOfDayVariable),
			// TODO CHAP2 Also check the chapter
		)))
	)
	hardcoded[""]!!.add(HardcodedExpression(
		name = "Chapter1Day", expression = DefinedVariableStateCondition(fallenStarQuest.isActive))
	)
}
