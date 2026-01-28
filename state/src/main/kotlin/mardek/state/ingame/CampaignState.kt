package mardek.state.ingame

import com.github.knokko.bitser.BitPostInit
import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.Bitser
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.io.BitInputStream
import com.github.knokko.bitser.options.WithParameter
import mardek.content.Content
import mardek.content.action.ActionPlayCutscene
import mardek.content.action.ActionToArea
import mardek.content.action.FixedActionNode
import mardek.content.area.Chest
import mardek.content.characters.PlayableCharacter
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
import mardek.state.ingame.area.loot.BattleLoot
import mardek.state.ingame.area.loot.ObtainedGold
import mardek.state.ingame.area.loot.ObtainedItemStack
import mardek.state.ingame.area.loot.generateBattleLoot
import mardek.content.battle.Battle
import mardek.state.ingame.battle.BattleStateMachine
import mardek.state.ingame.battle.BattleUpdateContext
import mardek.content.battle.Enemy
import mardek.content.characters.CharacterState
import mardek.content.inventory.ItemStack
import mardek.content.story.Timeline
import mardek.content.story.TimelineNode
import mardek.state.GameStateManager
import mardek.state.UsedPartyMember
import mardek.state.ingame.area.AreaSuspensionActions
import mardek.state.ingame.area.AreaSuspensionBattle
import mardek.state.ingame.area.AreaSuspensionIncomingRandomBattle
import mardek.state.ingame.area.AreaSuspensionOpeningChest
import mardek.state.ingame.area.AreaSuspensionTransition
import mardek.state.ingame.story.StoryState
import mardek.state.saves.SaveFile
import mardek.state.saves.SaveSelectionState
import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlin.time.Duration.Companion.seconds

@BitStruct(backwardCompatible = true)
class CampaignState : BitPostInit {

	/**
	 * The (primary) state (machine) of the campaign state. The campaign is always in one of the following states:
	 * - Inside an area
	 * - Outside an area, in a global action sequence
	 * - Outside an area, on the world map
	 */
	@BitField(id = 0)
	@ClassField(root = CampaignStateMachine::class)
	var state: CampaignStateMachine = CampaignActionsState(FixedActionNode())

	/**
	 * The characters currently in the party
	 */
	@BitField(id = 1)
	@NestedFieldSetting(path = "c", optional = true)
	@ReferenceField(stable = true, label = "playable characters")
	val party: Array<PlayableCharacter?> = arrayOf(null, null, null, null)

	@BitField(id = 2)
	@NestedFieldSetting(path = "k", fieldName = "CHARACTER_STATES_KEY")
	val characterStates = HashMap<PlayableCharacter, CharacterState>()

	@BitField(id = 3)
	@IntegerField(expectUniform = false, minValue = 0)
	var gold: Int = 0

	@BitField(id = 4)
	@ReferenceField(stable = true, label = "chests")
	val openedChests = HashSet<Chest>()

	/**
	 * The state of the timelines, from which many other states are derived. It determines among others which
	 * playable characters are available, and influences a lot of dialogue.
	 */
	@BitField(id = 5)
	val story = StoryState()

	@BitField(id = 6)
	val areaDiscovery = AreaDiscoveryMap()

	@BitField(id = 7)
	val triggers = ActivatedTriggers()

	/**
	 * - This variable is 0 at the start of the campaign, and is reset to 0 whenever a random battle is encountered.
	 * - This variable is increased by 1 whenever the player moves in an area with random battles
	 * - When this variable gets larger, the probability of encountering a random battle increases.
	 * - When this variable is too low, no random battle can be encountered.
	 */
	@BitField(id = 8)
	@IntegerField(expectUniform = false, minValue = 0)
	var stepsSinceLastBattle = 0

	@BitField(id = 9)
	@IntegerField(expectUniform = false, minValue = 0)
	var totalSteps = 0L

	@BitField(id = 10)
	@IntegerField(expectUniform = true, minValue = 0)
	var totalTime = 0.seconds

	/**
	 * The items that are currently in the item storage, which the player can access via a save crystal. This list
	 * starts empty, but will grow larger if needed.
	 */
	@BitField(id = 11)
	@NestedFieldSetting(path = "c", optional = true)
	val itemStorage = ArrayList<ItemStack?>()

	/**
	 * The item stack that is currently grabbed by the cursor (e.g. in the inventory tab).
	 */
	@BitField(id = 12, optional = true)
	var cursorItemStack: ItemStack? = null

	var shouldOpenMenu = false
	var gameOver = false

	override fun postInit(context: BitPostInit.Context) {
		val content = context.withParameters["content"] as Content
		story.validatePartyMembers(content, party, characterStates)
		for ((character, state) in characterStates) {
			state.initialize(character, itemStorage)
		}
	}

	fun update(context: UpdateContext) {
		while (true) {
			val event = context.input.consumeEvent() ?: break
			if (event is MouseMoveEvent) {
				when (val state = this.state) {
					is AreaState -> {
						state.processMouseMove(event)
						when (val suspension = state.suspension) {
							is AreaSuspensionActions -> {
								updateAreaActions(context, null, event, suspension.actions)
							}
							else -> {}
						}
					}
				}
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

			val currentArea = this.state
			if (currentArea !is AreaState) continue

			when (val suspension = currentArea.suspension) {
				is AreaSuspensionBattle -> {
					val battleLoot = suspension.loot
					if (battleLoot != null) {
						val lootContext = BattleLoot.UpdateContext(context, usedPartyMembers(), allPartyMembers())
						battleLoot.processKeyPress(event.key, lootContext)
						continue
					}

					val context = BattleUpdateContext(
						characterStates, context.content.audio.fixedEffects,
						context.content.stats.defaultWeaponElement, context.soundQueue
					)
					suspension.battle.processKeyPress(event.key, context)
				}

				is AreaSuspensionOpeningChest -> {
					suspension.obtainedItem?.processKeyPress(
						event.key, context.content.audio.fixedEffects, context.soundQueue
					)
				}

				is AreaSuspensionActions -> {
					updateAreaActions(context, event, null, suspension.actions)
				}

				else -> {
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
						if (nextPosition.x > 6 + nextArea.maxTileX || nextPosition.y > 4 + nextArea.maxTileY) {
							nextPosition = AreaPosition(3, 3)
						}
						this.state = AreaState(nextArea, nextPosition)
						continue
					}

					currentArea.processKeyPress(event.key)
				}
			}
		}

		val campaignActions = getCampaignActions()
		if (campaignActions != null) {
			campaignActions.update()
			return
		}

		val oldArea = this.state
		if (oldArea is AreaState) {
			when (val suspension = oldArea.suspension) {
				is AreaSuspensionBattle -> {
					val battleContext = BattleUpdateContext(
						characterStates, context.content.audio.fixedEffects,
						context.content.stats.defaultWeaponElement, context.soundQueue,
					)
					suspension.battle.update(battleContext)

					val battleState = suspension.battle.state
					if (battleState is BattleStateMachine.RanAway) {
						oldArea.suspension = null
						context.soundQueue.insert(context.content.audio.fixedEffects.battle.flee)
						for (combatant in suspension.battle.allPlayers()) {
							combatant.transferStatusBack(battleContext)
						}
					}
					if (battleState is BattleStateMachine.GameOver && battleState.shouldGoToGameOverMenu()) {
						gameOver = true
					}
					if (suspension.loot == null && battleState is BattleStateMachine.Victory && battleState.shouldGoToLootMenu()) {
						val loot = generateBattleLoot(context.content, suspension.battle.battle, usedPartyMembers())
						// TODO CHAP2 Handle plot items via timeline transitions: loot.plotItems
						suspension.loot = loot
						for (combatant in suspension.battle.allPlayers()) {
							combatant.transferStatusBack(battleContext)
						}
					}
					val loot = suspension.loot
					if (loot != null && loot.finishAt != 0L && System.nanoTime() > loot.finishAt) {
						gold += loot.gold
						oldArea.suspension = if (suspension.nextActions != null) {
							AreaSuspensionActions(suspension.nextActions)
						} else null
						oldArea.finishedBattleAt = oldArea.currentTime
					}
					return
				}

				is AreaSuspensionActions -> {
					updateAreaActions(context, null, null, suspension.actions)

					val newArea = this.state
					if (newArea is AreaState) {
						val newSuspension = newArea.suspension
						if (newSuspension is AreaSuspensionActions && newSuspension.actions.node == null) {
							newArea.suspension = null
						}
					}
				}

				else -> {}
			}
		}

		val areaState = this.state
		if (areaState is AreaState) {
			val areaContext = AreaState.UpdateContext(
				context, party, characterStates,
				areaDiscovery, triggers, story, stepsSinceLastBattle, totalSteps
			)
			areaState.update(areaContext)
			this.stepsSinceLastBattle = areaContext.stepsSinceLastBattle
			this.totalSteps = areaContext.totalSteps

			when (val suspension = areaState.suspension) {
				is AreaSuspensionActions -> {
					if (areaState !== oldArea) {
						updateAreaActions(context, null, null, suspension.actions)
						if (suspension.actions.node == null && areaState.suspension === suspension) {
							areaState.suspension = null
						}
					}
				}
				is AreaSuspensionTransition -> {
					val destination = suspension.destination
					val destinationArea = destination.area
					if (destinationArea != null) {
						this.state = AreaState(
							destinationArea, AreaPosition(destination.x, destination.y),
							destination.direction ?: (this.state as AreaState).getPlayerDirection(0),
						)
					} else areaState.suspension = null // TODO CHAP1 Support world map
				}
				is AreaSuspensionOpeningChest -> {
					if (suspension.obtainedItem == null) {
						val openedChest = suspension.chest
						if (!openedChests.contains(openedChest)) {
							context.soundQueue.insert(context.content.audio.fixedEffects.openChest)
							val chestBattle = openedChest.battle
							if (chestBattle != null) {
								val area = areaState.area
								areaState.suspension = AreaSuspensionIncomingRandomBattle(
									Battle(
										chestBattle.monsters.map {
											if (it == null) null else Enemy(
												context.content.battle.monsters.find { candidate ->
													candidate.name == it.name1
												}!!,
												it.level
											)
										}.toTypedArray(),
										chestBattle.enemyLayout,
										chestBattle.specialMusic ?: "battle",
										chestBattle.specialLootMusic ?: "VictoryFanfare",
										area.randomBattles!!.defaultBackground,
										canFlee = false,
										isRandom = false,
									),
									areaState.currentTime + 1.seconds, false
								)
								openedChests.add(openedChest)
								return
							}

							if (openedChest.stack != null) {
								suspension.obtainedItem = ObtainedItemStack(
									openedChest.stack!!, null, usedPartyMembers(), allPartyMembers()
								) { didTake ->
									areaState.suspension = null
									context.soundQueue.insert(context.content.audio.fixedEffects.ui.clickCancel)
									if (didTake) openedChests.add(openedChest)
								}
							}
							if (openedChest.plotItem != null) {
								suspension.obtainedItem = ObtainedItemStack(
									null, openedChest.plotItem, usedPartyMembers(), allPartyMembers()
								) { didTake ->
									areaState.suspension = null
									if (didTake) {
										// TODO CHAP2 Replace this code with timeline variables: chest should be 'opened' if and
										// only if its corresponding timeline is in its default state
//							collectedPlotItems.add(openedChest.plotItem!!)
										openedChests.add(openedChest)
									}
									context.soundQueue.insert(context.content.audio.fixedEffects.ui.clickCancel)
								}
							}
							if (openedChest.gold > 0) {
								openedChests.add(openedChest)
								areaState.obtainedGold = ObtainedGold(
									openedChest.x, openedChest.y, openedChest.gold,
									areaState.currentTime + 1.seconds
								)
								gold += openedChest.gold
							}
							if (suspension.obtainedItem == null) areaState.suspension = null
							// TODO CHAP3 dreamstone in chest
						} else areaState.suspension = null
					}
				}
				else -> {}
			}
		}
	}

	private fun getCampaignActions(): CampaignActionsState? {
		val currentState = this.state
		val node = if (currentState is CampaignActionsState) currentState.node else null

		if (node is FixedActionNode) {
			val action = node.action
			if (action is ActionToArea) {
				this.state = AreaState(
					action.area,
					AreaPosition(action.x, action.y),
					action.direction,
				)
				val nextNode = node.next
				if (nextNode != null) {
					(this.state as AreaState).suspension = AreaSuspensionActions(AreaActionsState(nextNode))
				}
			}
		}

		return when (val nextState = this.state) {
			is CampaignActionsState -> nextState
			else -> null
		}
	}

	/**
	 * Gets a list of party members that are currently used. Unlike `allPartyMembers`, this list does *not* include
	 * null members/empty party slots.
	 */
	fun usedPartyMembers() = party.withIndex().filter {
		it.value != null
	}.map { UsedPartyMember(it.index, it.value!!, characterStates[it.value!!]!!) }

	/**
	 * Gets an array of length 4, where the element at index `i` is `(playableCharacter, characterState)`, or `null`
	 * if the party member slot at index `i` is empty.
	 */
	fun allPartyMembers() = party.map {
		if (it == null) null else Pair(it, characterStates[it]!!)
	}.toTypedArray()

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
		for ((_, player, playerState) in usedPartyMembers()) {
			playerState.activeStatusEffects.clear()
			playerState.currentHealth = playerState.determineMaxHealth(player.baseStats, emptySet())
			playerState.currentMana = playerState.determineMaxMana(player.baseStats, emptySet())
		}
	}

	/**
	 * Transitions the current state/node of `timeline` to `newNode`, if `newNode` occurs later than the current
	 * state/node of `timeline`. If not, nothing happens.
	 */
	fun performTimelineTransition(context: UpdateContext, timeline: Timeline, newNode: TimelineNode) {
		story.transition(context.content, party, characterStates, timeline, newNode)
	}

	/**
	 * This method should be called when the player enters the campaign state/session, so either:
	 * - when the player starts a new game, or
	 * - when the player loads a saved game
	 *
	 * This method will reset some animation states.
	 */
	fun markSessionStart() {
		when (val currentState = this.state) {
			is CampaignActionsState -> currentState.markSessionStart()
			is AreaState -> {
				when (val suspension = currentState.suspension) {
					is AreaSuspensionBattle -> suspension.battle.markSessionStart()
					else -> {}
				}
			}
		}
	}

	private fun updateAreaActions(
		context: UpdateContext, event: InputKeyEvent?, mouseEvent: MouseMoveEvent?, actions: AreaActionsState
	) {
		val currentArea = this.state as AreaState
		val actionsContext = AreaActionsState.UpdateContext(
			context.input, context.timeStep, context.soundQueue, context.content.audio.fixedEffects,
			context.campaignName, currentArea.playerPositions,
			currentArea.playerDirections, currentArea.currentTime, party, currentArea.characterStates,
			characterStates, currentArea.fadingCharacters,
			story, itemStorage, this::healParty,
			{ timeline, newNode -> performTimelineTransition(context, timeline, newNode) },
			{ cursorItemStack }, { newStack -> this.cursorItemStack = newStack },
		)

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
			actions.processKeyEvent(actionsContext, event)
		} else if (mouseEvent != null) {
			actions.processMouseMove(actionsContext, mouseEvent)
		} else {
			actions.update(actionsContext)

			val switchArea = actionsContext.switchArea
			if (switchArea != null) {
				this.state = AreaState(
					switchArea.area, AreaPosition(switchArea.x, switchArea.y),
					switchArea.direction,
				)
				val nextNode = (actions.node as FixedActionNode).next
				if (nextNode != null) {
					(this.state as AreaState).suspension = AreaSuspensionActions(AreaActionsState(nextNode))
				}
			}

			val maybeBattle = actionsContext.startBattle
			if (maybeBattle != null) {
				val areaContext = AreaState.UpdateContext(
					context, party, characterStates, areaDiscovery,
					triggers, story, stepsSinceLastBattle, totalSteps,
				)
				if (maybeBattle.overridePlayers != null) {
					currentArea.engageBattle(areaContext, maybeBattle.battle, maybeBattle.overridePlayers!!)
				} else {
					currentArea.engageBattle(areaContext, maybeBattle.battle)
				}
			}

			val maybeMoney = actionsContext.setMoney
			if (maybeMoney != null) this.gold = maybeMoney
		}
	}

	/**
	 * Determines the name of the music track that the `AudioUpdater` should play in the current state. This is
	 * often the background music of the current area, but not always.
	 */
	fun determineMusicTrack(content: Content): String? {
		val areaState = this.state
		if (areaState is AreaState) {
			val suspension = areaState.suspension
			if (suspension is AreaSuspensionBattle) {
				val battleState = suspension.battle
				if (
					!battleState.battle.isRandom ||
							story.evaluate(content.story.fixedVariables.blockRandomBattleMusic) == null
				) {
					return if (battleState.state is BattleStateMachine.Victory) battleState.battle.lootMusic
					else battleState.battle.music
				}
			}

			return story.evaluate(areaState.area.properties.musicTrack)
		}

		val campaignActionNode = if (areaState is CampaignActionsState) areaState.node else null
		if (campaignActionNode is FixedActionNode) {
			val action = campaignActionNode.action
			if (action is ActionPlayCutscene) return action.cutscene.get().musicTrack
		}
		return null
	}

	class UpdateContext(
		parent: GameStateUpdateContext,
		val campaignName: String
	) : GameStateUpdateContext(parent)

	companion object {

		@Suppress("unused")
		@ReferenceField(stable = true, label = "playable characters")
		private val CHARACTER_STATES_KEY = false

		/**
		 * Loads the campaign state from a save file that is being read through `input`.
		 */
		fun loadSave(content: Content, input: InputStream): CampaignState {
			val bitInput = BitInputStream(input)
			val campaignState = GameStateManager.bitser.deserialize(
				CampaignState::class.java, bitInput,
				content,
				Bitser.BACKWARD_COMPATIBLE,
				WithParameter("content", content),
			)
			bitInput.close()

			return campaignState
		}

		/**
		 * Loads the [Content.checkpoints] with the given `name`
		 */
		fun loadCheckpoint(content: Content, name: String): CampaignState {
			val rawCheckpoint = content.checkpoints[name] ?: throw IllegalArgumentException(
				"Missing checkpoint $name: options are ${content.checkpoints}"
			)
			return loadSave(content, ByteArrayInputStream(rawCheckpoint))
		}

		/**
		 * Loads the initial state of the given `chapter`, e.g. `loadChapter(content, 2)` would return the
		 * campaign state that represents the start of chapter 2.
		 */
		fun loadChapter(content: Content, chapter: Int) = loadCheckpoint(content, "chapter$chapter")
	}
}
