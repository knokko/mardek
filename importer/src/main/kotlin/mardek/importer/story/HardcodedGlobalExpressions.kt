package mardek.importer.story

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import mardek.content.animation.ColorTransform
import mardek.content.story.ConstantTimelineExpression
import mardek.content.story.CustomTimelineVariable
import mardek.content.story.ExpressionOrDefaultTimelineExpression
import mardek.content.story.GlobalExpression
import mardek.content.story.GlobalTimelineExpression
import mardek.content.story.StoryContent
import mardek.content.story.SwitchCaseTimelineExpression
import mardek.content.story.TimelineColorTransformValue
import mardek.content.story.TimelineOptionalColorTransformValue
import mardek.content.story.TimelineOptionalStringValue
import mardek.content.story.TimelineStringValue
import mardek.content.story.VariableTimelineExpression

@Suppress("UNCHECKED_CAST")
internal fun hardcodeGlobalExpressions(content: StoryContent) {
	val timeOfDay = VariableTimelineExpression(content.customVariables.find {
		it.name == "TimeOfDay"
	}!! as CustomTimelineVariable<String?>)
	val ambienceWithoutDefault = GlobalExpression(
		"TimeOfDayAmbienceWithoutDefault",
		SwitchCaseTimelineExpression(
			input = ExpressionOrDefaultTimelineExpression(
				timeOfDay, ConstantTimelineExpression(TimelineStringValue("Day"))
			),
			cases = arrayOf(
				SwitchCaseTimelineExpression.Case(
					inputToMatch = ConstantTimelineExpression(TimelineStringValue("Evening")),
					outputWhenInputMatches = ConstantTimelineExpression(
						TimelineColorTransformValue(ColorTransform(
							addColor = 0,
							multiplyColor = rgb(1f, 0.8f, 0.7f),
							subtractColor = 0,
						))
					)
				),
				SwitchCaseTimelineExpression.Case(
					inputToMatch = ConstantTimelineExpression(TimelineStringValue("Night")),
					outputWhenInputMatches = ConstantTimelineExpression(TimelineColorTransformValue(
						ColorTransform(
							addColor = 0,
							multiplyColor = rgb(0.25f, 0.4f, 0.8f),
							subtractColor = 0,
						)
					))
				),
			),
			defaultOutput = ConstantTimelineExpression(TimelineOptionalColorTransformValue(null))
		)
	)
	content.globalExpressions.add(ambienceWithoutDefault)
	content.globalExpressions.add(GlobalExpression(
		"TimeOfDayAmbienceWithDefault",
		ExpressionOrDefaultTimelineExpression(
			GlobalTimelineExpression(ambienceWithoutDefault),
			ConstantTimelineExpression(TimelineColorTransformValue(ColorTransform.DEFAULT)),
		)
	))
	content.globalExpressions.add(GlobalExpression(
		"TimeOfDayMusic",
		SwitchCaseTimelineExpression(
			input = ExpressionOrDefaultTimelineExpression(
				timeOfDay, ConstantTimelineExpression(TimelineStringValue("Day"))
			),
			cases = arrayOf(
				SwitchCaseTimelineExpression.Case(
					inputToMatch = ConstantTimelineExpression(TimelineStringValue("Day")),
					outputWhenInputMatches = ConstantTimelineExpression(
						TimelineOptionalStringValue(null)
					)
				)
			),
			defaultOutput = ConstantTimelineExpression(TimelineStringValue("crickets"))
		)
	))
}
