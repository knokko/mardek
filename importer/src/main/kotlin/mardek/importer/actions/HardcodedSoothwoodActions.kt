package mardek.importer.actions

import mardek.content.Content
import mardek.content.action.ActionBattle
import mardek.content.action.ActionFadeCharacter
import mardek.content.action.ActionSequence
import mardek.content.action.ActionTalk
import mardek.content.action.ActionTargetAreaCharacter
import mardek.content.action.ActionTargetDefaultDialogueObject
import mardek.content.action.ActionTargetPartyMember
import mardek.content.action.ActionTimelineTransition
import mardek.content.action.FixedActionNode
import mardek.content.battle.Battle
import mardek.content.battle.Enemy
import java.util.UUID

internal fun hardcodeSoothwoodActions(
	content: Content, hardcoded: MutableMap<String, MutableList<ActionSequence>>
) {
	val speakerMardek = ActionTargetPartyMember(0)
	val speakerDeugan = ActionTargetPartyMember(1)
	val childConversationRoot = fixedActionChain(arrayOf(
		ActionTalk(
			speaker = speakerMardek,
			expression = "norm",
			text = "Well, this is the Soothwood.",
		),
		ActionTalk(
			speaker = speakerDeugan,
			expression = "norm",
			text = "Yes, we have to travel through this to get to where the thing crashed!",
		), ActionTalk(
			speaker = speakerDeugan,
			expression = "susp",
			text = "But wait, Mardek... Do we have our skills properly equipped? " +
					"We should probably check from the menu!",
		),
		ActionTalk(
			speaker = speakerMardek,
			expression = "susp",
			text = "What's a menu?"
		),
		ActionTalk(
			speaker = speakerDeugan,
			expression = "norm",
			text = "Uh... nevermind. Let's just continue on through the forest. " +
					"It's not very big so we should be able to get to the other side pretty quickly!",
		),
		ActionTalk(
			speaker = speakerMardek,
			expression = "grin",
			text = "Okay!",
		),
	), arrayOf(
		UUID.fromString("ab102665-448f-4f7d-a271-9ab9229df3ae"),
		UUID.fromString("26ad4ba8-ee08-4db3-9d54-d7b13665cacb"),
		UUID.fromString("92784ef9-9854-4170-ba67-58664b7ff91f"),
		UUID.fromString("99cc02bb-7e28-474a-aff8-1f70137f4ad7"),
		UUID.fromString("85240c77-3f0c-4c85-9e1a-9f2f785b4a05"),
		UUID.fromString("4f39cac2-9f2c-417b-a330-0f9b066106ab"),
	))!!

	// The monster & background lists can be empty during unit tests
	if (content.battle.monsters.isNotEmpty() && content.battle.backgrounds.isNotEmpty()) {
		val poshGoblin = content.battle.monsters.find { it.name == "poshgoblin" }!!
		val battleBackground = content.battle.backgrounds.find { it.name == "darkwood" }!!
		val poshGoblinRoot = fixedActionChain(actions = arrayOf(
			ActionTalk(
				speaker = ActionTargetDefaultDialogueObject(),
				expression = "norm",
				text = "Snee! Snee! I say! Snee! Pip-pip! Snee.",
			),
			ActionBattle(
				battle = Battle(
					startingEnemies = arrayOf(Enemy(
						monster = poshGoblin, overrideDisplayName = "PoshGoblin", level = 2,
					), null, null, null),
					enemyLayout = content.battle.enemyPartyLayouts.find { it.name == "SOLO" }!!,
					music = "battle",
					lootMusic = "VictoryFanfare2",
					background = battleBackground,
					canFlee = false,
					isRandom = false,
				),
				overridePlayers = null,
			),
			ActionFadeCharacter(target = ActionTargetAreaCharacter(
				characterID = UUID.fromString("b7536bf5-26e2-4353-9199-942783853e43")
			)),
			ActionTimelineTransition(
				timelineName = "MainTimeline",
				nodeName = "Defeated PoshGoblin",
			),
		), ids = arrayOf(
			UUID.fromString("3f4689a0-85c0-42aa-a607-299785fe53cd"),
			UUID.fromString("0abbe143-dce5-4926-be44-4d8f00b6dc73"),
			UUID.fromString("fa7b0b70-a2b0-47ac-be7f-9f1d026a2607"),
			UUID.fromString("2e70689a-3cf4-4803-a864-58bfcc808704"),
		))!!

		hardcoded["soothwood"] = mutableListOf(
			ActionSequence(name = "ChildConversation", root = childConversationRoot),
			ActionSequence(name = "PoshGoblin", root = poshGoblinRoot),
		)
	} else {
		hardcoded["soothwood"] = mutableListOf(
			ActionSequence(name = "ChildConversation", root = childConversationRoot),
			ActionSequence(name = "PoshGoblin", root = FixedActionNode()),
		)
	}

	val shamanConversation = FixedActionNode(
		id = UUID.fromString("a15db6ba-ed67-4b3b-9389-addf4fa1217f"),
		action = ActionTalk(
			speaker = ActionTargetDefaultDialogueObject(),
			expression = "smile",
			text = "Greetings, children. Out playing in the woods again, I see... Mind yourselves, children; " +
					"the beasts in the woods are not particularly dangerous, but they can get aggressive. " +
					"There's a Healing Crystal there if you are wounded."
		),
		next = null,
	) // TODO CHAP2 Change, depending on chapter
	hardcoded["soothwood_shaman"] = mutableListOf(
		ActionSequence(name = "c_shaman", root = shamanConversation)
	)
}
