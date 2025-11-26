@file:Suppress("CanConvertToMultiDollarString")

package mardek.importer.actions

import mardek.content.Content
import mardek.content.action.ActionBattle
import mardek.content.action.ActionFadeCharacter
import mardek.content.action.ActionParallel
import mardek.content.action.ActionRotate
import mardek.content.action.ActionSequence
import mardek.content.action.ActionTalk
import mardek.content.action.ActionTargetAreaCharacter
import mardek.content.action.ActionTargetPartyMember
import mardek.content.action.ActionTargetWholeParty
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
	hardcodeDragonLairEntryActions(hardcoded)
	hardcodeDragonLairRoom2Actions(hardcoded)
	hardcodeDragonLairRoom4Actions(content, hardcoded)
}

private fun hardcodeDragonLairEntryActions(hardcoded: MutableMap<String, MutableList<ActionSequence>>) {
	val targetMardek = ActionTargetPartyMember(0)
	val targetDeugan = ActionTargetPartyMember(1)

	val entryRoot = fixedActionChain(arrayOf(
		ActionWalk(ActionTargetWholeParty(), 5, 5, WalkSpeed.Normal),
		ActionRotate(targetMardek, Direction.Down),
		ActionTalk(targetMardek, "norm", "Well Deugan, this is The Dragon's Lair."),
		ActionTalk(targetDeugan, "grin", "Yes, Mardek, that it is! We have to get to the dragon and slay it to rescue the Princess! Tally-ho!"),
		// TODO CHAP1 Give quest
		ActionTalk(targetMardek, "susp", "What does 'tally-ho' mean?"),
		ActionTalk(targetDeugan, "deep", "Uhm... I'm not sure! But I've heard adventurers say it before maybe! It sounds like something they'd say!"),
		ActionTalk(targetMardek, "grin", "Tally-ho!"),
		ActionTalk(targetDeugan, "grin", "Tally-ho! We're adventurers! En guard! Forsooth! Bloody goblins!"),
		ActionTalk(targetMardek, "grin", "Tally-ho!"),
		ActionTalk(targetDeugan, "grin", "Now let's go and save that Princess! Tally-ho!"),
		ActionTalk(targetDeugan, "norm", "Oh, but Mardek, just a reminder about things! We can \$open the menu with the TAB key% to check our stats, skills and items! And we can also \$open doors and talk to people and stuff with the E key%! Remember these things!"),
		ActionTalk(targetDeugan, "norm", "It might be a good idea to \$read the Help section of the menu% now if you didn't read the Instructions already!"),
		// TODO CHAP2 Add help section :p
		ActionTalk(targetMardek, "susp", "...Huh?"),
		ActionTalk(targetDeugan, "grin", "Uh, I mean... Tally-ho! Let's go and slay that dragon!"),
	))!!

	hardcoded["DL_entr"] = mutableListOf(
		ActionSequence(name = "Entry", root = entryRoot)
	)
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
			background = dragonLairBackground,
			canFlee = false,
		), null),
		ActionFadeCharacter(targetDragon),
		ActionTalk(targetDragon, "norm", "OH DEAR!! I HAVE BEEN SLAIN!!!"),
		// TODO CHAP1 DONEQUEST("HEROES");
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
		// TODO CHAP1 Go to Hero Den
	))!!

	hardcoded["DL_area4"] = mutableListOf(
		ActionSequence(name = "Dragon", root = entryRoot)
	)
}
