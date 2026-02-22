package mardek.importer.actions

import mardek.content.action.ActionSequence
import mardek.content.action.ActionTalk
import mardek.content.action.ActionTargetAreaCharacter
import mardek.content.action.ActionTargetDefaultDialogueObject
import java.util.UUID

internal fun hardcodeGoznorInnActions(
	hardcoded: MutableMap<String, MutableList<ActionSequence>>
) {
	val chapter1Woman = fixedActionChain(
		actions = arrayOf( // TODO CHAP2 Completely different dialogue
			ActionTalk(
				speaker = ActionTargetDefaultDialogueObject(),
				expression = "grin",
				text = "Wasn't that falling star EXCITING! It was one of the most exciting, " +
						"thrilling things I ever did see!",
			),
			ActionTalk(
				speaker = ActionTargetDefaultDialogueObject(),
				expression = "susp",
				text = "Why didn't we have any falling stars at our wedding, luv? " +
						"I would've wanted falling stars at my wedding.",
			),
			ActionTalk(
				speaker = ActionTargetAreaCharacter(
					UUID.fromString("0276c358-c743-46f2-8a3c-046be60ac202")
				),
				expression = "shok",
				text = "I didn't know you wanted falling stars at our wedding, dearest!",
			),
			ActionTalk(
				speaker = ActionTargetDefaultDialogueObject(),
				expression = "sad",
				text = "Well I guess you just don't love me after all then!!!",
			),
		),
		ids = arrayOf(
			UUID.fromString("f6cd4570-4cdd-4dab-9e53-937669757491"),
			UUID.fromString("e0e21d4b-e93f-4841-a0ec-90d709090c94"),
			UUID.fromString("43cdf0d1-9767-48e4-8c71-23eae66b91ee"),
			UUID.fromString("f38ab916-2bda-4f57-8126-62752e93f504"),
		)
	)!!
	hardcoded["gz_inn"] = mutableListOf(
		ActionSequence(name = "Chapter1Woman", root = chapter1Woman),
	)
}
