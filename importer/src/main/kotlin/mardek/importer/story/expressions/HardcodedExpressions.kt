package mardek.importer.story.expressions

import mardek.content.Content

internal class HardcodedExpressions {

	private val hardcoded = mutableMapOf<String, MutableList<HardcodedExpression>>()

	internal fun hardcodeTimelineExpressions(content: Content) {
		hardcoded[""] = mutableListOf()
		hardcodeChapter1Expressions(content, hardcoded)
		hardcodeGoznorExpressions(content, hardcoded)
	}

	internal fun getHardcodedAreaExpressions(areaName: String, expressionName: String) = hardcoded[areaName]?.find {
		it.name == expressionName
	}?.expression

	internal fun getHardcodedGlobalExpressions(
		name: String
	) = getHardcodedAreaExpressions("", name)
}
