package mardek.importer

import com.github.knokko.bitser.Bitser
import mardek.content.BITSER
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
import mardek.content.action.WalkSpeed
import mardek.content.animation.CombatantAnimations
import mardek.content.area.Direction
import mardek.importer.actions.addDummyCutscenes
import mardek.importer.actions.fixedActionChain
import mardek.importer.actions.importCutscenes
import mardek.importer.area.importAreaBattleContent
import mardek.importer.area.importAreaContent
import mardek.importer.area.importAreaSprites
import mardek.importer.audio.importAudioContent
import mardek.importer.battle.importBattleContent
import mardek.importer.battle.importMonsters
import mardek.importer.characters.importPlayableCharacters
import mardek.importer.inventory.hardcodeItemTypes
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
import java.util.UUID

fun importVanillaContent(skipMonsters: Boolean = false): Content {

	val content = Content()
	importAudioContent(content.audio)
	importParticleEffects(content)
	importStatsContent(content)
	if (!skipMonsters) {
		importPortraits(content)
		importCutscenes(content)
	} else addDummyCutscenes(content.actions)
	importSkillsContent(content)
	val fatItemTypes = hardcodeItemTypes(content)

	val playerModelMapping = mutableMapOf<String, CombatantAnimations>()
	importClasses(content, fatItemTypes)
	importAreaSprites(content)
	if (!skipMonsters) importMonsters(content, playerModelMapping)
	importPlayableCharacters(content, playerModelMapping)
	importItemsContent(content, fatItemTypes)
	importBattleContent(content, !skipMonsters)
	importSimpleStoryContent(content.story)
	importAreaBattleContent(content)
	val hardcodedActions = importAreaContent(content)
	hardcodeTimeline(content)
	hardcodedActions.storeHardcodedActionSequences(content)
	content.ui = importUiSprites()
	content.fonts = importFonts()

	val startChapter1 = CampaignState()

	val introCutscene = content.actions.cutscenes.find { it.name == "Chapter 1 intro" }!!
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
		), arrayOf(
			UUID.fromString("6529c1ea-9723-4c5c-aef3-a21bed194a0f"),
			UUID.fromString("d7ffc98c-4242-4a0e-aee0-94928ed5d641"),
			UUID.fromString("d995719d-f8d1-4325-8585-562b98008dd9"),
			UUID.fromString("ac7841da-28d9-4b7c-a6d5-8c60c19def85"),
			UUID.fromString("9b380007-5cff-4252-a1d5-4f7d7f701209"),
			UUID.fromString("a411267d-4b4c-471a-af81-c7b924ee9c24"),
			UUID.fromString("c5502158-531c-451a-92c7-a59c635b2012"),
			UUID.fromString("85fbd941-f7c8-40ef-88ec-a80366e0cc5d"),
			UUID.fromString("9d8dd7bd-3dfa-49ff-bd91-2f32b63ccc1b"),
			UUID.fromString("bf3c99d1-1db6-4aa9-ab72-f2463468e8d5"),
			UUID.fromString("b8e555e8-09c4-4525-a41d-4b5e7cb15d77"),
			UUID.fromString("c305ce9e-7e6a-4a50-a83a-7372b3502a28"),
			UUID.fromString("af25ed4f-b5a0-4ee6-b919-b55e40fdfac6"),
			UUID.fromString("47a462a4-8600-4255-9677-0827316c4e59"),
			UUID.fromString("d5dedffd-edbf-4b57-80ed-fbdb16fa5ead"),
			UUID.fromString("3f3b309d-8e94-4b18-9ad3-623fccf7b483"),
			UUID.fromString("50cb7de9-ca41-4924-b011-448610560807"),
		))!!
		ActionSequence(name = "Chapter 1 intro", root = entryRoot)
	}
	content.actions.global.add(chapter1IntroSequence)
	startChapter1.state = CampaignActionsState(chapter1IntroSequence.root)

	hardcodedActions.resolveIncompleteActions(content)

	fun addCheckpoint(name: String, state: CampaignState) {
		content.checkpoints[name] = BITSER.toBytes(state, content, Bitser.BACKWARD_COMPATIBLE)
	}

	addCheckpoint("chapter1", startChapter1)

	return content
}
