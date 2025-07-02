@file:Suppress("CanConvertToMultiDollarString")

package mardek.importer.actions

import mardek.content.action.ActionSequence
import mardek.content.action.ActionTalk
import mardek.content.action.ActionTargetPartyMember
import mardek.content.action.ActionTargetWholeParty
import mardek.content.action.ActionWalk
import mardek.content.action.WalkSpeed

internal fun hardcodeDragonLairActions(hardcoded: MutableMap<String, MutableList<ActionSequence>>) {
	hardcodeDragonLairEntryActions(hardcoded)
	hardcodeDragonLairRoom2Actions(hardcoded)
}

private fun hardcodeDragonLairEntryActions(hardcoded: MutableMap<String, MutableList<ActionSequence>>) {
	val targetMardek = ActionTargetPartyMember(0)
	val targetDeugan = ActionTargetPartyMember(1)

	val entryRoot = fixedActionChain(arrayOf(
		ActionWalk(ActionTargetWholeParty(), 5, 5, WalkSpeed.Normal),
		ActionTalk(targetMardek, "norm", "Well Deugan, this is The Dragon's Lair."),
		ActionTalk(targetDeugan, "grin", "Yes, Mardek, that it is! We have to get to the dragon and slay it to rescue the Princess! Tally-ho!"),
		// TODO Give quest
		ActionTalk(targetMardek, "susp", "What does 'tally-ho' mean?"),
		ActionTalk(targetDeugan, "deep", "Uhm... I'm not sure! But I've heard adventurers say it before maybe! It sounds like something they'd say!"),
		ActionTalk(targetMardek, "grin", "Tally-ho!"),
		ActionTalk(targetDeugan, "grin", "Tally-ho! We're adventurers! En guard! Forsooth! Bloody goblins!"),
		ActionTalk(targetMardek, "grin", "Tally-ho!"),
		ActionTalk(targetDeugan, "grin", "Now let's go and save that Princess! Tally-ho!"),
		ActionTalk(targetDeugan, "norm", "Oh, but Mardek, just a reminder about things! We can \$open the menu with the TAB key% to check our stats, skills and items! And we can also \$open doors and talk to people and stuff with the E key%! Remember these things!"),
		ActionTalk(targetDeugan, "norm", "It might be a good idea to \$read the Help section of the menu% now if you didn't read the Instructions already!"),
		// TODO Add help section :p
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
