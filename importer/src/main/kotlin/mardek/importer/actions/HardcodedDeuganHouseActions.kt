package mardek.importer.actions

import mardek.content.action.ActionSequence
import mardek.content.action.ActionTalk
import mardek.content.action.ActionTargetDefaultDialogueObject
import mardek.content.action.ActionTargetPartyMember
import java.util.UUID

internal fun hardcodeDeuganHouseActions(
	hardcoded: MutableMap<String, MutableList<ActionSequence>>
) {
	val bookContent = arrayOf(
		"The elemental crystals were created by YALORT when He made the world, " +
				"and they provide Belfan with all of its magical energy and keep it alive. " +
				"They consist of pure elemental magical energy, and as such have the power to grant people " +
				"extreme power; for this reason, they have been sought by Adventurers and Villains alike.",
		"All of the crystals are located around the gulf separating Hadris and Fengue; " +
				"it is said that this general area is where YALORT came down to the newly-crafted " +
				"Belfan and breathed the gift of life into it.",
		"The Fire Crystal possesses the power of heat and flames. " +
				"It keeps the world warm so that things can grow and thrive, " +
				"and its power is harnessed every time a flame burns, magical or natural. " +
				"It is located in a temple in Fengue.",
		"The Water Crystal keeps the waters pure and flowing and the rains pouring. It is vital for life on Belfan. " +
				"It is also responsible for Man's rational thoughts, or so it has been said. " +
				"It can be found in Hadris.",
		"The Air Crystal keeps the winds blowing, and the air clean. " +
				"The city of Aeropolis was built around the temple in which it can still be found.",
		"The Earth Crystal is responsible for the lives of beasts and plants on Belfan. " +
				"Its energy keeps them growing and breeding. " +
				"It is held in a sacred temple within the Lifewood in Fengue.",
		"The Light Crystal, being the physical manifestation of the Goodness in Man's heart, " +
				"is the greatest treasure of Goznor, kept within the Castle of Goznor.",
		"The location of the Dark Crystal is unknown; this is probably for the best, " +
				"since it is the crystal most often sought by those who wish to inflict evil on the world.",
	)
	val crystalsBookRoot = fixedActionChain(
		actions = bookContent.map {
			ActionTalk(speaker = ActionTargetDefaultDialogueObject(), expression = "", text = it)
		}.toTypedArray(), // TODO CHAP3 Add the 6 crystals to the encyclopedia artefacts
		ids = arrayOf(
			UUID.fromString("5b31f266-0d3f-44de-ac6a-f4bd539e8816"),
			UUID.fromString("2697b313-af9c-44bf-a1f6-7a449f5b5d24"),
			UUID.fromString("37840978-4904-493f-9684-574826b4c1e5"),
			UUID.fromString("807caed6-260a-43bd-9771-0f9b6b71aae8"),
			UUID.fromString("967abbe1-7ab6-4422-a8c2-139b2d018d37"),
			UUID.fromString("72848269-93ac-4071-b2d8-839724116367"),
			UUID.fromString("e4d7de95-f78a-4421-8fb5-25862bb7053a"),
			UUID.fromString("cb5ab156-1679-4845-8477-9399243abb34"),
		),
	)!!
	val pollyRoot = fixedActionChain( // TODO CHAP2 Use different dialogue
		actions = arrayOf(
			ActionTalk(
				speaker = ActionTargetDefaultDialogueObject(),
				expression = "smile",
				text = "Eee, 'ello there, dear! Dears! An 'ow're you, Mardek?",
			),
			ActionTalk(
				speaker = ActionTargetPartyMember(0),
				expression = "norm",
				text = "Well, I'm alright, miss Deugan's mum!",
			),
			ActionTalk(
				speaker = ActionTargetDefaultDialogueObject(),
				expression = "smile",
				text = "You two're goin' out on an adventure today, are you? Just watch yerselves. " +
						"I don't want you gettin' 'urt or scared again by doin' somethin' stupid or dangerous! " +
						"Like that time where you both tried to walk over that river on that narrow fallen tree " +
						"and Deugan didn't go and was so scared he crapped his pants! " +
						"Did I show you those photos of when Deugan crapped his pants, Mardek?",
			),
			ActionTalk(
				speaker = ActionTargetPartyMember(1),
				expression = "shok",
				text = "Muuum!",
			),
			ActionTalk(
				speaker = ActionTargetPartyMember(0),
				expression = "susp",
				text = "What's a photo?",
			),
			ActionTalk(
				speaker = ActionTargetDefaultDialogueObject(),
				expression = "smile",
				text = "Anyway, 'adn't you two best be off now? " +
						"You'll miss the whole day if you dilly-dally much longer!",
			),
		),
		ids = arrayOf(
			UUID.fromString("f768e068-8f7c-4410-b613-4b5611e963a3"),
			UUID.fromString("f72d3d4a-43a7-49e4-b40d-4d0aef938edb"),
			UUID.fromString("8b6fd8e8-d4d6-42ce-a348-8504f997acf6"),
			UUID.fromString("ac5a2136-ad62-453c-8200-be2633955faf"),
			UUID.fromString("011d8a8e-17ce-4665-9bfc-450c3c962fc6"),
			UUID.fromString("35f4773e-ad9e-4c99-ab2f-1ad99f15fe26"),
		),
	)!!
	// TODO CHAP3 Add Derek
	hardcoded["gz_Dhouse"] = mutableListOf(
		ActionSequence(name = "CrystalsBook", root = crystalsBookRoot),
		ActionSequence(name = "Polly", root = pollyRoot),
	)
}
