package mardek.importer.actions

import mardek.content.Content
import mardek.content.action.ActionGiveItem
import mardek.content.action.ActionSequence
import mardek.content.action.ActionTakeItem
import mardek.content.action.ActionTalk
import mardek.content.action.ActionTargetDefaultDialogueObject
import mardek.content.action.ActionTargetPartyMember
import mardek.content.action.ActionTimelineTransition
import mardek.content.action.ChoiceActionNode
import mardek.content.action.ChoiceEntry
import mardek.content.action.ExpressionActionNode
import mardek.content.action.FixedActionNode
import mardek.content.expression.ConstantStateExpression
import mardek.content.expression.DefinedVariableStateCondition
import mardek.content.expression.ExpressionActionNodeValue
import mardek.content.expression.IfElseStateExpression
import mardek.content.expression.ItemCountStateCondition
import java.util.UUID

internal fun hardcodeInventorLabActions(
	content: Content, hardcoded: MutableMap<String, MutableList<ActionSequence>>
) {
	val chapter1Node = run {
		val rejectionNode = FixedActionNode(
			id = UUID.fromString("b6dc8e6b-f95d-42e5-8a8d-598e60201d6e"),
			action = ActionTalk(
				speaker = ActionTargetDefaultDialogueObject(),
				expression = "deep",
				text = "O-o-oh. M-m-maybe some other time, then.",
			),
			next = null
		)
		val acceptanceNode = fixedActionChain(
			actions = arrayOf(
				ActionTalk(
					speaker = ActionTargetDefaultDialogueObject(),
					expression = "grin",
					text = "M-m-marvellous! You j-j-just have to go into the sewers and kill a few fumerats. " +
							"They should drop LeadPipes. C-c-come back here when you've got five of them!",
				),
				ActionTimelineTransition("LeadPipeQuestTimeline", "Accepted Pipe Quest"),
			),
			ids = arrayOf(
				UUID.fromString("964f8db4-d965-4263-b100-15d66bf93641"),
				UUID.fromString("08c8e8b3-fec5-43a8-9619-204408a789a2"),
			),
		)!!
		val explanationNode = FixedActionNode(
			id = UUID.fromString("363d1f33-30d1-41f0-88b8-a3e39339c8f4"),
			action = ActionTalk(
				speaker = ActionTargetDefaultDialogueObject(),
				expression = "grin",
				text = "Ah, i-i-interested in my inventions as always, I see! This time I'm w-w-working on " +
						"something that I think will r-r-revolutionise the way we all live! I-I-I'm making a " +
						"*mechanical man*... I-i-it's like a man, but made out of metal and cogs. " +
						"It can do a-a-anything that a normal man can - like physical labour and (ugh) combat - " +
						"but n-n-never tires or disobeys."
			),
			next = FixedActionNode(
				id = UUID.fromString("9557fe56-48fb-4461-9a16-ef8c2376817b"),
				action = ActionTalk(
					speaker = ActionTargetDefaultDialogueObject(),
					expression = "smile",
					text = "I can s-s-see them being used to make all of our " +
							"lives easier b-b-by doing all the labour for us!"
				),
				next = FixedActionNode(
					id = UUID.fromString("6f515172-7b66-4c0c-b5b3-3c327c0408f3"),
					action = ActionTalk(
						speaker = ActionTargetPartyMember(0),
						expression = "grin",
						text = "Wow! Well, it sounds amazing! I can't wait until it's made!"
					),
					next = FixedActionNode(
						id = UUID.fromString("2012b9c1-3c71-4d29-9428-1bb4c48bfb4b"),
						action = ActionTalk(
							speaker = ActionTargetDefaultDialogueObject(),
							expression = "grin",
							text = "Nor can I, b-b-but of course! B-b-but I need your help! " +
									"S-s-so will you retrieve these LeadPipes for me?"
						),
						next = ChoiceActionNode(
							id = UUID.fromString("96938ed9-adbd-4c03-8e15-72f2142fa0b0"),
							speaker = ActionTargetPartyMember(0),
							options = arrayOf(
								ChoiceEntry(
									expression = "grin",
									text = "Well, of course!",
									next = acceptanceNode,
								),
								ChoiceEntry(
									expression = "norm",
									text = "Sorry, but we're busy.",
									next = rejectionNode,
								),
							),
						),
					),
				),
			),
		)
		val interestedNode = FixedActionNode(
			id = UUID.fromString("8f7c580c-b6f1-4e3f-b44f-f24998810929"),
			action = ActionTalk(
				speaker = ActionTargetDefaultDialogueObject(),
				expression = "grin",
				text = "B-b-brilliant! I knew I could count on you two!",
			),
			next = FixedActionNode(
				id = UUID.fromString("16e83f33-3b89-42cc-9ecf-8e42de797aff"),
				action = ActionTalk(
					speaker = ActionTargetDefaultDialogueObject(),
					expression = "norm",
					text = "I-I-I'm trying to make a new invention, you see, but I-I-I don't have all the parts I need. " +
							"I need some more LeadPipes - a-a-about five should do - but the best place to g-g-get " +
							"those is in the sewers, from the fumerats."
				),
				next = FixedActionNode(
					id = UUID.fromString("4d698362-f68c-46dd-9c62-1f85caa14486"),
					action = ActionTalk(
						speaker = ActionTargetDefaultDialogueObject(),
						expression = "norm",
						text = "Y-y-you know I'm incompetent when it comes to combat of a-a-any kind, " +
								"and *I* know that you relish it and have f-f-fought fumerats before! " +
								"So I thought this might be a g-g-good chance for " +
								"you to fight things for an actual reason!"
					),
					next = FixedActionNode(
						id = UUID.fromString("1ebd0494-c041-4482-ba70-596db0aad4e4"),
						action = ActionTalk(
							speaker = ActionTargetDefaultDialogueObject(),
							expression = "norm",
							text = "Of c-c-course, I'll also give you a reward for your efforts! " +
									"So what do you say?A-a-are you interested?"
						),
						next = ChoiceActionNode(
							id = UUID.fromString("52b7e39c-2c40-41e8-88ee-1ca65e846f10"),
							speaker = ActionTargetPartyMember(0),
							options = arrayOf(
								ChoiceEntry(
									expression = "grin",
									text = "Well, still yes!",
									next = acceptanceNode,
								),
								ChoiceEntry(
									expression = "susp",
									text = "What do you need the parts for?",
									next = explanationNode,
								),
								ChoiceEntry(
									expression = "norm",
									text = "Sorry, but we're busy.",
									next = rejectionNode,
								),
							),
						),
					),
				),
			),
		)
		val notStartedNode = FixedActionNode(
			id = UUID.fromString("4d1213db-98e6-495e-88e9-e0e5849ee1e6"),
			action = ActionTalk(
				speaker = ActionTargetDefaultDialogueObject(),
				expression = "smile",
				text = "Oh, h-h-hello there, Mardek and Deugan. Off on another adventure again t-t-today, are you, eh?"
			),
			next = FixedActionNode(
				id = UUID.fromString("ec4abb56-82c7-42a5-8a70-68498e4e2378"),
				action = ActionTalk(
					speaker = ActionTargetPartyMember(1),
					expression = "smile",
					text = "Yeh, we're off looking for a Fallen Star...!",
				),
				next = FixedActionNode(
					id = UUID.fromString("32fdd72a-59cc-4ae1-9e2c-03873664c227"),
					action = ActionTalk(
						speaker = ActionTargetDefaultDialogueObject(),
						expression = "smile",
						text = "Oh, excellent! Excellent. I-I-I actually have another adventure for you again today! " +
								"I-i-it won't take long; it's a sort of 'side-quest' thing. A-a-are you interested?"
					),
					next = ChoiceActionNode(
						id = UUID.fromString("8d0b03fb-9e9f-4ca7-8750-57f1674582de"),
						speaker = ActionTargetPartyMember(0),
						options = arrayOf(
							ChoiceEntry(
								expression = "grin",
								text = "Well, yes!",
								next = interestedNode,
							),
							ChoiceEntry(
								expression = "norm",
								text = "Sorry, but we're busy.",
								next = rejectionNode,
							),
						),
					),
				),
			),
		)

		val unfinishedNode = FixedActionNode(
			id = UUID.fromString("fb2f0fde-debe-4bdb-8357-21127207d187"),
			action = ActionTalk(
				speaker = ActionTargetDefaultDialogueObject(),
				expression = "smile",
				text = "There's n-n-no hurry, lads, so just have fun with it!"
			),
			next = null
		)

		val leadPipe = content.items.items.find { it.displayName == "LeadPipe" }!!
		val cogNecklace = content.items.items.find { it.displayName == "Cog Necklace" }!!
		val finishingNode = fixedActionChain(
			actions = arrayOf(
				ActionTalk(
					speaker = ActionTargetDefaultDialogueObject(),
					expression = "grin",
					text = "G-g-great! I knew I could c-c-count on you!",
				),
				ActionTakeItem(leadPipe, 5),
				ActionTimelineTransition("LeadPipeQuestTimeline", "Finished Pipe Quest"),
				ActionTalk(
					speaker = ActionTargetDefaultDialogueObject(),
					expression = "deep",
					text = "N-n-now, your reward... What c-c-can I give you...?",
				),
				ActionTalk(
					speaker = ActionTargetDefaultDialogueObject(),
					expression = "grin",
					text = "Ah-ha! I know! You can h-h-have this Cog Necklace! It's a little necklace I had " +
							"magically ench-ch-chanted to protect from Sleep, so then I could work longer hours " +
							"on m-my inventions, but the health problems outweighed the b-b-benefits... " +
							"It'd be useful in the w-w-woods, with all those pesky fungoblins around!"
				),
				ActionGiveItem(cogNecklace, 1),
				ActionTalk(
					speaker = ActionTargetPartyMember(0),
					expression = "grin",
					text = "Thanks!"
				),
				ActionTalk(
					speaker = ActionTargetDefaultDialogueObject(),
					expression = "smile",
					text = "N-n-now, shouldn't you boys be getting back to your big adventure? " +
							"I've held you up enough for t-t-today!"
				),
			),
			ids = arrayOf(
				UUID.fromString("1bc847bb-dd9f-425d-97dd-68c7f2edd55c"),
				UUID.fromString("fe27850c-adf3-4c02-9d6a-21ea91faadb0"),
				UUID.fromString("d3cc9900-10ae-4a02-8b5a-ccc81d51476b"),
				UUID.fromString("c567a1ec-6123-4e83-8a9b-7bd7a8168b9b"),
				UUID.fromString("41ddca54-4978-4f5d-8f23-01e9f54dbb54"),
				UUID.fromString("4068f431-de48-49ad-87e6-6cc00acf1ccc"),
				UUID.fromString("e9430cdc-6a67-4ee9-8e51-71db5196cd70"),
				UUID.fromString("7d49e232-1796-470d-b3ad-f10f83cbe657"),
			)
		)!!
		val alreadyStartedNode = FixedActionNode(
			id = UUID.fromString("a34e51a6-eadb-4e7c-af34-856ce4dd7e10"),
			action = ActionTalk(
				speaker = ActionTargetDefaultDialogueObject(),
				expression = "susp",
				text = "D-d-did you find the five LeadPipes yet?",
			),
			next = ChoiceActionNode(
				id = UUID.fromString("6607c088-2566-4d8e-9e28-527d710e16fb"),
				speaker = ActionTargetPartyMember(0),
				options = arrayOf(
					ChoiceEntry(
						expression = "grin",
						text = "Well, yes!",
						next = finishingNode,
						condition = ItemCountStateCondition(leadPipe, 5, null),
					),
					ChoiceEntry(
						expression = "deep",
						text = "We don't have five yet...",
						next = unfinishedNode,
					),
				),
			),
		)

		val alreadyFinishedNode = FixedActionNode(
			id = UUID.fromString("b5f05a0f-02d8-47f9-a5e1-bf43a18e02ad"),
			action = ActionTalk(
				speaker = ActionTargetDefaultDialogueObject(),
				expression = "norm",
				text = "H-h-hello again, boys. I've got nothing else for you to d-d-do today, I'm afraid. " +
						"Maybe come back later though and I can show you my n-n-new inv-v-vention?"
			),
			next = null,
		)
		val quest = content.story.quests.find { it.tabName == "LeadPipes" }!!
		ExpressionActionNode(
			id = UUID.fromString("0a1c2f76-9d1a-4ceb-8933-5a701826494c"),
			expression = IfElseStateExpression(
				condition = DefinedVariableStateCondition(quest.wasCompleted),
				ifTrue = ConstantStateExpression(ExpressionActionNodeValue(alreadyFinishedNode)),
				ifFalse = IfElseStateExpression(
					condition = DefinedVariableStateCondition(quest.isActive),
					ifTrue = ConstantStateExpression(ExpressionActionNodeValue(alreadyStartedNode)),
					ifFalse = ConstantStateExpression(ExpressionActionNodeValue(notStartedNode)),
				),
			),
		)
	}

	val workbenchRoot = FixedActionNode(
		id = UUID.fromString("a9d90bfa-81dc-473a-90cf-331941efcaad"),
		action = ActionTalk(
			speaker = ActionTargetDefaultDialogueObject(),
			expression = "",
			text = "This is Meraeador's Workbench, where he builds inventions.",
		),
		next = null // TODO CHAP3 Add invention crafting
	)
	// TODO CHAP2 CHAP3 Add the rest of the possibilities
	hardcoded["gz_house02"] = mutableListOf(
		ActionSequence(name = "inventor", root = chapter1Node),
		ActionSequence(name = "workbench", root = workbenchRoot)
	)
}
