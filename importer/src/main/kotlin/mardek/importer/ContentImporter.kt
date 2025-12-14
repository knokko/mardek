package mardek.importer

import com.github.knokko.bitser.io.BitOutputStream
import com.github.knokko.bitser.Bitser
import mardek.content.Content
import mardek.content.action.ActionPlayCutscene
import mardek.content.action.ActionSequence
import mardek.content.action.ActionShowChapterName
import mardek.content.action.ActionToArea
import mardek.content.action.FixedActionNode
import mardek.content.animation.CombatantAnimations
import mardek.content.area.Direction
import mardek.importer.actions.addDummyCutscenes
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
	val chapter1IntroSequence = ActionSequence(
		name = "Chapter 1 intro",
		root = FixedActionNode(
			action = ActionShowChapterName(1, "A Fallen Star"),
			next = FixedActionNode(
				action = ActionPlayCutscene(cutscene = introCutscene),
				next = FixedActionNode(
					action = ActionToArea("DL_entr", 5, 10, Direction.Up),
					next = null,
				)
			),
		)
	)
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
