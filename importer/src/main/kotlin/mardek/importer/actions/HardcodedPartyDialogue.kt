package mardek.importer.actions

import mardek.content.Content
import mardek.content.action.ActionTalk
import mardek.content.action.ActionTargetPartyMember
import mardek.content.action.ActionTargetPlayer
import mardek.content.action.ChoiceActionNode
import mardek.content.action.ChoiceEntry
import mardek.content.action.ExpressionActionNode
import mardek.content.action.FixedActionNode
import mardek.content.expression.ConstantStateExpression
import mardek.content.expression.DefinedVariableStateCondition
import mardek.content.expression.ExpressionActionNodeValue
import mardek.content.expression.IfElseStateExpression
import mardek.content.expression.PartyMemberStateCondition
import java.util.UUID

internal fun hardcodePartyDialogue(content: Content) {
	val choiceNode = ChoiceActionNode(
		id = UUID.fromString("a7350b46-19d3-4d94-a129-8136935c461f"),
		speaker = ActionTargetPartyMember(0),
		options = arrayOf(
			hardcodedHeroDeuganPartyDialogue(content),
			hardcodedChildDeuganPartyDialogue(content),
			ChoiceEntry(
				expression = "norm",
				text = "Never mind...",
				next = null,
			)
		)
	)
	content.actions.partyDialogueNode = FixedActionNode(
		id = UUID.fromString("bc5ddb20-516d-49bb-bc59-e02d12dcb3ff"),
		action = ActionTalk(
			speaker = ActionTargetPartyMember(0),
			expression = "norm",
			text = "Well, who should I talk to?"
		),
		next = choiceNode,
	)
}

private fun hardcodedHeroDeuganPartyDialogue(content: Content): ChoiceEntry {
	val deugan = content.playableCharacters.find { it.areaSprites.name == "deugan_hero" }!!
	return ChoiceEntry(
		expression = "norm",
		text = "Talk to Deugan",
		next = FixedActionNode(
			id = UUID.fromString("d79e4a87-597d-49d7-ae1e-e03a15349c79"),
			action = ActionTalk(
				speaker = ActionTargetPlayer(deugan),
				expression = "smile",
				text = "We're adventuring, Mardek!"
			),
			next = null
		),
		condition = PartyMemberStateCondition(deugan),
	)
}

private fun hardcodedChildDeuganPartyDialogue(content: Content): ChoiceEntry {
	val deugan = content.playableCharacters.find { it.areaSprites.name == "deugan_child" }!!
	val finishedFallenStar = content.story.quests.find { it.tabName == "The Fallen Star" }!!.wasCompleted
	val timeOfDay = content.story.customVariables.find { it.name == "TimeOfDay" }!!
	return ChoiceEntry(
		expression = "norm",
		text = "Talk to Deugan",
		next = ExpressionActionNode(
			id = UUID.fromString("992d9a40-35e7-4f84-bef0-55c6e67d70de"),
			expression = IfElseStateExpression(
				condition = DefinedVariableStateCondition(finishedFallenStar),
				ifTrue = ConstantStateExpression(ExpressionActionNodeValue(FixedActionNode(
					id = UUID.fromString("923061b3-b086-45d4-bb63-8d9b9aced268"),
					action = ActionTalk(
						speaker = ActionTargetPlayer(deugan),
						expression = "sad",
						text = "Are you alright, Mardek? I hope you are..."
					),
					next = null,
				))),
				ifFalse = IfElseStateExpression(
					condition = DefinedVariableStateCondition(timeOfDay),
					ifTrue = ConstantStateExpression(ExpressionActionNodeValue(FixedActionNode(
						id = UUID.fromString("5f1bb18d-134d-42a3-882c-37751596c8d3"),
						action = ActionTalk(
							speaker = ActionTargetPlayer(deugan),
							expression = "smile",
							text = "Let's go home, Mardek."
						),
						next = null,
					))),
					ifFalse = ConstantStateExpression(ExpressionActionNodeValue(FixedActionNode(
						id = UUID.fromString("805b13d8-018a-411c-9d0c-f589d932d773"),
						action = ActionTalk(
							speaker = ActionTargetPlayer(deugan),
							expression = "smile",
							text = "We're adventuring, Mardek!",
						),
						next = null,
					)))
				),
			)
		),
		condition = PartyMemberStateCondition(deugan),
	)
}
