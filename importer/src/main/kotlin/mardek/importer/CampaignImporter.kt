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
import mardek.state.ingame.CampaignState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.characters.CharacterSelectionState
import mardek.state.ingame.characters.CharacterState
import java.io.ByteArrayOutputStream
import java.io.File

fun importDefaultCampaign(bitser: Bitser): Campaign {
	val combatAssets = importCombatAssets()
	val skillAssets = importSkills(combatAssets, "mardek/importer/combat/skills.txt")
	val inventoryAssets = importInventoryAssets(combatAssets, skillAssets, "mardek/importer/inventory/data.txt")
	importClasses(combatAssets, skillAssets, inventoryAssets)
	val areaAssets = importAreaAssets(File("importer/src/main/resources/mardek/importer/area"))

	val fatCharacters = importPlayableCharacters(
		combatAssets, skillAssets, inventoryAssets, areaAssets, "mardek/importer/combat/monsters.txt"
	)
	val heroMardek = fatCharacters.find { it.wrapped.areaSprites.name == "mardek_hero" }!!
	val heroDeugan = fatCharacters.find { it.wrapped.areaSprites.name == "deugan_hero" }!!

	fun initState(fat: FatPlayableCharacter): CharacterState {
		val state = CharacterState()
		state.currentLevel = fat.initialLevel
		// TODO currentHealth and currentMana?
		for ((index, item) in fat.initialEquipment.withIndex()) state.equipment[index] = item
		for ((index, itemStack) in fat.initialItems.withIndex()) state.inventory[index] = itemStack
		for (skill in fat.initialMasteredSkills) {
			state.skillMastery[skill] = skill.masteryPoints
		}
		// TODO toggled skills
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
		)
	)
	startChapter1.currentArea!!.lastPlayerDirection = Direction.Up

	val campaign = Campaign(combatAssets, skillAssets, inventoryAssets, areaAssets, ArrayList(fatCharacters.map { it.wrapped }))

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
