package mardek.importer.actions

import mardek.content.Content
import mardek.content.action.ActionSequence
import mardek.content.action.ActionTalk
import mardek.content.action.ActionTargetDefaultDialogueObject
import mardek.content.action.FixedActionNode
import java.util.UUID

internal fun hardcodeGoznorDialogues(
	content: Content, hardcoded: MutableMap<String, MutableList<ActionSequence>>
) {
	// TODO CHAP1 CHAP2 CHAP3 Change dialogue based on timeline state
	val bernardoRoot = FixedActionNode(
		id = UUID.fromString("6aa38b43-eb40-4d5e-b5ae-0a94593fe4f8"),
		action = ActionTalk(
			speaker = ActionTargetDefaultDialogueObject(),
			expression = "norm",
			text = "Oy, it's like a lickle boy or summat. Shouldn' you be goin' 'ome, sonny?",
		), next = null
	)
	val marcellusRoot = fixedActionChain(
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
	)!!
	// TODO CHAP1 Only spawn George at sunset/night during chapter 1
	// TODO CHAP1 Change conversation based on timeline
	val georgeRoot = fixedActionChain(
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
	hardcoded["goznor"]!!.addAll(listOf(
		ActionSequence(name = "guard_bernardo", root = bernardoRoot),
		ActionSequence(name = "guard_marcellus", root = marcellusRoot),
		ActionSequence(name = "blocking_george", root = georgeRoot),
	))
}
