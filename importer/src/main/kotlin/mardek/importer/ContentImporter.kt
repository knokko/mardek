package mardek.importer

import com.github.knokko.bitser.io.BitOutputStream
import com.github.knokko.bitser.Bitser
import mardek.content.Content
import mardek.content.action.ActionPlayCutscene
import mardek.content.action.ActionRotate
import mardek.content.action.ActionSequence
import mardek.content.action.ActionShowChapterName
import mardek.content.action.ActionTalk
import mardek.content.action.ActionTargetPartyMember
import mardek.content.action.ActionTargetWholeParty
import mardek.content.action.ActionToArea
import mardek.content.action.ActionWalk
import mardek.content.action.FixedActionNode
import mardek.content.action.WalkSpeed
import mardek.content.animation.CombatantAnimations
import mardek.content.area.Direction
import mardek.importer.actions.addDummyCutscenes
import mardek.importer.actions.fixedActionChain
import mardek.importer.actions.generateUUIDs
import mardek.importer.actions.getAllActionNodesFromSequence
import mardek.importer.actions.importCutscenes
import mardek.importer.area.importAreaBattleContent
import mardek.importer.area.importAreaContent
import mardek.importer.area.importAreaSprites
import mardek.importer.audio.importAudioContent
import mardek.importer.battle.importBattleContent
import mardek.importer.characters.importPlayableCharacters
import mardek.importer.stats.importClasses
import mardek.importer.stats.importStatsContent
import mardek.importer.inventory.importItemsContent
import mardek.importer.particle.importParticleEffects
import mardek.importer.portrait.importPortraits
import mardek.importer.skills.importSkillsContent
import mardek.importer.ui.importFonts
import mardek.importer.ui.importUiSprites
import mardek.state.ingame.CampaignState
import mardek.state.ingame.actions.CampaignActionsState
import mardek.importer.story.hardcodeTimeline
import mardek.importer.story.importSimpleStoryContent
import java.io.ByteArrayOutputStream

fun importVanillaContent(bitser: Bitser, skipMonsters: Boolean = false): Content {

	val content = Content()
	importAudioContent(content.audio)
	importParticleEffects(content)
	importStatsContent(content)
	if (!skipMonsters) {
		importPortraits(content)
		importCutscenes(content.actions)
	} else addDummyCutscenes(content.actions)
	importSkillsContent(content)
	importItemsContent(content)

	val playerModelMapping = if (skipMonsters) null else mutableMapOf<String, CombatantAnimations>()
	importBattleContent(content, playerModelMapping)
	importClasses(content)
	importAreaSprites(content)
	importPlayableCharacters(content, playerModelMapping)
	importSimpleStoryContent(content.story)
	importAreaBattleContent(content)
	val hardcodedActions = importAreaContent(content)
	hardcodeTimeline(content)
	hardcodedActions.storeHardcodedActionSequences(content)
	content.ui = importUiSprites()
	content.fonts = importFonts()

	val startChapter1 = CampaignState()

	val introCutscene = content.actions.cutscenes.find { it.get().name == "Chapter 1 intro" }!!
	val chapter1IntroSequence = run {
		val targetMardek = ActionTargetPartyMember(0)
		val targetDeugan = ActionTargetPartyMember(1)

		@Suppress("CanConvertToMultiDollarString")
		val entryRoot = fixedActionChain(arrayOf(
			ActionShowChapterName(1, "A Fallen Star"),
			ActionPlayCutscene(cutscene = introCutscene),
			ActionToArea("DL_entr", 5, 10, Direction.Up),
			ActionWalk(ActionTargetWholeParty(), 5, 5, WalkSpeed.Normal),
			ActionRotate(targetMardek, Direction.Down),
			ActionTalk(targetMardek, "norm", "Well Deugan, this is The Dragon's Lair."),
			ActionTalk(targetDeugan, "grin", "Yes, Mardek, that it is! We have to get to the dragon and slay it to rescue the Princess! Tally-ho!"),
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
		ActionSequence(name = "Chapter 1 intro", root = entryRoot)
	}
	content.actions.global.add(chapter1IntroSequence)
	startChapter1.actions = CampaignActionsState(chapter1IntroSequence.root)
	generateUUIDs(chapter1IntroSequence)
	for (node in getAllActionNodesFromSequence(chapter1IntroSequence)) {
		if (node is FixedActionNode) {
			val action = node.action
			if (action is ActionToArea) action.resolve(content.areas.areas)
		}
	}

	fun addCheckpoint(name: String, state: CampaignState) {
		val byteOutput = ByteArrayOutputStream()
		val bitOutput = BitOutputStream(byteOutput)
		bitser.serialize(state, bitOutput, content, Bitser.BACKWARD_COMPATIBLE)
		bitOutput.finish()
		content.checkpoints[name] = byteOutput.toByteArray()
	}

	addCheckpoint("chapter1", startChapter1)

	return content
}
