package mardek.state.ingame

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.area.Chest
import mardek.content.characters.PlayableCharacter
import mardek.content.inventory.PlotItem
import mardek.input.InputKey
import mardek.input.InputKeyEvent
import mardek.input.MouseMoveEvent
import mardek.state.GameStateUpdateContext
import mardek.state.ingame.area.AreaDiscoveryMap
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.area.IncomingRandomBattle
import mardek.state.ingame.area.loot.BattleLoot
import mardek.state.ingame.area.loot.ObtainedGold
import mardek.state.ingame.area.loot.ObtainedItemStack
import mardek.state.ingame.area.loot.generateBattleLoot
import mardek.state.ingame.battle.Battle
import mardek.state.ingame.battle.BattleStateMachine
import mardek.state.ingame.battle.BattleUpdateContext
import mardek.state.ingame.battle.Enemy
import mardek.state.ingame.characters.CharacterSelectionState
import mardek.state.ingame.characters.CharacterState
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

	/**
	 * - This variable is 0 at the start of the campaign, and is reset to 0 whenever a random battle is encountered.
	 * - This variable is increased by 1 whenever the player moves in an area with random battles
	 * - When this variable gets larger, the probability of encountering a random battle increases.
	 * - When this variable is too low, no random battle can be encountered.
	 */
	@BitField(id = 7)
	@IntegerField(expectUniform = false, minValue = 0)
	var stepsSinceLastBattle = 0

	@BitField(id = 8)
	@IntegerField(expectUniform = false, minValue = 0)
	var totalSteps = 0L

	constructor() : this(null, CharacterSelectionState(), HashMap(), 0)

	var shouldOpenMenu = false
	var gameOver = false

	fun update(context: GameStateUpdateContext) {
		while (true) {
			val event = context.input.consumeEvent() ?: break
			if (event is MouseMoveEvent) {
				this.currentArea?.activeBattle?.processMouseMove(event)
			}
			if (event !is InputKeyEvent || !event.didPress) continue

			val currentArea = this.currentArea ?: continue

			val battleLoot = currentArea.battleLoot
			if (battleLoot != null) {
				val lootContext = BattleLoot.UpdateContext(context, getParty())
				if (battleLoot.processKeyPress(event.key, lootContext)) {
					gold += battleLoot.gold
					currentArea.activeBattle = null
					currentArea.battleLoot = null
				}
				continue
			}

			val currentBattle = currentArea.activeBattle
			if (currentBattle != null) {
				val physicalElement = context.content.stats.elements.find { it.rawName == "NONE" }!!
				val context = BattleUpdateContext(
					characterStates, context.content.audio.fixedEffects,
					physicalElement, context.soundQueue
				)
				currentBattle.processKeyPress(event.key, context)
				continue
			}

			val obtainedItemStack = currentArea.obtainedItemStack
			if (obtainedItemStack != null) {
				obtainedItemStack.processKeyPress(
					event.key, context.content.audio.fixedEffects, context.soundQueue
				)
				continue
			}

			if (event.key == InputKey.ToggleMenu) {
				shouldOpenMenu = true
				context.soundQueue.insert(context.content.audio.fixedEffects.ui.openMenu)
				continue
			}

			if (event.key == InputKey.ScrollUp || event.key == InputKey.ScrollDown) {
				val areas = context.content.areas.areas
				val currentIndex = areas.indexOf(currentArea.area)

				var nextIndex = currentIndex
				if (event.key == InputKey.ScrollUp) nextIndex -= 1
				else nextIndex += 1

				if (nextIndex < 0) nextIndex += areas.size
				if (nextIndex >= areas.size) nextIndex -= areas.size

				var nextPosition = currentArea.getPlayerPosition(0)
				val nextArea = areas[nextIndex]
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
			val currentArea = this.currentArea!!
			val physicalElement = context.content.stats.elements.find { it.rawName == "NONE" }!!
			val battleContext = BattleUpdateContext(
				characterStates, context.content.audio.fixedEffects, physicalElement, context.soundQueue
			)
			activeBattle.update(battleContext)
			val battleState = activeBattle.state
			if (battleState is BattleStateMachine.RanAway) {
				currentArea.activeBattle = null
				context.soundQueue.insert(context.content.audio.fixedEffects.battle.flee)
				for (combatant in activeBattle.allPlayers()) {
					combatant.transferStatusBack(battleContext)
				}
			}
			if (battleState is BattleStateMachine.GameOver && battleState.shouldGoToGameOverMenu()) {
				gameOver = true
			}
			if (currentArea.battleLoot == null && battleState is BattleStateMachine.Victory && battleState.shouldGoToLootMenu()) {
				val loot = generateBattleLoot(context.content, activeBattle.battle, getParty())
				collectedPlotItems.addAll(loot.plotItems)
				currentArea.battleLoot = loot
				for (combatant in activeBattle.allPlayers()) {
					combatant.transferStatusBack(battleContext)
				}
			}
			return
		}

		currentArea?.let {
			val areaContext = AreaState.UpdateContext(
				context, characterSelection.party, characterStates,
				areaDiscovery, stepsSinceLastBattle, totalSteps
			)
			it.update(areaContext)
			this.stepsSinceLastBattle = areaContext.stepsSinceLastBattle
			this.totalSteps = areaContext.totalSteps
		}
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
				context.soundQueue.insert(context.content.audio.fixedEffects.openChest)
				val chestBattle = openedChest.battle
				if (chestBattle != null) {
					val area = currentArea.area
					currentArea.incomingRandomBattle = IncomingRandomBattle(
						Battle(
							chestBattle.monsters.map { if (it == null) null else Enemy(
								context.content.battle.monsters.find {
									candidate -> candidate.name == it.name1
								}!!,
								it.level
							) }.toTypedArray(),
							chestBattle.enemyLayout,
							chestBattle.specialMusic ?: "battle",
							area.randomBattles!!.defaultBackground
						),
						currentArea.currentTime + 1.seconds, false
					)
					openedChests.add(openedChest)
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
						context.soundQueue.insert(context.content.audio.fixedEffects.ui.clickCancel)
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
						context.soundQueue.insert(context.content.audio.fixedEffects.ui.clickCancel)
					}
				}
				// TODO dreamstone in chest
			}
		}
	}

	fun getParty() = characterSelection.party.filterNotNull().map {
		Pair(it, characterStates[it]!!)
	}

	companion object {

		@Suppress("unused")
		@ReferenceField(stable = true, label = "playable characters")
		private val CHARACTER_STATES_KEY = false
	}
}
