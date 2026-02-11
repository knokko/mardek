package mardek.importer.actions

import mardek.content.Content
import mardek.content.action.ActionSequence
import mardek.content.action.ActionTalk
import mardek.content.action.ActionTargetDefaultDialogueObject
import mardek.content.action.FixedActionNode
import java.util.UUID

internal fun hardcodeMardekHouseActions(
	content: Content, hardcoded: MutableMap<String, MutableList<ActionSequence>>
) {
	val motherRoot = FixedActionNode( // TODO CHAP1 Change this, depending on the timeline state
		id = UUID.fromString("b436e5c5-3163-4576-86d3-df4c435934c6"),
		action = ActionTalk(
			speaker = ActionTargetDefaultDialogueObject(),
			expression = "smile",
			text = "Sweet dreams, dear."
		),
		next = null,
	)
	hardcoded["gz_Mhouse1"] = mutableListOf(
		ActionSequence(name = "mother", root = motherRoot),
	)
}
