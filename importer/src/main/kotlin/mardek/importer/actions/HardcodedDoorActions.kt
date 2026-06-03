package mardek.importer.actions

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import mardek.content.action.ActionSequence
import mardek.content.action.ActionSetOverlayColor
import mardek.content.action.ActionTalk
import mardek.content.action.ActionTargetDefaultDialogueObject
import mardek.content.action.ActionTargetPartyMember
import mardek.content.action.ActionTimelineTransition
import mardek.content.action.ExpressionActionNode
import mardek.content.action.FixedActionNode
import mardek.content.expression.ConstantStateExpression
import mardek.content.expression.DefinedVariableStateCondition
import mardek.content.expression.ExpressionActionNodeValue
import mardek.content.expression.IfElseStateExpression
import mardek.content.story.CustomTimelineVariable
import mardek.content.story.StoryContent
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

internal fun hardcodeDoorActions(content: StoryContent, hardcoded: Map<String, MutableList<ActionSequence>>) {
	hardcoded[""]!!.add(ActionSequence("lock_sealed", FixedActionNode(
		id = UUID.fromString("38783a43-4e36-4a63-b072-08edcff511d6"),
		action = ActionTalk(
			speaker = ActionTargetDefaultDialogueObject(),
			expression = "",
			text = "The door has shut behind you and will not open!",
		),
		next = null,
	)))
	hardcoded[""]!!.add(ActionSequence("lock_tight", FixedActionNode(
		id = UUID.fromString("5967c3ce-0707-433e-8203-c8e136832b15"),
		action = ActionTalk(
			speaker = ActionTargetDefaultDialogueObject(),
			expression = "",
			text = "It's locked tight. No amount of shouting at it or insulting its mother will cause it to open.",
		),
		next = null,
	)))
	hardcoded[""]!!.add(ActionSequence("lock_lock", FixedActionNode(
		id = UUID.fromString("f8baf8fd-af83-4d32-ba43-d65849240076"),
		action = ActionTalk(
			speaker = ActionTargetDefaultDialogueObject(),
			expression = "",
			text = "It's locked.",
		),
		next = null,
	)))
	hardcoded[""]!!.add(ActionSequence("lock_rust", FixedActionNode(
		id = UUID.fromString("4e449d14-3f92-4eca-8088-2c64b37291df"),
		action = ActionTalk(
			speaker = ActionTargetDefaultDialogueObject(),
			expression = "",
			text = "It's rusted shut and can't be opened.",
		),
		next = null,
	)))
	hardcoded[""]!!.add(ActionSequence("lock_magic", FixedActionNode(
		id = UUID.fromString("3ce624ab-6db4-4ecd-a716-a3f213870683"),
		action = ActionTalk(
			speaker = ActionTargetDefaultDialogueObject(),
			expression = "",
			text = "This door is sealed with powerful magic. You can't open it, or bash it down.",
		),
		next = null,
	)))
	hardcoded[""]!!.add(ActionSequence("lock_yes", FixedActionNode(
		id = UUID.fromString("30fab52f-9ab0-404f-90ec-8b22c1cee3bf"),
		action = ActionTalk(
			speaker = ActionTargetDefaultDialogueObject(),
			expression = "",
			text = "Wait... this isn't a door. It's actually a wall, painted to look like a door! How misleading!",
		),
		next = null,
	)))
	hardcoded[""]!!.add(ActionSequence("lock_lock", FixedActionNode(
		id = UUID.fromString("991aeac3-e404-4288-80fe-db730871a6d2"),
		action = ActionTalk(
			speaker = ActionTargetDefaultDialogueObject(),
			expression = "",
			text = "It's locked tight, but there's a keyhole. With the right key, you'd be able to get through.",
		),
		next = null,
	)))
	hardcoded["goznor"]!!.add(ActionSequence("lock_night", FixedActionNode(
		id = UUID.fromString("c4ceef26-613c-4398-95e4-a08d3c2ddd88"),
		// TODO CHAP2 Change text during Zombie outbreak
		action = ActionTalk(
			speaker = ActionTargetDefaultDialogueObject(),
			expression = "",
			text = "It's locked.",
		),
		next = null,
	)))
	hardcoded["goznor"]!!.add(ActionSequence("lock_weapon_shop", FixedActionNode(
		id = UUID.fromString("ade3b3c6-68f4-49c3-84bc-85766bdd5b2c"),
		// TODO CHAP2 Change text during Zombie outbreak
		action = ActionTalk(
			speaker = ActionTargetDefaultDialogueObject(),
			expression = "",
			text = "It's locked.",
		),
		next = null,
	)))

	val dropDeuganBeforeRohoph = fixedActionChain(arrayOf(
		ActionTalk(
			speaker = ActionTargetPartyMember(1),
			expression = "norm",
			text = "Well, I'd better get home myself... So bye for now, Mardek! " +
					"See you tomorrow for some more heroic adventures!",
		),
		ActionSetOverlayColor(color = rgb(0, 0, 0), transitionTime = 500.milliseconds),
		ActionTimelineTransition(
			"MainTimeline", "Dropped Deugan home before the falling 'star'"
		),
		ActionSetOverlayColor(color = 0, transitionTime = 500.milliseconds),
	), ids = arrayOf(
		UUID.fromString("fc5bc415-db85-4b36-b516-2ce0df156d95"),
		UUID.fromString("eb7f3ff2-d772-476e-ab88-32fcca234c1c"),
		UUID.fromString("969d92cf-0dd1-4904-b00c-ac1417a1a907"),
		UUID.fromString("3f5cf8d5-9dde-4d8d-a424-615c317b7cb8"),
	))!!

	val dropDeuganAfterRohoph = fixedActionChain(arrayOf(
		ActionTalk(
			speaker = ActionTargetPartyMember(1),
			expression = "norm",
			text = "Phew, what a weird thing that was that happened... I need to go home now, so, uh... Bye Mardek. " +
					"I hope that nothing bad happens to you because of that thing!"
		),
		ActionSetOverlayColor(color = rgb(0, 0, 0), transitionTime = 500.milliseconds),
		ActionTimelineTransition(
			"MainTimeline", "Dropped Deugan home before after Rohoph entered Mardeks body"
		),
		ActionSetOverlayColor(color = 0, transitionTime = 500.milliseconds),
	), ids = arrayOf(
		UUID.fromString("4610eae5-ebcb-460b-b89a-d9ea1699df51"),
		UUID.fromString("19a988f6-24ab-4dc3-ad51-33d5fcd97a13"),
		UUID.fromString("aa4aa7d8-a822-4ad5-b64e-2be4573211ff"),
		UUID.fromString("96d9d10d-5edb-4728-84f1-992586d0c9f8"),
	))!!

	@Suppress("UNCHECKED_CAST")
	val beforeVariable = content.customVariables.find {
		it.name == "WithDeuganBeforeFallingStar"
	}!! as CustomTimelineVariable<Unit>

	// TODO CHAP2 Change text during Zombie outbreak
	val droppedDeuganNode = ExpressionActionNode(
		id = UUID.fromString("72eebef9-a953-4557-ae6f-95d6545eeb43"),
		expression = IfElseStateExpression(
			condition = DefinedVariableStateCondition(beforeVariable),
			ifTrue = ConstantStateExpression(ExpressionActionNodeValue(dropDeuganBeforeRohoph)),
			ifFalse = ConstantStateExpression(ExpressionActionNodeValue(dropDeuganAfterRohoph)),
		)
	)
	hardcoded["goznor"]!!.add(ActionSequence("lock_mardek_house", droppedDeuganNode))
}
