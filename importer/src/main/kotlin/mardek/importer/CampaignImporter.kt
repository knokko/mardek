package mardek.importer

import com.github.knokko.bitser.io.BitOutputStream
import com.github.knokko.bitser.serialize.Bitser
import mardek.assets.Campaign
import mardek.assets.area.Direction
import mardek.importer.area.importAreaAssets
import mardek.importer.characters.FatPlayableCharacter
import mardek.importer.characters.importPlayableCharacters
import mardek.importer.combat.importClasses
import mardek.importer.combat.importCombatAssets
import mardek.importer.inventory.importInventoryAssets
import mardek.importer.skills.importSkills
import mardek.importer.ui.importUiSprites
import mardek.state.ingame.CampaignState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.characters.CharacterSelectionState
import mardek.state.ingame.characters.CharacterState
import java.io.ByteArrayOutputStream

fun importDefaultCampaign(bitser: Bitser): Campaign {
	val combatAssets = importCombatAssets()
	val skillAssets = importSkills(combatAssets)
	val inventoryAssets = importInventoryAssets(combatAssets, skillAssets)
	importClasses(combatAssets, skillAssets, inventoryAssets)
	val areaAssets = importAreaAssets(inventoryAssets)
	val uiSprites = importUiSprites()

	val fatCharacters = importPlayableCharacters(combatAssets, skillAssets, inventoryAssets, areaAssets,)
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
		state.currentHealth = state.determineMaxHealth(fat.wrapped.baseStats, combatAssets.stats)
		state.currentMana = state.determineMaxMana(fat.wrapped.baseStats, combatAssets.stats)
		return state
	}

	val startChapter1 = CampaignState(
		currentArea = AreaState(areaAssets.areas.find { it.properties.rawName == "DL_entr" }!!, AreaPosition(5, 10)),
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

	val campaign = Campaign(
		combatAssets, skillAssets, inventoryAssets, areaAssets,
		ArrayList(fatCharacters.map { it.wrapped }), uiSprites
	)

	fun addCheckpoint(name: String, state: CampaignState) {
		val byteOutput = ByteArrayOutputStream()
		val bitOutput = BitOutputStream(byteOutput)
		bitser.serialize(state, bitOutput, campaign)
		bitOutput.finish()
		campaign.checkpoints[name] = byteOutput.toByteArray()
	}

	addCheckpoint("chapter1", startChapter1)

	return campaign
}
