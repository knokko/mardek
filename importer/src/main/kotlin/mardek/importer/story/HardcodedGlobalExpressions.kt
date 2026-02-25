package mardek.importer.story

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import mardek.content.animation.ColorTransform
import mardek.content.expression.ConstantStateExpression
import mardek.content.story.CustomTimelineVariable
import mardek.content.expression.ExpressionOrDefaultStateExpression
import mardek.content.expression.GlobalExpression
import mardek.content.expression.GlobalStateExpression
import mardek.content.story.StoryContent
import mardek.content.expression.SwitchCaseStateExpression
import mardek.content.expression.ExpressionColorTransformValue
import mardek.content.expression.ExpressionOptionalColorTransformValue
import mardek.content.expression.ExpressionOptionalStringValue
import mardek.content.expression.ExpressionStringValue
import mardek.content.expression.VariableStateExpression

@Suppress("UNCHECKED_CAST")
internal fun hardcodeGlobalExpressions(content: StoryContent) {
	val timeOfDay = VariableStateExpression(content.customVariables.find {
		it.name == "TimeOfDay"
	}!! as CustomTimelineVariable<String?>)
	val ambienceWithoutDefault = GlobalExpression(
		"TimeOfDayAmbienceWithoutDefault",
		SwitchCaseStateExpression(
			input = ExpressionOrDefaultStateExpression(
				timeOfDay, ConstantStateExpression(ExpressionStringValue("Day"))
			),
			cases = arrayOf(
				SwitchCaseStateExpression.Case(
					inputToMatch = ConstantStateExpression(ExpressionStringValue("Evening")),
					outputWhenInputMatches = ConstantStateExpression(
						ExpressionColorTransformValue(ColorTransform(
							addColor = 0,
							multiplyColor = rgb(1f, 0.8f, 0.7f),
							subtractColor = 0,
						))
					)
				),
				SwitchCaseStateExpression.Case(
					inputToMatch = ConstantStateExpression(ExpressionStringValue("Night")),
					outputWhenInputMatches = ConstantStateExpression(ExpressionColorTransformValue(
						ColorTransform(
							addColor = 0,
							multiplyColor = rgb(0.25f, 0.4f, 0.8f),
							subtractColor = 0,
						)
					))
				),
			),
			defaultOutput = ConstantStateExpression(ExpressionOptionalColorTransformValue(null))
		)
	)
	content.globalExpressions.add(ambienceWithoutDefault)
	content.globalExpressions.add(GlobalExpression(
		"TimeOfDayAmbienceWithDefault",
		ExpressionOrDefaultStateExpression(
			GlobalStateExpression(ambienceWithoutDefault),
			ConstantStateExpression(ExpressionColorTransformValue(ColorTransform.DEFAULT)),
		)
	))
	content.globalExpressions.add(GlobalExpression(
		"TimeOfDayMusic",
		SwitchCaseStateExpression(
			input = ExpressionOrDefaultStateExpression(
				timeOfDay, ConstantStateExpression(ExpressionStringValue("Day"))
			),
			cases = arrayOf(
				SwitchCaseStateExpression.Case(
					inputToMatch = ConstantStateExpression(ExpressionStringValue("Day")),
					outputWhenInputMatches = ConstantStateExpression(
						ExpressionOptionalStringValue(null)
					)
				)
			),
			defaultOutput = ConstantStateExpression(ExpressionStringValue("crickets"))
		)
	))
}
