package mardek.importer.story.expressions

import mardek.content.Content
import mardek.content.story.CustomTimelineVariable
import mardek.content.expression.DefinedVariableStateCondition
import mardek.content.expression.NegateStateCondition

@Suppress("UNCHECKED_CAST")
internal fun hardcodeGoznorExpressions(
	content: Content, hardcoded: MutableMap<String, MutableList<HardcodedExpression>>
) {
	val withDeugan = content.story.customVariables.find {
		it.name == "WithDeuganBeforeFallingStar"
	}!! as CustomTimelineVariable<Unit>
	val timeOfDay = content.story.customVariables.find {
		it.name == "TimeOfDay"
	}!! as CustomTimelineVariable<String>

	hardcoded["goznor"] = mutableListOf(
		HardcodedExpression(
			name = "lock_mardek_house",
			expression = NegateStateCondition(DefinedVariableStateCondition(withDeugan)),
		),
		HardcodedExpression(
			name = "lock_night",
			expression = NegateStateCondition(DefinedVariableStateCondition(timeOfDay)),
		),
		HardcodedExpression(
			name = "lock_weapon_shop",
			// TODO CHAP3 Close during chapter 3
			expression = NegateStateCondition(DefinedVariableStateCondition(timeOfDay)),
		),
	)
}
