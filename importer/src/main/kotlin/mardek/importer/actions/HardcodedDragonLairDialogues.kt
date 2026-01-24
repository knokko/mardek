@file:Suppress("CanConvertToMultiDollarString")

package mardek.importer.actions

import mardek.content.Content
import mardek.content.action.ActionBattle
import mardek.content.action.ActionFadeCharacter
import mardek.content.action.ActionParallel
import mardek.content.action.ActionRotate
import mardek.content.action.ActionSequence
import mardek.content.action.ActionSetMoney
import mardek.content.action.ActionTalk
import mardek.content.action.ActionTargetAreaCharacter
import mardek.content.action.ActionTargetPartyMember
import mardek.content.action.ActionTargetWholeParty
import mardek.content.action.ActionTeleport
import mardek.content.action.ActionTimelineTransition
import mardek.content.action.ActionToArea
import mardek.content.action.ActionWalk
import mardek.content.action.WalkSpeed
import mardek.content.area.Direction
import mardek.content.battle.Battle
import mardek.content.battle.BattleBackground
import mardek.content.battle.Enemy
import mardek.content.battle.Monster
import java.util.UUID

internal fun hardcodeDragonLairActions(
	content: Content, hardcoded: MutableMap<String, MutableList<ActionSequence>>
) {
	hardcodeDragonLairRoom2Actions(hardcoded)
	hardcodeDragonLairRoom4Actions(content, hardcoded)
}

private fun hardcodeDragonLairRoom2Actions(hardcoded: MutableMap<String, MutableList<ActionSequence>>) {
	val targetMardek = ActionTargetPartyMember(0)
	val targetDeugan = ActionTargetPartyMember(1)

	val entryRoot = fixedActionChain(arrayOf(
		ActionRotate(targetMardek, Direction.Down),
		ActionTalk(targetDeugan, "grin", "Remember, Mardek, with our super-duper Hero " +
				"Powers, we're practically invincible if we successfully use all our \$Reactions% well enough with the " +
				"\$E key right when we attack or get attacked%! But we've got to get the timing right, " +
				"so let's experiment a bit, and use these \$Reactions% to our advantage!"
		),
		ActionTalk(targetDeugan, "blah", "(Apparently I have to mention this or else " +
				"you'll probably not figure out that reaction commands exist at all.)"
		),
		ActionTalk(targetMardek, "susp", "...Huh?"),
		ActionTalk(targetDeugan, "grin",
			"Let's just continue on our way, Mardek! Tally-ho!"
		),
	), arrayOf(
		UUID.fromString("96f6dcae-c59a-474f-a885-4b9c01843d6d"),
		UUID.fromString("2ed35a29-8ceb-4358-9d4d-51796199d326"),
		UUID.fromString("60276244-f4f6-48e0-ba56-2a2e2bba149d"),
		UUID.fromString("8c30eac2-5f9b-4ec5-b3fe-ba14f44fae08"),
		UUID.fromString("9b8945eb-08f0-4958-8ec0-2e4eaebbbe81"),
	))!!

	hardcoded["DL_area2"] = mutableListOf(
		ActionSequence(name = "Entry", root = entryRoot)
	)
}

private fun hardcodeDragonLairRoom4Actions(
	content: Content, hardcoded: MutableMap<String, MutableList<ActionSequence>>
) {
	val targetMardek = ActionTargetPartyMember(0)
	val targetDeugan = ActionTargetPartyMember(1)
	val targetDragon = ActionTargetAreaCharacter(UUID.fromString("6d8a7f59-5b45-4054-8266-49eae259fdbb"))
	val targetPrincess = ActionTargetAreaCharacter(UUID.fromString("7ab53ddd-39bd-4dd2-9748-7a2d0732d5d4"))

	val dragonMonster = if (content.battle.monsters.isNotEmpty()) {
		content.battle.monsters.find { it.name == "mightydragon" }!!
	} else Monster()
	val dragonEnemy = Enemy(dragonMonster, 40, "The Dragon")
	val dragonPartyLayout = content.battle.enemyPartyLayouts.find { it.name == "DRAGON" }!!
	val dragonLairBackground = if (content.battle.backgrounds.isNotEmpty()) {
		content.battle.backgrounds.find { it.name == "dragonlair" }!!
	} else BattleBackground()

	val entryRoot = fixedActionChain(arrayOf(
		ActionWalk(ActionTargetWholeParty(), 6, 8, WalkSpeed.Normal),
		ActionTalk(targetDragon, "norm", "HAHAHAH! HEROES! " +
				"YOU ARE... (Uh... What would a dragon say..?)... YOU ARE PATHETIC MORTALS! I HAVE A PRINCESS! " +
				"YOU CAN'T HAVE THIS PRINCESS! THIS PRINCESS IS MINE!"),
		ActionTalk(targetDeugan, "angr", "We will slay you, mighty The Dragon! " +
				"For we are great and Mighty Heroes, even mightier than you! Tally-ho forsooth!"),
		ActionTalk(targetMardek, "grin", "We'll kick your arse dragon!"),
		ActionTalk(targetDeugan, "susp", "Do dragons have arses?"),
		ActionTalk(targetMardek, "grin", "I bet they do!"),
		ActionTalk(targetDeugan, "grin", "If they do, " +
				"then we'll kick it like it's never been kicked before!"),
		ActionTalk(targetMardek, "grin", "Yeh! With our big swords!"),
		ActionTalk(targetDeugan, "angr", "So bring it on, dragon!"),
		ActionTalk(targetDragon, "norm", "HAHAHAH! YOU WILL NEVER DEFEAT ME!!!"),
		ActionBattle(Battle(
			startingEnemies = arrayOf(dragonEnemy, null, null, null),
			enemyLayout = dragonPartyLayout,
			music = "BossBattle",
			lootMusic = "VictoryFanfare2",
			background = dragonLairBackground,
			canFlee = false,
			isRandom = false,
		), null),
		ActionFadeCharacter(targetDragon),
		ActionTalk(targetDragon, "norm", "OH DEAR!! I HAVE BEEN SLAIN!!!"),
		ActionParallel(arrayOf(
			ActionWalk(targetPrincess, 6, 7, WalkSpeed.Slow),
			ActionTalk(targetMardek, "grin", "Well, we beated the dragon!"),
		)),
		ActionTalk(targetPrincess, "smile",
			"Oh thank you so much for saving me, mighty heroes!"),
		ActionTalk(targetMardek, "grin", "Well it was really all my fault."),
		ActionTalk(targetDeugan, "sad", "All your *fault*? " +
				"Do you mean all your *doing* or something? Because that's not fair! We beat the dragon together..."),
		ActionTalk(targetMardek, "blah", "Well I still get the princess."),
		ActionRotate(targetMardek, Direction.Down),
		ActionTalk(targetDeugan, "susp", "You can have her! " +
				"She's a *giiiirl*! Girls are headlice, I heard! " +
				"They suck on your wallet and drain out all your money, that's what my dad says!"),
		ActionTalk(targetMardek, "shok", "Really?!"),
		ActionTalk(targetDeugan, "grin", "Yeh, really! My dad told me so it must be true!"),
		ActionTalk(targetMardek, "smile",
			"Now that we've beatened the dragon though, now what do we do?"),
		ActionToArea("heroes_den", 10, 6, Direction.Down),
		ActionTimelineTransition("MainTimeline", "Childhood"),
		ActionSetMoney(10),
		ActionTeleport(targetDeugan, 10, 7, Direction.Up),
		ActionTalk(targetDeugan, "norm", "It's getting late... I suppose we should head home. I bet our parents are worried!"),
		ActionTalk(targetMardek, "grin", "Okay. Let's go back to Goznor!"),
	), arrayOf(
		UUID.fromString("3bbcc73a-2fd3-47a8-91a3-6bc1506ce7c4"),
		UUID.fromString("f6bf48ab-466b-49f2-a07a-1cf019b014d9"),
		UUID.fromString("36f11cb1-2bdc-4212-85cb-923b97a6c855"),
		UUID.fromString("88620bc0-4436-425b-9e07-d59cd508f428"),
		UUID.fromString("c8419be0-d858-4f5c-b655-07a10fd58853"),
		UUID.fromString("57d71da4-3140-41ea-a694-cd23e88fac19"),
		UUID.fromString("2ba401d3-87f3-4344-9511-238b410c7106"),
		UUID.fromString("44f161b3-4779-4dd3-a114-58b29fcb3593"),
		UUID.fromString("7457b83a-174a-43b5-b7a3-2ca756074c88"),
		UUID.fromString("e710c442-3ef8-47c7-96ef-a22c4482c0cf"),
		UUID.fromString("445bc2ea-066c-4e33-ac95-3c945ad63581"),
		UUID.fromString("d37b9151-57d0-4b23-b845-c75e75cbaa9c"),
		UUID.fromString("c1f26b99-75e5-4330-a538-965b1296cc6e"),
		UUID.fromString("43b2ad90-7a47-4b83-98c5-6242ae5b5350"),
		UUID.fromString("2eef2566-b178-461f-b8ca-b36b4544f7ee"),
		UUID.fromString("21ba3446-a2a6-4ffa-a129-8caaa0cb6df0"),
		UUID.fromString("75bea984-aaf8-4595-81bd-afdbfc3407da"),
		UUID.fromString("8b5ee060-b42e-479a-84bf-4ab5eae7b38d"),
		UUID.fromString("0043e5ce-4c5a-4358-bdc6-9926e2568877"),
		UUID.fromString("b9e2f1f1-cdf0-415c-b324-6937148821df"),
		UUID.fromString("8f050669-ac81-4221-b278-b4a45a57a6b5"),
		UUID.fromString("88841a40-fa47-4a97-a37c-778eb7db232f"),
		UUID.fromString("4cbbf663-e2b3-4669-a812-40f78be1d181"),
		UUID.fromString("8eeba3a1-480e-4673-aa1e-556e35b47885"),
		UUID.fromString("5fc6cb96-1960-48ec-b3eb-24cf8ebdcc3a"),
		UUID.fromString("086ba95f-2ca1-4f04-8a09-3a1c9dd175d9"),
		UUID.fromString("558ce3f5-586d-444d-8de1-bc2ec78535e9"),
		UUID.fromString("24638873-05a6-4e51-8f2b-ba07c0f15b6b"),
		UUID.fromString("610fe299-3b1c-49d8-9672-8f70d4573d2e"),
	))!!

	hardcoded["DL_area4"] = mutableListOf(
		ActionSequence(name = "Dragon", root = entryRoot)
	)
}
