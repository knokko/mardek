package mardek.importer.actions

import mardek.content.Content
import mardek.content.action.ActionSequence
import mardek.content.action.ActionTalk
import mardek.content.action.ActionTargetDefaultDialogueObject
import mardek.content.action.ActionTargetPartyMember
import mardek.content.action.FixedActionNode
import mardek.content.action.TimelineActionNode
import mardek.content.story.ConstantTimelineExpression
import mardek.content.story.DefinedVariableTimelineCondition
import mardek.content.story.IfElseTimelineExpression
import mardek.content.story.TimelineActionNodeValue
import java.util.UUID

internal fun hardcodeGoznorDialogues(
	content: Content, hardcoded: MutableMap<String, MutableList<ActionSequence>>
) {
	val timeOfDay = content.story.customVariables.find { it.name == "TimeOfDay" }!!
	// TODO CHAP2 CHAP3 Change dialogue based on timeline state
	val bernardoRoot = TimelineActionNode(
		id = UUID.fromString("83bd105b-5ef1-43c8-9951-93306c1ce719"),
		expression = IfElseTimelineExpression(
			condition = DefinedVariableTimelineCondition(timeOfDay),
			ifTrue = ConstantTimelineExpression(TimelineActionNodeValue(
				FixedActionNode(
					id = UUID.fromString("6aa38b43-eb40-4d5e-b5ae-0a94593fe4f8"),
					action = ActionTalk(
						speaker = ActionTargetDefaultDialogueObject(),
						expression = "norm",
						text = "Oy, it's like a lickle boy or summat. Shouldn' you be goin' 'ome, sonny?",
					), next = null
				)
			)),
			ifFalse = ConstantTimelineExpression(TimelineActionNodeValue(
				FixedActionNode(
					id = UUID.fromString("8daefee4-c263-401d-9cbe-c2022cf6ae30"),
					action = ActionTalk(
						speaker = ActionTargetDefaultDialogueObject(),
						expression = "norm",
						text = "Oy, it\'s like a lickle boy or summat. Buggroff.",
					), next = null
				)
			)),
		)
	)
	val marcellusRoot = TimelineActionNode(
		id = UUID.fromString("965227b5-0588-4ded-860e-66921fa9070d"),
		expression = IfElseTimelineExpression(
			condition = DefinedVariableTimelineCondition(timeOfDay),
			ifTrue = ConstantTimelineExpression(TimelineActionNodeValue(fixedActionChain(
				actions = arrayOf(
					ActionTalk(
						speaker = ActionTargetDefaultDialogueObject(),
						expression = "blah",
						text = "Everyone else is in bed (yes, before the sun's even set, I know!), " +
								"and here WE are on the 'night shift', protecting these barracks from monsters, supposedly.",
					),
					ActionTalk(
						speaker = ActionTargetDefaultDialogueObject(),
						expression = "blah",
						text = "Pfft. This place has been here for years though, " +
								"and in all my time here it's only been 'attacked' once, " +
								"and that was by a drunken gruul that passed out after knocking on the door once anyway!"
					),
				),
				ids = arrayOf(
					UUID.fromString("0596b9c7-42f0-4c95-b4a6-477b343c121d"),
					UUID.fromString("6db2547d-9d99-4380-abec-22e5ee8fa385"),
				),
			)!!)),
			ifFalse = ConstantTimelineExpression(TimelineActionNodeValue(FixedActionNode(
				id = UUID.fromString("c039737b-bd9c-417e-b6f3-eb2fd86d1617"),
				action = ActionTalk(
					speaker = ActionTargetDefaultDialogueObject(),
					expression = "norm",
					text = "Oh, hello there, sonny. The barracks are no place for children, but go in for all I care.",
				), next = null
			)))
		)
	)
	val fallenStarQuest = content.story.quests.find { it.tabName == "The Fallen Star" }!!
	val georgeRoot = TimelineActionNode(
		id = UUID.fromString("d1642cb8-a4d0-41a2-9faf-a6d856620d90"),
		expression = IfElseTimelineExpression(
			condition = DefinedVariableTimelineCondition(fallenStarQuest.wasCompleted),
			ifTrue = ConstantTimelineExpression(TimelineActionNodeValue(
				fixedActionChain(
					actions = arrayOf(
						ActionTalk(
							speaker = ActionTargetDefaultDialogueObject(),
							expression = "norm",
							text = "Hello there, boys! Back from another adventure again? " +
									"I\'m just, uh... just wandering the streets. " +
									"Yes, there\'s nothing suspicious about THAT, is there?"
						),
					),
					ids = arrayOf(
						UUID.fromString("a627d664-a318-4573-9105-1b3784a155ef"),
					),
				)!!
			)),
			ifFalse = ConstantTimelineExpression(TimelineActionNodeValue(
				fixedActionChain(
					actions = arrayOf(
						ActionTalk(
							speaker = ActionTargetDefaultDialogueObject(),
							expression = "norm",
							text = "You're out late there, boys! Shouldn't you be going back home to bed? " +
									"Your parents are probably worried!"
						),
						ActionTalk(
							speaker = ActionTargetDefaultDialogueObject(),
							expression = "susp",
							text = "You... do remember where you live, right? " +
									"In those little houses in the south-west part of town...?"
						),
						ActionTalk(
							speaker = ActionTargetDefaultDialogueObject(),
							expression = "o_O",
							text = "No, I, uh... I don't know why I thought you might've forgotten! " +
									"But it's useful to know anyway, innit?"
						),
					),
					ids = arrayOf(
						UUID.fromString("c063a2cc-fc3e-47be-940b-af9537862491"),
						UUID.fromString("f33ee5a6-2cf0-4b52-a81d-5dc2e37566cd"),
						UUID.fromString("c24c0d44-3e2f-435a-8ea8-fd7fc9f7f4a5"),
					),
				)!!
			))
		)
	)
	hardcoded["goznor"]!!.addAll(listOf(
		ActionSequence(name = "guard_bernardo", root = bernardoRoot),
		ActionSequence(name = "guard_marcellus", root = marcellusRoot),
		ActionSequence(name = "blocking_george", root = georgeRoot),
	))

	val jacquesRoot = fixedActionChain(
		actions = arrayOf(
			ActionTalk(
				speaker = ActionTargetDefaultDialogueObject(),
				expression = "smile",
				text = "Ah, Mardek and Deugan! 'Ow nice it iz to see you zis day! And you come at good timing, too, " +
						"as I 'ave news for you concerning your faazer!"
			),
			ActionTalk(
				speaker = ActionTargetPartyMember(0),
				expression = "shok",
				text = "You know something about my dad?!?",
			),
			ActionTalk(
				speaker = ActionTargetDefaultDialogueObject(),
				expression = "deep",
				text = "Aah, I do not know much, but I 'eard from one of zee soldiers " +
						"zat he was sighted in Nole'Oth... Zee frozen Northern Wastes.",
			),
			ActionTalk(
				speaker = ActionTargetDefaultDialogueObject(),
				expression = "grin",
				text = "Zis iz good! It means 'e still lives! 'E seems to be on some kind of epic zhournee, a quest, " +
						"and knowing 'im, 'e should 'ave it finished soon and will surely be back in no time!",
			),
			ActionTalk(
				speaker = ActionTargetPartyMember(0),
				expression = "grin",
				text = "Well, I hope so!",
			),
			ActionTalk(
				speaker = ActionTargetDefaultDialogueObject(),
				expression = "grin",
				text = "We all can but 'ope... Enki was a great man, though... 'E will be back soon, " +
						"I am sure of zis! And when 'e returns, " +
						"I am sure 'e will 'ave many gifts to give and tales to tell!"
			),
		),
		ids = arrayOf(
			UUID.fromString("c1878482-2868-4c39-bf52-99e6c90564e0"),
			UUID.fromString("4c1bb4ba-f6fa-4e6f-a778-7a5a426e2a11"),
			UUID.fromString("acdbb3d5-1346-4cb4-baa5-00435bd078fc"),
			UUID.fromString("4b212299-a45a-4cb4-9108-333b2473e7bd"),
			UUID.fromString("a539cb3a-6458-45b2-bf70-4310fa17adc2"),
			UUID.fromString("76777d4a-3793-4c98-b304-93a96511e3d3"),
		)
	)!!
	hardcoded["gz_barracks"] = mutableListOf(ActionSequence(name = "jacques", root = jacquesRoot))
}
