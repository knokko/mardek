package mardek.importer.actions

import mardek.content.action.ActionSequence
import mardek.content.action.ActionTalk
import mardek.content.action.ActionTargetDefaultDialogueObject
import mardek.content.action.ActionTargetPartyMember
import java.util.UUID

internal fun hardcodeWeaponShopActions(
	hardcoded: MutableMap<String, MutableList<ActionSequence>>
) {
	val chapter1Dialogue = fixedActionChain(
		actions = arrayOf( // TODO CHAP2 Change dialogue in chapter 2
			ActionTalk(
				speaker = ActionTargetDefaultDialogueObject(),
				expression = "smile",
				text = "Hello there, son. Out on an adventure again today, are you?",
			),
			ActionTalk(
				speaker = ActionTargetPartyMember(1),
				expression = "norm",
				text = "Yes, dad... I saw this star fall from the sky, so me and Mardek are going to look for it!",
			),
			ActionTalk(
				speaker = ActionTargetDefaultDialogueObject(),
				expression = "norm",
				text = "A star, you say? That's the thing that other people saw too, they say... " +
						"Everyone's talking about it.",
			),
			ActionTalk(
				speaker = ActionTargetDefaultDialogueObject(),
				expression = "grin",
				text = "If you find it, boys, you'll have to tell me all about it!",
			),
			ActionTalk(
				speaker = ActionTargetDefaultDialogueObject(),
				expression = "norm",
				text = "As it is though, I can't come and see for myself; I have a shop to run! " +
						"And speaking of which, I have customers to probably attend to! So shoo, you two!"
			),
			ActionTalk(
				speaker = ActionTargetPartyMember(1),
				expression = "blah",
				text = "But dad, you barely EVER have customers. " +
						"Nobody in this little tiny village wants to buy weapons... Except us, but we're too young."
			),
			ActionTalk(
				speaker = ActionTargetDefaultDialogueObject(),
				expression = "grin",
				text = "But someone COULD come in at any moment! A weary and battle-hardened Adventurer, " +
						"just stopping by on one of his mighty quests! And he'll want good weapons, and fast! " +
						"And I'll be here to supply them! And he'll tell his friends! " +
						"And business will be booming in no time!"
			),
			ActionTalk(
				speaker = ActionTargetPartyMember(1),
				expression = "blah",
				text = "But dad, the weapons you sell here probably aren't very impressive to big adventurers " +
						"like Social Fox... And adventurers don't usually have many friends.",
			),
			ActionTalk(
				speaker = ActionTargetDefaultDialogueObject(),
				expression = "grin",
				text = "Sure they do! They have those... wossmacallits... yes, " +
						"they have those Celebrations following them around all the time! " +
						"There's always joy and festivities following close behind them all the time, they say! " +
						"And such festivities are always full of drunk people, and they'll buy ANYTHING!",
			),
			ActionTalk(
				speaker = ActionTargetPartyMember(1),
				expression = "norm",
				text = "Well, maybe you're right... I don't know. Uh, good luck anyway...! " +
						"Now, me and Mardek have to go and, uh... go on an adventure. " +
						"Yeh. So I'll be back later! Bye dad!",
			),
			ActionTalk(
				speaker = ActionTargetDefaultDialogueObject(),
				expression = "smile",
				text = "Okay boys, have fun!"
			),
		),
		ids = arrayOf(
			UUID.fromString("9897ddc4-825f-4881-be12-9b2764c66db7"),
			UUID.fromString("f200c3c4-6fb2-4c0b-8854-c07c50fc8f8f"),
			UUID.fromString("26168fae-af93-40bb-bb8b-8e17a9eaa3b0"),
			UUID.fromString("37150418-deb8-4e6e-aa2f-a51246352bd9"),
			UUID.fromString("9c11245c-8698-4f5c-92cf-016c11810a4e"),
			UUID.fromString("5720ec46-a9b7-4b37-9dd4-e876e754140f"),
			UUID.fromString("ecc00173-2fdc-42b0-b1b0-4e17d3bada11"),
			UUID.fromString("765608d9-a2e7-4216-9216-f7822385724f"),
			UUID.fromString("bc920593-9835-419a-879d-829cefa998bd"),
			UUID.fromString("5262160b-f57d-45e1-a381-c2da3955856a"),
			UUID.fromString("ba9e74fb-3664-42ee-bc2a-95a6020016f0"),
		)
	)!!
	hardcoded["gz_shop_W"] = mutableListOf(
		ActionSequence(name = "Chapter1Dialogue", root = chapter1Dialogue)
	)
}
