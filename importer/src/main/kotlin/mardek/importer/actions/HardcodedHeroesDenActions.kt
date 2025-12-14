package mardek.importer.actions

import mardek.content.Content
import mardek.content.action.ActionSequence
import mardek.content.action.ActionSetMoney
import mardek.content.action.ActionTalk
import mardek.content.action.ActionTargetPartyMember
import mardek.content.action.ActionTeleport
import mardek.content.area.Direction

internal fun hardcodeHeroesDenActions(
	hardcoded: MutableMap<String, MutableList<ActionSequence>>
) {
	val targetMardek = ActionTargetPartyMember(0)
	val targetDeugan = ActionTargetPartyMember(1)

	val entryRoot = fixedActionChain(arrayOf(
		ActionSetMoney(10),
		ActionTeleport(targetDeugan, 10, 7, Direction.Up),
		ActionTalk(targetDeugan, "norm", "It's getting late... I suppose we should head home. I bet our parents are worried!"),
		ActionTalk(targetMardek, "grin", "Okay. Let's go back to Goznor!"),
	))!!

	hardcoded["heroes_den"] = mutableListOf(
		ActionSequence(name = "AfterDragonLair", root = entryRoot)
	)
}
