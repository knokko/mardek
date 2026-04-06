package mardek.importer.encyclopedia

import mardek.content.Content
import mardek.content.expression.ConstantStateExpression
import mardek.content.expression.ExpressionBooleanValue
import mardek.content.expression.ExpressionIntValue
import mardek.content.expression.GreaterEqualStateCondition
import mardek.content.expression.StateExpression
import mardek.content.expression.VariableStateExpression
import mardek.importer.util.parseActionScriptObject
import mardek.importer.util.parseActionScriptObjectList
import mardek.importer.util.parseActionScriptResource
import java.lang.Integer.parseInt

internal fun importSomeEncyclopediaContent(
	content: Content, section: String, importFunction: (
		elementsList: List<Map<String, String>>,
		chapterExpression: StateExpression<Int>,
		simpleShouldShow: (Map<String, String>) -> StateExpression<Boolean>
	) -> Unit
) {
	val chapterVariable = content.story.fixedVariables.chapter

	@Suppress("UNCHECKED_CAST")
	val chapterExpression = VariableStateExpression(chapterVariable) as StateExpression<Int>

	val encyclopediaCode = parseActionScriptResource("mardek/importer/encyclopedia.txt")
	val rawEncyclopedia = parseActionScriptObject(encyclopediaCode.variableAssignments["Encyclopaedia"]!!)
	val rawArtefacts = rawEncyclopedia[section]!!
	val elementsList = parseActionScriptObjectList(rawArtefacts)

	fun simpleShouldShow(entry: Map<String, String>): StateExpression<Boolean> {
		val firstChapter = parseInt(entry["CH"]!!)

		return if (firstChapter == 1) {
			ConstantStateExpression(ExpressionBooleanValue(true))
		} else {
			GreaterEqualStateCondition(
				left = chapterExpression,
				right = ConstantStateExpression(ExpressionIntValue(firstChapter))
			)
		}
	}

	importFunction(elementsList, chapterExpression, ::simpleShouldShow)
}
