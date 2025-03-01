package mardek.state.ingame

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import mardek.assets.Campaign
import mardek.assets.area.Chest
import mardek.assets.characters.PlayableCharacter
import mardek.assets.inventory.PlotItem
import mardek.input.InputKey
import mardek.input.InputKeyEvent
import mardek.input.InputManager
import mardek.state.SoundQueue
import mardek.state.ingame.area.AreaDiscoveryMap
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.area.loot.ObtainedGold
import mardek.state.ingame.area.loot.ObtainedItemStack
import mardek.state.ingame.characters.CharacterSelectionState
import mardek.state.ingame.characters.CharacterState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@BitStruct(backwardCompatible = true)
class CampaignState(

	@BitField(id = 0, optional = true)
	var currentArea: AreaState?,

	@BitField(id = 1)
	val characterSelection: CharacterSelectionState,

	@BitField(id = 2)
	@NestedFieldSetting(path = "k", fieldName = "CHARACTER_STATES_KEY")
	val characterStates: HashMap<PlayableCharacter, CharacterState>,

	@BitField(id = 3)
	@IntegerField(expectUniform = false, minValue = 0)
	var gold: Int,
) {

	@BitField(id = 4)
	@ReferenceField(stable = true, label = "chests")
	val openedChests = HashSet<Chest>()

	@BitField(id = 5)
	@ReferenceField(stable = true, label = "plot items")
	val collectedPlotItems = HashSet<PlotItem>()

	@BitField(id = 6)
	val areaDiscovery = AreaDiscoveryMap()

	constructor() : this(null, CharacterSelectionState(), HashMap(), 0)

	var shouldOpenMenu = false

	fun update(input: InputManager, timeStep: Duration, soundQueue: SoundQueue, assets: Campaign) {
		while (true) {
			val event = input.consumeEvent() ?: break
			if (event !is InputKeyEvent || !event.didPress) continue

			val currentArea = this.currentArea ?: continue
			val currentBattle = currentArea.activeBattle

			if (currentBattle != null) {
				currentBattle.processKeyPress(event.key)
				continue
			}

			val obtainedItemStack = currentArea.obtainedItemStack
			if (obtainedItemStack != null) {
				obtainedItemStack.processKeyPress(event.key, soundQueue)
				continue
			}

			if (event.key == InputKey.ToggleMenu) {
				shouldOpenMenu = true
				soundQueue.insert("menu-open")
				continue
			}

			if (event.key == InputKey.ScrollUp || event.key == InputKey.ScrollDown) {
				val currentIndex = assets.areas.areas.indexOf(currentArea.area)

				var nextIndex = currentIndex
				if (event.key == InputKey.ScrollUp) nextIndex -= 1
				else nextIndex += 1

				if (nextIndex < 0) nextIndex += assets.areas.areas.size
				if (nextIndex >= assets.areas.areas.size) nextIndex -= assets.areas.areas.size

				var nextPosition = currentArea.getPlayerPosition(0)
				val nextArea = assets.areas.areas[nextIndex]
				if (nextPosition.x > 5 + nextArea.width || nextPosition.y > 3 + nextArea.height) {
					nextPosition = AreaPosition(3, 3)
				}
				this.currentArea = AreaState(nextArea, nextPosition)
				continue
			}

			currentArea.processKeyPress(event.key)
		}

		// TODO Don't update currentArea during battles!!
		currentArea?.update(input, this, assets, timeStep)
		val destination = currentArea?.nextTransition
		if (destination != null) {
			val destinationArea = destination.area
			if (destinationArea != null) {
				currentArea = AreaState(destinationArea, AreaPosition(destination.x, destination.y))
			} else currentArea!!.nextTransition = null
		}

		val currentArea = currentArea
		val openedChest = currentArea?.openedChest
		if (openedChest != null) {
			currentArea.openedChest = null
			if (!openedChests.contains(openedChest)) {
				soundQueue.insert("open-chest")
				if (openedChest.battle != null) {
					// TODO chest battle
					return
				}

				if (openedChest.gold > 0) {
					openedChests.add(openedChest)
					currentArea.obtainedGold = ObtainedGold(
						openedChest.x, openedChest.y, openedChest.gold, currentArea.currentTime + 1.seconds
					)
					gold += openedChest.gold
				}
				if (openedChest.stack != null) {
					currentArea.obtainedItemStack = ObtainedItemStack(
						openedChest.stack!!, null, characterSelection.party, characterStates
					) { didTake ->
						currentArea.obtainedItemStack = null
						soundQueue.insert("click-cancel")
						if (didTake) openedChests.add(openedChest)
					}
				}
				if (openedChest.plotItem != null) {
					currentArea.obtainedItemStack = ObtainedItemStack(
						null, openedChest.plotItem, characterSelection.party, characterStates
					) { didTake ->
						currentArea.obtainedItemStack = null
						if (didTake) {
							collectedPlotItems.add(openedChest.plotItem!!)
							openedChests.add(openedChest)
						}
						soundQueue.insert("click-cancel")
					}
				}
				// TODO dreamstone in chest
			}
		}
	}

	companion object {

		@Suppress("unused")
		@ReferenceField(stable = true, label = "playable characters")
		private val CHARACTER_STATES_KEY = false
	}
}
