package mardek.state.ingame

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.action.ActionToArea
import mardek.content.action.FixedActionNode
import mardek.content.area.Chest
import mardek.content.characters.PlayableCharacter
import mardek.content.inventory.PlotItem
import mardek.input.InputKey
import mardek.input.InputKeyEvent
import mardek.input.MouseMoveEvent
import mardek.state.GameStateUpdateContext
import mardek.state.ingame.actions.ActivatedTriggers
import mardek.state.ingame.actions.AreaActionsState
import mardek.state.ingame.actions.CampaignActionsState
import mardek.state.ingame.area.AreaDiscoveryMap
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.area.IncomingRandomBattle
import mardek.state.ingame.area.loot.BattleLoot
import mardek.state.ingame.area.loot.ObtainedGold
import mardek.state.ingame.area.loot.ObtainedItemStack
import mardek.state.ingame.area.loot.generateBattleLoot
import mardek.content.battle.Battle
import mardek.state.ingame.battle.BattleStateMachine
import mardek.state.ingame.battle.BattleUpdateContext
import mardek.content.battle.Enemy
import mardek.state.ingame.characters.CharacterSelectionState
import mardek.state.ingame.characters.CharacterState
import mardek.state.saves.SaveFile
import mardek.state.saves.SaveSelectionState
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

	@BitField(id = 4, optional = true)
	var actions: CampaignActionsState? = null

	@BitField(id = 5)
	@ReferenceField(stable = true, label = "chests")
	val openedChests = HashSet<Chest>()

	@BitField(id = 6)
	@ReferenceField(stable = true, label = "plot items")
	val collectedPlotItems = HashSet<PlotItem>()

	@BitField(id = 7)
	val areaDiscovery = AreaDiscoveryMap()

	@BitField(id = 8)
	val triggers = ActivatedTriggers()

	/**
	 * - This variable is 0 at the start of the campaign, and is reset to 0 whenever a random battle is encountered.
	 * - This variable is increased by 1 whenever the player moves in an area with random battles
	 * - When this variable gets larger, the probability of encountering a random battle increases.
	 * - When this variable is too low, no random battle can be encountered.
	 */
	@BitField(id = 9)
	@IntegerField(expectUniform = false, minValue = 0)
	var stepsSinceLastBattle = 0

	@BitField(id = 10)
	@IntegerField(expectUniform = false, minValue = 0)
	var totalSteps = 0L

	@BitField(id = 11)
	@IntegerField(expectUniform = true, minValue = 0)
	var totalTime = 0.seconds

	constructor() : this(null, CharacterSelectionState(), HashMap(), 0)

	var shouldOpenMenu = false
	var gameOver = false

	fun update(context: UpdateContext) {
		while (true) {
			val event = context.input.consumeEvent() ?: break
			if (event is MouseMoveEvent) {
				this.currentArea?.activeBattle?.processMouseMove(event)
			}
			if (event !is InputKeyEvent || !event.didPress) continue

			if (event.key == InputKey.CheatSave) {
				context.saves.createSave(context.content, this, context.campaignName, SaveFile.Type.Cheat)
			}

			val campaignActions = getCampaignActions()
			if (campaignActions != null) {
				campaignActions.processKeyPress(event.key)
				continue
			}

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

			val areaActions = currentArea.actions
			if (areaActions != null) {
				updateAreaActions(context, event, areaActions)
				continue
			}

			if (event.key == InputKey.ToggleMenu) {
				shouldOpenMenu = true
				context.soundQueue.insert(context.content.audio.fixedEffects.ui.openMenu)
				continue
			}

			if (event.key == InputKey.CheatScrollUp || event.key == InputKey.CheatScrollDown) {
				val areas = context.content.areas.areas
				val currentIndex = areas.indexOf(currentArea.area)

				var nextIndex = currentIndex
				if (event.key == InputKey.CheatScrollUp) nextIndex -= 1
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

		val campaignActions = getCampaignActions()
		if (campaignActions != null) {
			campaignActions.update()
			return
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

		val areaActions = currentArea?.actions
		if (areaActions != null) {
			updateAreaActions(context, null, areaActions)
			if (areaActions.node == null) currentArea!!.finishActions()
		}

		currentArea?.let {
			val areaContext = AreaState.UpdateContext(
				context, characterSelection.party, characterStates,
				areaDiscovery, triggers, stepsSinceLastBattle, totalSteps
			)
			it.update(areaContext)
			this.stepsSinceLastBattle = areaContext.stepsSinceLastBattle
			this.totalSteps = areaContext.totalSteps
			if (it.actions != null) return
		}

		val destination = currentArea?.nextTransition
		if (destination != null) {
			val destinationArea = destination.area
			if (destinationArea != null) {
				currentArea = AreaState(
					destinationArea, AreaPosition(destination.x, destination.y),
					destination.direction ?: currentArea!!.getPlayerDirection(0),
				)
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
							area.randomBattles!!.defaultBackground,
							false,
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
				// TODO CHAP3 dreamstone in chest
			}
		}
	}

	private fun getCampaignActions(): CampaignActionsState? {
		val node = this.actions?.node
		if (currentArea != null && node != null) throw IllegalStateException(
			"Current area ${currentArea?.area} must be null when actions $actions is non-null"
		)

		if (node is FixedActionNode) {
			val action = node.action
			if (action is ActionToArea) {
				this.currentArea = AreaState(action.area, AreaPosition(action.x, action.y))
				this.actions = null
			}
		}

		return this.actions
	}

	fun getParty() = characterSelection.party.filterNotNull().map {
		Pair(it, characterStates[it]!!)
	}

	fun clampHealthAndMana() {
		for ((playableCharacter, state) in characterStates) {
			val maxHealth = state.determineMaxHealth(playableCharacter.baseStats, state.activeStatusEffects)
			state.currentHealth = state.currentHealth.coerceIn(1, maxHealth)
			val maxMana = state.determineMaxMana(playableCharacter.baseStats, state.activeStatusEffects)
			state.currentMana = state.currentMana.coerceIn(0, maxMana)
		}
	}

	/**
	 * Restores all HP and MP of all party members, and removes all their status effects.
	 */
	fun healParty() {
		for ((player, playerState) in getParty()) {
			playerState.activeStatusEffects.clear()
			playerState.currentHealth = playerState.determineMaxHealth(player.baseStats, emptySet())
			playerState.currentMana = playerState.determineMaxMana(player.baseStats, emptySet())
		}
	}

	/**
	 * This method should be called when the player enters the campaign state/session, so either:
	 * - when the player starts a new game, or
	 * - when the player loads a saved game
	 *
	 * This method will reset some animation states.
	 */
	fun markSessionStart() {
		this.actions?.markSessionStart()
		this.currentArea?.activeBattle?.markSessionStart()
	}

	private fun updateAreaActions(context: UpdateContext, event: InputKeyEvent?, actions: AreaActionsState) {
		val saveSelection = actions.saveSelectionState
		if (saveSelection != null) {
			val saveContext = SaveSelectionState.UpdateContext(
				context.saves,
				context.content,
				context.soundQueue,
				true,
			)

			if (event != null) {
				val outcome = saveSelection.pressKey(saveContext, event.key)
				if (outcome.canceled) actions.finishSaveNode()
				var failed = false
				if (outcome.finished) {
					if (outcome.save == null || outcome.save.file.delete()) {
						if (context.saves.createSave(
								context.content, this,
								context.campaignName, SaveFile.Type.Crystal
							)) {
							context.soundQueue.insert(context.content.audio.fixedEffects.ui.clickConfirm)
							actions.finishSaveNode()
						} else failed = true
					} else {
						failed = true
					}
				}
				if (failed || outcome.failed) {
					context.soundQueue.insert(context.content.audio.fixedEffects.ui.clickReject)
					actions.retrySaveNode()
				}
			} else {
				saveSelection.update(saveContext)
			}
		} else if (event != null) {
			actions.processKeyEvent(event)
		} else {
			val currentArea = this.currentArea!!
			val actionsContext = AreaActionsState.UpdateContext(
				context.input, context.timeStep, context.soundQueue, context.campaignName,
				currentArea.characterStates, currentArea.fadingCharacters, this::healParty,
			)
			actions.update(actionsContext)

			val maybeBattle = actionsContext.startBattle
			if (maybeBattle != null) {
				val areaContext = AreaState.UpdateContext(
					context, characterSelection.party, characterStates, areaDiscovery,
					triggers, stepsSinceLastBattle, totalSteps,
				)
				if (maybeBattle.overridePlayers != null) {
					currentArea.engageBattle(areaContext, maybeBattle.battle, maybeBattle.overridePlayers!!)
				} else {
					currentArea.engageBattle(areaContext, maybeBattle.battle)
				}
			}
		}
	}

	class UpdateContext(
		parent: GameStateUpdateContext,
		val campaignName: String
	) : GameStateUpdateContext(parent)

	companion object {

		@Suppress("unused")
		@ReferenceField(stable = true, label = "playable characters")
		private val CHARACTER_STATES_KEY = false
	}
}
