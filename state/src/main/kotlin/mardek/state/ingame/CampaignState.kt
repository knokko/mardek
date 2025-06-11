package mardek.state.ingame

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.Content
import mardek.content.area.Chest
import mardek.content.characters.PlayableCharacter
import mardek.content.inventory.PlotItem
import mardek.input.InputKey
import mardek.input.InputKeyEvent
import mardek.input.InputManager
import mardek.state.SoundQueue
import mardek.state.ingame.area.AreaDiscoveryMap
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.area.loot.ObtainedGold
import mardek.state.ingame.area.loot.ObtainedItemStack
import mardek.state.ingame.battle.BattleStateMachine
import mardek.state.ingame.battle.BattleUpdateContext
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

	fun update(input: InputManager, timeStep: Duration, soundQueue: SoundQueue, content: Content) {
		while (true) {
			val event = input.consumeEvent() ?: break
			if (event !is InputKeyEvent || !event.didPress) continue

			val currentArea = this.currentArea ?: continue
			val currentBattle = currentArea.activeBattle

			if (currentBattle != null) {
				val physicalElement = content.stats.elements.find { it.rawName == "NONE" }!!
				val context = BattleUpdateContext(
					characterStates, content.audio.fixedEffects, physicalElement, soundQueue
				)
				currentBattle.processKeyPress(event.key, context)
				continue
			}

			val obtainedItemStack = currentArea.obtainedItemStack
			if (obtainedItemStack != null) {
				obtainedItemStack.processKeyPress(event.key, content.audio.fixedEffects, soundQueue)
				continue
			}

			if (event.key == InputKey.ToggleMenu) {
				shouldOpenMenu = true
				soundQueue.insert(content.audio.fixedEffects.ui.openMenu)
				continue
			}

			if (event.key == InputKey.ScrollUp || event.key == InputKey.ScrollDown) {
				val currentIndex = content.areas.areas.indexOf(currentArea.area)

				var nextIndex = currentIndex
				if (event.key == InputKey.ScrollUp) nextIndex -= 1
				else nextIndex += 1

				if (nextIndex < 0) nextIndex += content.areas.areas.size
				if (nextIndex >= content.areas.areas.size) nextIndex -= content.areas.areas.size

				var nextPosition = currentArea.getPlayerPosition(0)
				val nextArea = content.areas.areas[nextIndex]
				if (nextPosition.x > 5 + nextArea.width || nextPosition.y > 3 + nextArea.height) {
					nextPosition = AreaPosition(3, 3)
				}
				this.currentArea = AreaState(nextArea, nextPosition)
				continue
			}

			currentArea.processKeyPress(event.key)
		}

		// Don't update currentArea during battles!!
		val activeBattle = currentArea?.activeBattle
		if (activeBattle != null) {
			val physicalElement = content.stats.elements.find { it.rawName == "NONE" }!!
			val context = BattleUpdateContext(
				characterStates, content.audio.fixedEffects, physicalElement, soundQueue
			)
			activeBattle.update(context)
			val battleState = activeBattle.state
			if (battleState is BattleStateMachine.RanAway) {
				currentArea!!.activeBattle = null
				soundQueue.insert(content.audio.fixedEffects.battle.flee)
			}
			if (battleState is BattleStateMachine.GameOver && battleState.shouldGoToGameOverMenu()) {
				TODO("Game over")
			}
			if (battleState is BattleStateMachine.Victory && battleState.shouldGoToLootMenu()) {
				TODO("Open loot menu")
			}
			return
		}

		currentArea?.update(input, this, soundQueue, timeStep, content)
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
				soundQueue.insert(content.audio.fixedEffects.openChest)
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
						soundQueue.insert(content.audio.fixedEffects.ui.clickCancel)
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
						soundQueue.insert(content.audio.fixedEffects.ui.clickCancel)
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
