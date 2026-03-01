package mardek.state.ingame.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.action.ActionTargetData
import mardek.content.action.WalkSpeed
import mardek.content.area.*
import mardek.content.area.objects.AreaCharacter
import mardek.content.characters.PlayableCharacter
import mardek.input.InputKey
import mardek.input.InputManager
import mardek.state.ingame.actions.AreaActionsState
import mardek.state.ingame.area.loot.ObtainedGold
import mardek.content.battle.Battle
import mardek.state.ingame.battle.BattleState
import mardek.state.ingame.battle.BattleUpdateContext
import mardek.content.battle.Enemy
import mardek.content.characters.CharacterState
import mardek.input.Event
import mardek.input.InputKeyEvent
import mardek.input.MouseMoveEvent
import mardek.state.ingame.CampaignState
import mardek.state.ingame.CampaignStateMachine
import mardek.state.ingame.area.loot.BattleLoot
import mardek.state.ingame.area.loot.ObtainedItemStack
import mardek.state.ingame.area.loot.generateBattleLoot
import mardek.state.ingame.battle.BattleStateMachine
import mardek.state.ingame.story.StoryState
import mardek.state.ingame.worldmap.AreaExitPoint
import mardek.state.ingame.worldmap.WorldMapState
import java.lang.Math.clamp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * This class captures the state when the player is currently inside an area, it tracks e.g.
 * - the position and rotation of the player
 * - the position and rotation of the NPCs
 * - whether the player is currently inside a dialogue
 *
 * This class is one of the classes that extends [CampaignStateMachine]. When the player is currently inside an area,
 * the [CampaignState.state] will be an instance of [AreaState].
 */
@BitStruct(backwardCompatible = true)
class AreaState(

	/**
	 * The area in which the player resides, which must be an element of [AreaContent.areas]
	 */
	@BitField(id = 0)
	@ReferenceField(stable = true, label = "areas")
	val area: Area,

	/**
	 * The story state when the player enters the area, which may affect the NPCs that will spawn.
	 *
	 * This should only be null in unit tests and in dummy constructors. When this is null, not a single NCP will spawn.
	 */
	story: StoryState?,

	/**
	 * When `story != null`, this is the context needed to evaluate its variables and expressions.
	 */
	expressionContext: StoryState.ExpressionContext?,

	/**
	 * The initial position of the player (where the player enters the area)
	 */
	initialPlayerPosition: AreaPosition,

	/**
	 * The initial rotation of the player
	 */
	initialPlayerDirection: Direction = Direction.Up,

	/**
	 * Whether the area fade-in should be skipped. This should only be `true` during unit tests.
	 */
	skipFadeIn: Boolean = false,
) : CampaignStateMachine() {

	/**
	 * The in-game time that elapsed since the player entered the area.
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = true)
	var currentTime = if (skipFadeIn) DOOR_OPEN_DURATION else ZERO
		internal set

	/**
	 * The current position of each of the party members. (`playerPositions[0]` is the position of Mardek,
	 * `playerPositions[1]` is the position of the second party member, etc...)
	 * This also tracks the position of the 'missing' party members (e.g. party member 3 when the party contains
	 * only Mardek and Deugan), but these 'missing' party members are simply not rendered.
	 *
	 * Note that while the player is walking to another tile, this array will not be updated until the players reach
	 * their destination tile. To check whether the player is currently walking to another tile, you need to check
	 * whether [suspension] is an instance of [AreaSuspensionPlayerWalking].
	 */
	@BitField(id = 2)
	@NestedFieldSetting(path = "", sizeField = IntegerField(expectUniform = true, minValue = 4, maxValue = 4))
	internal val playerPositions = Array(4) { initialPlayerPosition }

	/**
	 * The current direction/rotation of each of the party members
	 */
	@BitField(id = 3)
	@NestedFieldSetting(path = "", sizeField = IntegerField(expectUniform = true, minValue = 4, maxValue = 4))
	internal val playerDirections = Array(4) { initialPlayerDirection }

	/**
	 * The current state (position and rotation) of each NPC. When an NPC is mapped to `null` (or is not present in
	 * this map), it is currently not present in the area.
	 *
	 * Note that this map can be temporarily overridden by [AreaActionsState.overrideCharacterStates].
	 */
	@BitField(id = 5)
	@NestedFieldSetting(path = "k", fieldName = "CHARACTER_STATES_KEY_PROPERTIES")
	internal val characterStates = HashMap<AreaCharacter, AreaCharacterState>()

	/**
	 * This field tracks whether the area state is currently *suspended*. When the state is suspended, the player
	 * temporarily loses control of its party. For instance:
	 * - The state is suspended while a playable character is walking, which means that the player cannot move the
	 * characters again until they reach the next tile.
	 * - The state is suspended during dialogues, which means that the player cannot move the party during dialogues.
	 * (Note that characters will move automatically during some dialogues though.)
	 * - The state is also suspended during battles...
	 */
	@BitField(id = 6, optional = true)
	@ClassField(root = AreaSuspension::class)
	var suspension: AreaSuspension? = null

	/**
	 * The characters (typically bosses) that died very recently. A fading red 'aura' of it should be disabled a few
	 * seconds after some characters die.
	 */
	val fadingCharacters = mutableListOf<FadingCharacter>()

	private val rng = Random.Default

	/**
	 * When the player recently opened a chest containing only gold, that information will be stored in this field. The
	 * renderer uses this field to render the 'obtained gold' indicator after the player opens such a chest.
	 */
	var obtainedGold: ObtainedGold? = null

	/**
	 * The value of `currentTime` when the last battle was finished. This is used to render a small fade-in right after
	 * claiming the battle loot.
	 *
	 * When no battle was finished this session, it is a negative number, which should be ignored.
	 */
	var finishedBattleAt = -(1.seconds)

	private var shouldInteract = false

	@Suppress("unused")
	private constructor() : this(
		Area(), null, null,
		AreaPosition(), Direction.Up,
	)

	init {
		if (story != null && expressionContext != null) {
			for (character in area.objects.characters) {
				if (character.condition == null || story.evaluate(character.condition!!, expressionContext)) {
					var direction = character.startDirection
					if (direction == Direction.Random) direction = Direction.allProper().random()
					characterStates[character] = AreaCharacterState(
						x = character.startX,
						y = character.startY,
						direction = direction,
						next = null,
					)
				}
			}
		}
	}

	private fun processKeyEvent(context: UpdateContext, event: InputKeyEvent) {
		when (val suspension = this.suspension) {
			is AreaSuspensionBattle -> processBattleKeyEvent(context, suspension, event)
			is AreaSuspensionOpeningChest -> processChestKeyEvent(context, suspension, event)
			is AreaSuspensionActions -> suspension.actions.processKeyEvent(
				AreaActionsState.UpdateContext(context, this), event
			)
			null -> processUnsuspendedKeyEvent(context, event)
			else -> {}
		}
	}

	private fun processBattleKeyEvent(context: UpdateContext, suspension: AreaSuspensionBattle, event: InputKeyEvent) {
		if (!event.didPress) return
		val battleLoot = suspension.loot
		if (battleLoot != null) {
			val lootContext = BattleLoot.UpdateContext(
				context,
				context.campaign.usedPartyMembers(),
				context.campaign.allPartyMembers(),
			)
			battleLoot.processKeyPress(event.key, lootContext)
		} else {
			val battleContext = BattleUpdateContext(
				context.campaign.characterStates, context.content.audio.fixedEffects,
				context.content.stats.defaultWeaponElement, context.soundQueue
			)
			suspension.battle.processKeyPress(event.key, battleContext)
		}
	}

	private fun processChestKeyEvent(
		context: UpdateContext,
		suspension: AreaSuspensionOpeningChest,
		event: InputKeyEvent
	) {
		if (!event.didPress) return
		suspension.obtainedItem?.processKeyPress(
			event.key, context.content.audio.fixedEffects, context.soundQueue
		)
	}

	private fun processUnsuspendedKeyEvent(context: UpdateContext, event: InputKeyEvent) {
		if (!event.didPress) return
		val key = event.key

		if (key ==  InputKey.Interact) shouldInteract = true
		if (key == InputKey.ToggleMenu) {
			context.campaign.shouldOpenMenu = true
			context.soundQueue.insert(context.content.audio.fixedEffects.ui.openMenu)
		}

		if (key == InputKey.CheatScrollUp || key == InputKey.CheatScrollDown) {
			val areas = context.content.areas.areas
			val currentIndex = areas.indexOf(this.area)

			var nextIndex = currentIndex
			if (key == InputKey.CheatScrollUp) nextIndex -= 1
			else nextIndex += 1

			if (nextIndex < 0) nextIndex += areas.size
			if (nextIndex >= areas.size) nextIndex -= areas.size

			var nextPosition = this.getPlayerPosition(0)
			val nextArea = areas[nextIndex]
			if (nextPosition.x > 6 + nextArea.maxTileX || nextPosition.y > 4 + nextArea.maxTileY) {
				nextPosition = AreaPosition(3, 3)
			}
			context.campaign.state = AreaState(
				nextArea, context.campaign.story,
				context.campaign.expressionContext(),
				nextPosition
			)
		}
	}

	/**
	 * Updates this area state, which will, among others:
	 * - potentially move the player character
	 * - potentially move NPCs
	 * - trigger random battles
	 */
	private fun update(context: UpdateContext) {
		if (currentTime == ZERO && !area.flags.hasClearMap) {
			context.campaign.areaDiscovery.readWrite(area).discover(playerPositions[0].x, playerPositions[0].y)
		}

		obtainedGold?.run {
			if (currentTime >= showUntil) obtainedGold = null
		}

		while (true) {
			val suspension = this.suspension
			when (suspension) {
				is AreaSuspensionPlayerWalking -> updatePlayerPosition(context, suspension.destination)
				is AreaSuspensionIncomingRandomBattle -> updateIncomingRandomBattle(context, suspension)
				is AreaSuspensionIncomingBattle -> updateIncomingBattle(context, suspension)
				is AreaSuspensionBattle -> updateActiveBattle(context, suspension)
				is AreaSuspensionActions -> suspension.actions.update(
					AreaActionsState.UpdateContext(context, this)
				)
				is AreaSuspensionTransition -> updateTransition(context, suspension)
				is AreaSuspensionOpeningDoor -> updateOpeningDoor(suspension)
				is AreaSuspensionOpeningChest -> updateOpeningChest(context, suspension)
				null -> updateWithoutSuspension(context)
			}
			if (suspension === this.suspension) break
		}

		if (suspension !is AreaSuspensionBattle) updateNPCs(context)
		if (suspension?.shouldUpdateCurrentTime() != false) currentTime += context.timeStep
		shouldInteract = false
	}

	/**
	 * Starts a new battle, after displaying the 'incoming battle flash effect' for half a second.
	 */
	internal fun engageBattle(
		context: UpdateContext,
		battle: Battle,
		players: Array<PlayableCharacter?> = context.campaign.party,
	) {
		val oldSuspension = suspension
		val nextActions = if (oldSuspension is AreaSuspensionActions) oldSuspension.actions else null
		suspension = AreaSuspensionIncomingBattle(battle, currentTime + 500.milliseconds, players, nextActions)
	}

	private fun updateNPCs(context: UpdateContext) {
		val stateChanges = mutableMapOf<AreaCharacter, AreaCharacterState>()

		val rng = Random.Default
		for ((npc, state) in characterStates) {
			if (state.next != null) {
				if (state.next.arrivalTime <= currentTime) {
					stateChanges[npc] = AreaCharacterState(
						x = state.next.position.x,
						y = state.next.position.y,
						direction = Direction.exactDelta(
							state.next.position.x - state.x, state.next.position.y - state.y
						)!!,
						next = null,
					)
				}
				continue
			}

			if (npc.walkBehavior.movesPerSecond <= 0f) continue

			val updatesPerSecond = 1.seconds / context.timeStep
			val walkChance = npc.walkBehavior.movesPerSecond / updatesPerSecond
			if (rng.nextDouble() >= walkChance) continue

			val candidateDirections = Direction.allProper().toMutableList()
			while (candidateDirections.isNotEmpty() && !stateChanges.containsKey(npc)) {
				val direction = candidateDirections.removeAt(rng.nextInt(candidateDirections.size))
				val destination = AreaPosition(state.x + direction.deltaX, state.y + direction.deltaY)
				if (shouldAllowNpcMove(
						this, context.campaign.openedChests, npc, destination
				)) {
					val walkTime = WalkSpeed.Slow.duration
					val next = NextAreaPosition(
						destination, currentTime,
						currentTime + walkTime, null,
					)
					stateChanges[npc] = AreaCharacterState(state.x, state.y, direction, next)
				}
			}
		}

		for ((npc, newState) in stateChanges) {
			characterStates[npc] = newState
		}
	}

	private fun interact(context: UpdateContext) {
		var x = playerPositions[0].x + playerDirections[0].deltaX
		var y = playerPositions[0].y + playerDirections[0].deltaY

		for (trigger in area.objects.talkTriggers) {
			if (x != trigger.x || y != trigger.y) continue
			val condition = trigger.condition
			if (condition != null && !context.campaign.story.evaluate(
					condition, context.campaign.expressionContext()
			)) continue
			x = trigger.talkX
			y = trigger.talkY
		}

		for (chest in area.chests) {
			if (x == chest.x && y == chest.y) {
				suspension = AreaSuspensionOpeningChest(chest)
				return
			}
		}

		for (character in area.objects.characters) {
			val characterState = characterStates[character] ?: continue
			if (characterState.next != null || characterState.x != x || characterState.y != y) continue
			val rootAction = character.sharedActionSequence?.root ?: character.ownActions
			if (rootAction != null) {
				val newSuspension = AreaSuspensionActions(AreaActionsState(
					rootAction, ActionTargetData(
						character.name, character.element, character.portrait
					)
				))

				val overrideDirection = Direction.bestDelta(
					playerPositions[0].x - x, playerPositions[0].y - y
				)!!
				newSuspension.actions.overrideCharacterStates[character] = AreaCharacterState(
					x = x, y = y, direction = overrideDirection, next = null
				)
				suspension = newSuspension
				return
			} else {
				println("interact with $character")
			}
		}

		for (decoration in area.objects.decorations) {
			if (decoration.x != x || decoration.y != y) continue

			val rootAction = decoration.sharedActionSequence?.root ?: decoration.ownActions
			if (rootAction != null) {
				suspension = AreaSuspensionActions(AreaActionsState(
					rootAction, ActionTargetData(
						decoration.displayName ?: "ERROR", null, null
					)
				))
				return
			} else {
				println("interact with $decoration")
			}
		}

		for (door in area.objects.doors) {
			if (x == door.x && y == door.y) {
				suspension = if (context.campaign.story.evaluate(
						door.canOpen, context.campaign.expressionContext()
				)) {
					AreaSuspensionOpeningDoor(door, currentTime + DOOR_OPEN_DURATION)
				} else {
					AreaSuspensionActions(AreaActionsState(
						door.cannotOpenActions!!.root, ActionTargetData(
							door.displayName, null, null
						)
					))
				}
				return
			}
		}

		for (orb in area.objects.switchOrbs) {
			if (x == orb.x && y == orb.y) println("switch color " + orb.color)
		}
	}

	/**
	 * A public getter for [playerPositions]
	 */
	fun getPlayerPosition(index: Int) = playerPositions[index]

	/**
	 * A public getter for [playerDirections]
	 */
	fun getPlayerDirection(index: Int) = playerDirections[index]

	/**
	 * Gets the current state (position, rotation, etc...) of the given `AreaCharacter`, or `null` if the character is
	 * currently not present.
	 */
	fun getCharacterState(character: AreaCharacter): AreaCharacterState? {
		val suspension = this.suspension
		if (suspension is AreaSuspensionActions) {
			val overrideStates = suspension.actions.overrideCharacterStates
			if (overrideStates.containsKey(character)) return overrideStates[character]
		}
		return characterStates[character]
	}

	private fun findTransitions(x: Int, y: Int): TransitionDestination? {
		for (portal in area.objects.portals) {
			if (x == portal.x && y == portal.y) {
				val destination = portal.destination
				if (destination is AreaTransitionDestination &&
					destination.area.properties.dreamType != AreaDreamType.None
				) continue // TODO CHAP3 Handle this properly
				return destination
			}
		}

		for (transition in area.objects.transitions) {
			if (x == transition.x && y == transition.y) return transition.destination
		}

		return null
	}

	private fun updatePlayerPosition(context: UpdateContext, nextPlayerPosition: NextAreaPosition) {
		if (nextPlayerPosition.arrivalTime <= currentTime) {
			context.campaign.totalSteps += 1

			for (index in 1 until playerPositions.size) {
				playerPositions[index] = playerPositions[index - 1]
			}
			playerPositions[0] = nextPlayerPosition.position
			if (!area.flags.hasClearMap) {
				context.campaign.areaDiscovery.readWrite(area).discover(
					nextPlayerPosition.position.x, nextPlayerPosition.position.y
				)
			}

			if (nextPlayerPosition.transition != null) {
				suspension = AreaSuspensionTransition(nextPlayerPosition.transition)
				return
			} else suspension = null

			maybeStartRandomBattle(context)

			for (character in context.campaign.party) {
				if (character == null) continue
				val state = context.campaign.characterStates[character]!!
				for (effect in state.activeStatusEffects) {
					val walkDamage = effect.damageWhileWalking ?: continue
					if (context.campaign.totalSteps % walkDamage.period != 0L) continue

					val maxHealth = state.determineMaxHealth(character.baseStats, state.activeStatusEffects)
					state.currentHealth -= max(1, (walkDamage.hpFraction * maxHealth).roundToInt())
					state.currentHealth = clamp(state.currentHealth.toLong(), 1, maxHealth)
					state.lastWalkDamage = CharacterState.WalkDamage(walkDamage.blinkColor)
				}
			}
		}
	}

	private fun updateIncomingRandomBattle(context: UpdateContext, incoming: AreaSuspensionIncomingRandomBattle) {
		if (context.input.isPressed(InputKey.CheatMove)) {
			suspension = null
		} else if (incoming.canAvoid && context.input.isPressed(InputKey.Cancel)) {
			suspension = null
		} else if (currentTime >= incoming.startAt) {
			context.soundQueue.insert(context.content.audio.fixedEffects.battle.engage)
			engageBattle(context, incoming.battle)
		}
	}

	private fun updateIncomingBattle(context: UpdateContext, incomingBattle: AreaSuspensionIncomingBattle) {
		if (currentTime >= incomingBattle.startAt) {
			suspension = AreaSuspensionBattle(BattleState(
				battle = incomingBattle.battle,
				players = incomingBattle.players,
				playerLayout = context.content.battle.enemyPartyLayouts.find { it.name == "DEFAULT" }!!,
				context = BattleUpdateContext(
					context.campaign.characterStates,
					context.content.audio.fixedEffects,
					context.content.stats.defaultWeaponElement,
					context.soundQueue
				)
			), nextActions = incomingBattle.nextActions)
		}
	}

	private fun updateActiveBattle(context: UpdateContext, suspension: AreaSuspensionBattle) {
		val battleContext = BattleUpdateContext(
			context.campaign.characterStates, context.content.audio.fixedEffects,
			context.content.stats.defaultWeaponElement, context.soundQueue,
		)
		suspension.battle.update(battleContext)

		val battleState = suspension.battle.state
		if (battleState is BattleStateMachine.RanAway) {
			this.suspension = null
			context.soundQueue.insert(context.content.audio.fixedEffects.battle.flee)
			for (combatant in suspension.battle.allPlayers()) {
				combatant.transferStatusBack(battleContext)
			}
		}
		if (battleState is BattleStateMachine.GameOver && battleState.shouldGoToGameOverMenu()) {
			context.campaign.gameOver = true
		}
		if (suspension.loot == null && battleState is BattleStateMachine.Victory && battleState.shouldGoToLootMenu()) {
			val loot = generateBattleLoot(context.content, suspension.battle.battle, context.campaign.usedPartyMembers())
			// TODO CHAP2 Handle plot items via timeline transitions: loot.plotItems
			suspension.loot = loot
			for (combatant in suspension.battle.allPlayers()) {
				combatant.transferStatusBack(battleContext)
			}
		}

		val loot = suspension.loot
		if (loot != null && loot.finishAt != 0L && System.nanoTime() > loot.finishAt) {
			context.campaign.gold += loot.gold
			this.suspension = if (suspension.nextActions != null) {
				AreaSuspensionActions(suspension.nextActions)
			} else null
			this.finishedBattleAt = this.currentTime
		}
	}

	private fun updateTransition(context: UpdateContext, suspension: AreaSuspensionTransition) {
		when (val destination = suspension.destination) {
			is AreaTransitionDestination -> {
				context.campaign.state = AreaState(
					destination.area, context.campaign.story, context.campaign.expressionContext(),
					AreaPosition(destination.x, destination.y),
					destination.direction ?: this.getPlayerDirection(0),
				)
			}
			is WorldMapTransitionDestination -> {
				val exitPosition = this.getPlayerPosition(0)
				val exitPoint = AreaExitPoint(this.area, exitPosition.x, exitPosition.y)
				context.campaign.state = WorldMapState(
					destination.worldMap, destination.node, exitPoint,
				)
			}
		}
	}

	private fun updateOpeningDoor(opening: AreaSuspensionOpeningDoor) {
		if (currentTime >= opening.finishTime) suspension = AreaSuspensionTransition(opening.door.destination)
	}

	private fun updateOpeningChest(context: UpdateContext, suspension: AreaSuspensionOpeningChest) {
		if (suspension.obtainedItem == null) {
			val openedChest = suspension.chest
			if (!context.campaign.openedChests.contains(openedChest)) {
				context.soundQueue.insert(context.content.audio.fixedEffects.openChest)
				val chestBattle = openedChest.battle
				if (chestBattle != null) {
					this.suspension = AreaSuspensionIncomingRandomBattle(
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
						this.currentTime + 1.seconds, false
					)
					context.campaign.openedChests.add(openedChest)
					return
				}

				if (openedChest.stack != null) {
					suspension.obtainedItem = ObtainedItemStack(
						openedChest.stack!!, null,
						context.campaign.usedPartyMembers(),
						context.campaign.allPartyMembers(),
					) { didTake ->
						this.suspension = null
						context.soundQueue.insert(context.content.audio.fixedEffects.ui.clickCancel)
						if (didTake) context.campaign.openedChests.add(openedChest)
					}
				}
				if (openedChest.plotItem != null) {
					suspension.obtainedItem = ObtainedItemStack(
						null, openedChest.plotItem,
						context.campaign.usedPartyMembers(),
						context.campaign.allPartyMembers(),
					) { didTake ->
						this.suspension = null
						if (didTake) {
							// TODO CHAP2 Replace this code with timeline variables: chest should be 'opened' if and
							// only if its corresponding timeline is in its default state
//							collectedPlotItems.add(openedChest.plotItem!!)
							context.campaign.openedChests.add(openedChest)
						}
						context.soundQueue.insert(context.content.audio.fixedEffects.ui.clickCancel)
					}
				}
				if (openedChest.gold > 0) {
					context.campaign.openedChests.add(openedChest)
					this.obtainedGold = ObtainedGold(
						openedChest.x, openedChest.y, openedChest.gold,
						this.currentTime + 1.seconds
					)
					context.campaign.gold += openedChest.gold
				}
				if (suspension.obtainedItem == null) this.suspension = null
				// TODO CHAP3 dreamstone in chest
			} else this.suspension = null
		}
	}

	private fun updateWithoutSuspension(context: UpdateContext) {
		checkTriggers(context)
		if (suspension != null) return
		processMovementInput(context.input)
		if (suspension == null && shouldInteract) {
			interact(context)
			shouldInteract = false
		}
	}

	private fun maybeStartRandomBattle(context: UpdateContext) {
		val randomBattles = area.randomBattles ?: return
		if (
			randomBattles.chance > 0 && context.campaign.stepsSinceLastBattle > randomBattles.minSteps &&
			rng.nextInt(150 - min(149, context.campaign.stepsSinceLastBattle)) <= randomBattles.chance
		) {
			fun chooseLevel(range: LevelRange) = range.min + rng.nextInt(1 + range.max - range.min)

			val possibleEnemySelections = randomBattles.getEnemySelections()
			val selection = possibleEnemySelections[rng.nextInt(possibleEnemySelections.size)]
			val enemies = selection.enemies.map {
				if (it != null) Enemy(it, chooseLevel(randomBattles.getLevelRange())) else null
			}.toTypedArray()

			val battle = Battle(
				enemies, selection.enemyLayout, "battle", "VictoryFanfare",
				randomBattles.specialBackground ?: randomBattles.defaultBackground,
				canFlee = true, isRandom = true,
			)

			val averagePlayerLevel = context.campaign.party.filterNotNull().map {
				context.campaign.characterStates[it]!!.currentLevel
			}.average()
			val averageMonsterLevel = enemies.filterNotNull().map { it.level }.average()
			val canAvoid = averagePlayerLevel > 2 + averageMonsterLevel

			suspension = AreaSuspensionIncomingRandomBattle(battle, currentTime + 1.seconds, canAvoid)
			context.soundQueue.insert(context.content.audio.fixedEffects.battle.encounter)
			context.campaign.stepsSinceLastBattle = 0
		} else context.campaign.stepsSinceLastBattle += 1
	}

	private fun checkTriggers(context: UpdateContext) {
		for (trigger in area.objects.walkTriggers) {
			if (trigger.walkOn != true) continue
			if (trigger.x != playerPositions[0].x || trigger.y != playerPositions[0].y) continue
			if (trigger.condition != null && !context.campaign.story.evaluate(
					trigger.condition!!, context.campaign.expressionContext()
			)) continue
			if (!context.campaign.triggers.activateTrigger(trigger)) continue

			val triggerActions = trigger.actions
			if (triggerActions != null) {
				suspension = AreaSuspensionActions(AreaActionsState(
					triggerActions.root,
					ActionTargetData(trigger.name, null, null),
				))
			} else {
				println("Hit flash trigger ${trigger.flashCode}")
			}

			// Make sure it can't get overwritten by any other trigger
			return
		}
	}

	private fun processMouseMove(event: MouseMoveEvent, context: UpdateContext) {
		when (val suspension = this.suspension) {
			is AreaSuspensionBattle -> suspension.battle.processMouseMove(event)
			is AreaSuspensionActions -> suspension.actions.processMouseMove(
				AreaActionsState.UpdateContext(context, this), event
			)
			else -> {}
		}
	}

	private fun processMovementInput(input: InputManager) {
		val lastPressed = input.mostRecentlyPressed(arrayOf(
			InputKey.MoveLeft, InputKey.MoveDown, InputKey.MoveRight, InputKey.MoveUp
		))
		val moveDirection = when (lastPressed) {
			InputKey.MoveLeft -> Direction.Left
			InputKey.MoveDown -> Direction.Down
			InputKey.MoveRight -> Direction.Right
			InputKey.MoveUp -> Direction.Up
			else -> null
		}

		if (moveDirection != null) {
			val nextX = playerPositions[0].x + moveDirection.deltaX
			val nextY = playerPositions[0].y + moveDirection.deltaY
			if (canPlayerWalkTo(input, nextX, nextY)) {
				val next = NextAreaPosition(
					AreaPosition(nextX, nextY),
					currentTime,
					currentTime + 0.2.seconds,
					findTransitions(nextX, nextY),
				)
				suspension = AreaSuspensionPlayerWalking(next)
				for (index in 1 until playerPositions.size) {
					val walkDirection = Direction.exactDelta(
						playerPositions[index - 1].x - playerPositions[index].x,
						playerPositions[index - 1].y - playerPositions[index].y,
					)
					playerDirections[index] = walkDirection ?: playerDirections[index - 1]
				}
				playerDirections[0] = Direction.exactDelta(
					next.position.x - playerPositions[0].x,
					next.position.y - playerPositions[0].y
				)!!
			} else {
				playerDirections[0] = moveDirection
			}
		}
	}

	private fun canPlayerWalkTo(input: InputManager, x: Int, y: Int): Boolean {
		if (input.isPressed(InputKey.CheatMove)) return true
		return canWalkTo(x, y)
	}

	/**
	 * Checks whether the player can walk to the tile at `position`. This method checks whether the tile is walkable,
	 * and whether it is occupied by an NPC or other obstacle.
	 */
	fun canWalkTo(position: AreaPosition) = canWalkTo(position.x, position.y)

	/**
	 * Checks whether the player can walk to the tile at `(x, y)`. This method checks whether the tile is walkable,
	 * and whether it is occupied by an NPC or other obstacle.
	 */
	fun canWalkTo(x: Int, y: Int): Boolean {
		if (!area.canWalkOnTile(x, y)) return false

		for (character in area.objects.characters) {
			val characterState = characterStates[character] ?: continue
			val next = characterState.next
			if (next != null) {
				if (x == next.position.x && y == next.position.y) return false
			} else {
				if (x == characterState.x && y == characterState.y) return false
			}
		}

		for (decoration in area.objects.decorations) {
			if (!decoration.canWalkThrough && x == decoration.x && y == decoration.y) return false
		}

		// TODO CHAP3 Switch gates and platforms
		return true
	}

	override fun processEvent(
		event: Event,
		campaignContext: CampaignState.UpdateContext,
		campaign: CampaignState
	) {
		val context = UpdateContext(campaignContext, campaign)
		if (event is MouseMoveEvent) processMouseMove(event, context)
		if (event is InputKeyEvent) processKeyEvent(context, event)
	}

	override fun update(
		campaignContext: CampaignState.UpdateContext,
		campaign: CampaignState
	) {
		update(UpdateContext(campaignContext, campaign))
	}

	/**
	 * This class contains the 'parameters' that should be supplied to several methods of this class.
	 */
	internal open class UpdateContext(
		parent: CampaignState.UpdateContext,

		/**
		 * The campaign state (containing this `AreaState`)
		 */
		val campaign: CampaignState,
	) : CampaignState.UpdateContext(parent, parent.campaignName)

	companion object {

		/**
		 * The time it takes to open a door in an area
		 */
		val DOOR_OPEN_DURATION = 500.milliseconds

		@Suppress("unused")
		@ReferenceField(stable = true, label = "area characters")
		private val CHARACTER_STATES_KEY_PROPERTIES = false
	}
}
