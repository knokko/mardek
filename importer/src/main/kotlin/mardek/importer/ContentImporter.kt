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
import mardek.importer.actions.addDummyCutscenes
import mardek.importer.actions.generateUUIDs
import mardek.importer.actions.importCutscenes
import mardek.importer.area.importAreaBattleContent
import mardek.importer.area.importAreaContent
import mardek.importer.area.importAreaSprites
import mardek.importer.audio.importAudioContent
import mardek.importer.battle.importBattleContent
import mardek.importer.characters.FatPlayableCharacter
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
import mardek.state.ingame.characters.CharacterSelectionState
import mardek.state.ingame.characters.CharacterState
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
	val fatCharacters = importPlayableCharacters(content, playerModelMapping)
	importAreaBattleContent(content)
	importAreaContent(content)
	content.ui = importUiSprites()
	content.fonts = importFonts()

	val heroMardek = fatCharacters.find { it.wrapped.areaSprites.name == "mardek_hero" }!!
	val heroDeugan = fatCharacters.find { it.wrapped.areaSprites.name == "deugan_hero" }!!

	fun initState(fat: FatPlayableCharacter): CharacterState {
		val state = CharacterState()
		state.currentLevel = fat.initialLevel
		for ((index, item) in fat.initialEquipment.withIndex()) state.equipment[index] = item
		for ((index, itemStack) in fat.initialItems.withIndex()) state.inventory[index] = itemStack
		for (skill in fat.initialMasteredSkills) {
			state.skillMastery[skill] = skill.masteryPoints
		}
		state.toggledSkills.addAll(fat.initialToggledSkills)
		state.currentHealth = state.determineMaxHealth(fat.wrapped.baseStats, state.activeStatusEffects)
		state.currentMana = state.determineMaxMana(fat.wrapped.baseStats, state.activeStatusEffects)
		return state
	}

	val startChapter1 = CampaignState(
		currentArea = null,
		characterSelection = CharacterSelectionState(
			hashSetOf(heroMardek.wrapped, heroDeugan.wrapped),
			HashSet(0),
			arrayOf(heroMardek.wrapped, heroDeugan.wrapped, null, null)
		),
		characterStates = hashMapOf(
			Pair(heroMardek.wrapped, initState(heroMardek)),
			Pair(heroDeugan.wrapped, initState(heroDeugan))
		),
		gold = 0
	)

	val dragonLairEntry = content.areas.areas.find { it.properties.rawName == "DL_entr" }!!

	val introCutscene = content.actions.cutscenes.find { it.get().name == "Chapter 1 intro" }!!
	val chapter1IntroSequence = ActionSequence(
		name = "Chapter 1 intro",
		root = FixedActionNode(
			action = ActionShowChapterName(1, "A Fallen Star"),
			next = FixedActionNode(
				action = ActionPlayCutscene(cutscene = introCutscene),
				next = FixedActionNode(
					action = ActionToArea(dragonLairEntry, 5, 10),
					next = null,
				)
			),
		)
	)
	content.actions.global.add(chapter1IntroSequence)
	startChapter1.actions = CampaignActionsState(chapter1IntroSequence.root)
	generateUUIDs(chapter1IntroSequence)

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
