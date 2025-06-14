package mardek.importer

import com.github.knokko.bitser.io.BitOutputStream
import com.github.knokko.bitser.serialize.Bitser
import mardek.content.Content
import mardek.content.animations.BattleModel
import mardek.content.area.Direction
import mardek.importer.area.importAreaContent
import mardek.importer.audio.importAudioContent
import mardek.importer.battle.importBattleContent
import mardek.importer.characters.FatPlayableCharacter
import mardek.importer.characters.importPlayableCharacters
import mardek.importer.stats.importClasses
import mardek.importer.stats.importStatsContent
import mardek.importer.inventory.importItemsContent
import mardek.importer.particle.importParticleEffects
import mardek.importer.skills.importSkillsContent
import mardek.importer.ui.importUiSprites
import mardek.state.ingame.CampaignState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.characters.CharacterSelectionState
import mardek.state.ingame.characters.CharacterState
import java.io.ByteArrayOutputStream

fun importVanillaContent(bitser: Bitser, skipMonsters: Boolean = false): Content {
	val content = Content()
	importAudioContent(content.audio)
	importParticleEffects(content)
	importStatsContent(content)
	importSkillsContent(content)
	importItemsContent(content)

	val playerModelMapping = if (skipMonsters) null else mutableMapOf<String, BattleModel>()
	importBattleContent(content, playerModelMapping)
	importClasses(content)
	importAreaContent(content)
	content.ui = importUiSprites()

	val fatCharacters = importPlayableCharacters(content, playerModelMapping)
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
		currentArea = AreaState(content.areas.areas.find { it.properties.rawName == "DL_entr" }!!, AreaPosition(5, 10)),
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
	startChapter1.currentArea!!.lastPlayerDirection = Direction.Up

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
