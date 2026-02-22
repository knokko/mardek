package mardek.importer.actions

import mardek.content.action.ActionSequence
import mardek.content.action.ActionTalk
import mardek.content.action.ActionTargetDefaultDialogueObject
import java.util.UUID

internal fun hardcodeGoznorHouse1Dialogues(
	hardcoded: MutableMap<String, MutableList<ActionSequence>>
) {
	// TODO CHAP2 CHAP3 Change dialogue based on chapter
	val manRoot = fixedActionChain(
		actions = arrayOf(
			ActionTalk(
				speaker = ActionTargetDefaultDialogueObject(),
				expression = "grin",
				text = "Hello, boys! Welcome to our humble home! " +
						"Make yourselves comfortable and stay as long as you like!"
			),
			ActionTalk(
				speaker = ActionTargetDefaultDialogueObject(),
				expression = "norm",
				text = "I'm afraid I don't have anything interesting to say, though. " +
						"Everyone's talking about some weird star that fell from the sky... " +
						"Of course, I didn't see it because apparently I never leave this chair!"
			),
		),
		ids = arrayOf(
			UUID.fromString("afb7c6b5-0ed6-4d6e-b0e6-b6bfeb290f13"),
			UUID.fromString("13bbc2c0-6959-4606-885c-1c17687d9e1f"),
		),
	)!!
	val womanRoot = fixedActionChain(
		actions = arrayOf(
			ActionTalk(
				speaker = ActionTargetDefaultDialogueObject(),
				expression = "blah",
				text = "Children are so rude these days! Walking into peoples' houses uninvited, all willy-nilly! " +
						"And talking to the people who live there, no less! Well I never! " +
						"Who do you think you are, adventurers or something?"
			),
		),
		ids = arrayOf(UUID.fromString("e839b1e5-a113-4d4c-8366-051ee422ced1")),
	)!!
	hardcoded["gz_house01"] = mutableListOf(
		ActionSequence(name = "man", root = manRoot),
		ActionSequence(name = "woman", root = womanRoot),
	)
}
