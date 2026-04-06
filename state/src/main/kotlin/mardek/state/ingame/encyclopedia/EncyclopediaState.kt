package mardek.state.ingame.encyclopedia

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.battle.Monster
import mardek.content.encyclopedia.EncyclopediaArea
import mardek.content.encyclopedia.EncyclopediaArtefact
import mardek.content.encyclopedia.EncyclopediaContent
import mardek.content.encyclopedia.EncyclopediaMonster
import mardek.content.encyclopedia.EncyclopediaPerson
import mardek.state.ingame.CampaignState

/**
 * The state of the encyclopedia: this tracks which people the player has met, which areas the player has visited,
 * etc...
 */
@BitStruct(backwardCompatible = true)
class EncyclopediaState {

	/**
	 * The people that the player has talked to, or otherwise learned/read about. These people are shown in the
	 * "People" section.
	 */
	@BitField(id = 0)
	@ReferenceField(stable = true, label = "encyclopedia people")
	val encounteredPeople = HashSet<EncyclopediaPerson>()

	/**
	 * The places that the player has visited. They are shown in the "Places" section.
	 */
	@BitField(id = 1)
	@ReferenceField(stable = true, label = "encyclopedia places")
	val discoveredPlaces = HashSet<EncyclopediaArea>()

	/**
	 * The artefacts that the player has acquired, or otherwise learned/read about. These artefacts are shown in the
	 * "Artefacts" section.
	 */
	@BitField(id = 2)
	@ReferenceField(stable = true, label = "encyclopedia artefacts")
	val discoveredArtefacts = HashSet<EncyclopediaArtefact>()

	@BitField(id = 3)
	@NestedFieldSetting(path = "k", fieldName = "SLAIN_MONSTERS_KEY_PROPERTIES")
	@NestedFieldSetting(path = "v", fieldName = "SLAIN_MONSTERS_VALUE_PROPERTIES")
	private val slainMonsters = HashMap<Monster, Int>()

	/**
	 * Reports [monster] as slain.
	 * - If the player hasn't slain the monster before, it will be added to the "Bestiary" section of the encyclopedia.
	 * - If the player has slain the monster before, the 'kill counter' will be incremented. This kill counter is shown
	 * in the "Bestiary" section on the right of the name of each monster that was slain at least once.
	 */
	fun reportMonsterAsSlain(monster: Monster) {
		slainMonsters[monster] = slainMonsters.getOrDefault(monster, 0) + 1
	}

	/**
	 * Creates an [EncyclopediaSnapshot] of the current encyclopedia state. This is used by the Encyclopedia tab of the
	 * in-game menu to determine which people/places/monsters it should show.
	 */
	fun createSnapshot(content: EncyclopediaContent, campaign: CampaignState) = EncyclopediaSnapshot(
		people = content.people.map {
			Pair(
				it,
				campaign.story.evaluate(it.chooseSnapshot, campaign.expressionContext()),
			)
		}.filter {
			it.second != null
		}.map { if (encounteredPeople.contains(it.first)) {
			EncyclopediaSnapshot.OptionalEntry(it.second, null, null)
		} else {
			EncyclopediaSnapshot.OptionalEntry<EncyclopediaPerson.Snapshot>(
				null, it.second!!.firstName.length, null
			)
		}}.toTypedArray(),

		places = content.places.filter {
			campaign.story.evaluate(it.shouldShowUp, campaign.expressionContext())
		}.map {
			if (discoveredPlaces.contains(it)) {
				EncyclopediaSnapshot.OptionalEntry(it, null, null)
			} else {
				EncyclopediaSnapshot.OptionalEntry<EncyclopediaArea>(
					null, it.name.length, null
				)
			}
		}.toTypedArray(),

		artefacts = content.artefacts.filter {
			campaign.story.evaluate(it.shouldShowUp, campaign.expressionContext())
		}.map {
			if (discoveredArtefacts.contains(it)) {
				EncyclopediaSnapshot.OptionalEntry(it, null, null)
			} else {
				EncyclopediaSnapshot.OptionalEntry<EncyclopediaArtefact>(
					null, it.name.length, null
				)
			}
		}.toTypedArray(),

		monsters = content.monsters.filter {
			campaign.story.evaluate(it.shouldShowUp, campaign.expressionContext())
		}.map { monsterEntry ->
			Pair(monsterEntry, monsterEntry.monsters.sumOf {
				slainMonsters.getOrDefault(it, 0)
			})
		}.map {
			if (it.second > 0) {
				EncyclopediaSnapshot.OptionalEntry(it.first, null, it.second)
			} else {
				EncyclopediaSnapshot.OptionalEntry<EncyclopediaMonster>(
					null, it.first.monsters[0].displayName.length, it.second
				)
			}
		}.toTypedArray(),
	)

	companion object {

		@Suppress("unused")
		@ReferenceField(stable = true, label = "monsters")
		private const val SLAIN_MONSTERS_KEY_PROPERTIES = false

		@Suppress("unused")
		@IntegerField(expectUniform = false, minValue = 1)
		private const val SLAIN_MONSTERS_VALUE_PROPERTIES = false
	}
}
