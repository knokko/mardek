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
import mardek.state.GameStateUpdateContext
import mardek.state.ingame.actions.ActivatedTriggers
import mardek.state.ingame.actions.AreaActionsState
import mardek.state.ingame.area.loot.ObtainedGold
import mardek.content.battle.Battle
import mardek.state.ingame.battle.BattleState
import mardek.state.ingame.battle.BattleUpdateContext
import mardek.content.battle.Enemy
import mardek.content.characters.CharacterState
import mardek.input.MouseMoveEvent
import mardek.state.ingame.CampaignStateMachine
import mardek.state.ingame.story.StoryState
import java.lang.Math.clamp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@BitStruct(backwardCompatible = true)
class AreaState(
	@BitField(id = 0)
	@ReferenceField(stable = true, label = "areas")
	val area: Area,

	story: StoryState?,

	initialPlayerPosition: AreaPosition,
	initialPlayerDirection: Direction = Direction.Up,

	skipFadeIn: Boolean = false,
) : CampaignStateMachine() {

	@BitField(id = 1)
	@IntegerField(expectUniform = true)
	var currentTime = if (skipFadeIn) DOOR_OPEN_DURATION else ZERO
		private set

	@BitField(id = 2)
	@NestedFieldSetting(path = "", sizeField = IntegerField(expectUniform = true, minValue = 4, maxValue = 4))
	internal val playerPositions = Array(4) { initialPlayerPosition }

	@BitField(id = 3)
	@NestedFieldSetting(path = "", sizeField = IntegerField(expectUniform = true, minValue = 4, maxValue = 4))
	internal val playerDirections = Array(4) { initialPlayerDirection }

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
	private constructor() : this(Area(), null, AreaPosition(), Direction.Up)

	init {
		if (story != null) {
			for (character in area.objects.characters) {
				if (character.condition == null || story.evaluate(character.condition!!)) {
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

	fun processKeyPress(key: InputKey) {
		if (key == InputKey.Interact) shouldInteract = true
	}

	/**
	 * Updates this area state, which will, among others:
	 * - potentially move the player character
	 * - potentially move NPCs
	 * - trigger random battles
	 */
	fun update(context: UpdateContext) {
		if (currentTime == ZERO && !area.flags.hasClearMap) {
			context.discovery.readWrite(area).discover(playerPositions[0].x, playerPositions[0].y)
		}

		obtainedGold?.run {
			if (currentTime >= showUntil) obtainedGold = null
		}

		when (val suspension = this.suspension) {
			is AreaSuspensionPlayerWalking -> updatePlayerPosition(context, suspension.destination)
			is AreaSuspensionIncomingRandomBattle -> updateIncomingRandomBattle(context, suspension)
			is AreaSuspensionIncomingBattle -> updateIncomingBattle(context, suspension)
			is AreaSuspensionBattle -> {} // handled by CampaignState
			is AreaSuspensionActions -> {} // handled by CampaignState
			is AreaSuspensionTransition -> {} // handled by CampaignState
			is AreaSuspensionOpeningDoor -> updateOpeningDoor(suspension)
			is AreaSuspensionOpeningChest -> {} // handled by CampaignState
			null -> {} // handled in the next line of code
		}

		if (suspension == null) updateWithoutSuspension(context)
		if (suspension !is AreaSuspensionBattle) updateNPCs(context)
		if (suspension?.shouldUpdateCurrentTime() != false) currentTime += context.timeStep
		shouldInteract = false
	}

	internal fun engageBattle(
		context: UpdateContext,
		battle: Battle,
		players: Array<PlayableCharacter?> = context.party,
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
				if (shouldAllowNpcMove(this, context.openedChests, npc, destination)) {
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
			if (condition != null && !context.story.evaluate(condition)) continue
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
				suspension = if (context.story.evaluate(door.canOpen)) {
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

		for (shop in area.objects.shops) {
			if (x == shop.x && y == shop.y) println("open shop $shop")
		}

		for (orb in area.objects.switchOrbs) {
			if (x == orb.x && y == orb.y) println("switch color " + orb.color)
		}
	}

	fun getPlayerPosition(index: Int) = playerPositions[index]

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
			context.totalSteps += 1

			for (index in 1 until playerPositions.size) {
				playerPositions[index] = playerPositions[index - 1]
			}
			playerPositions[0] = nextPlayerPosition.position
			if (!area.flags.hasClearMap) {
				context.discovery.readWrite(area).discover(
					nextPlayerPosition.position.x, nextPlayerPosition.position.y
				)
			}

			if (nextPlayerPosition.transition != null) {
				suspension = AreaSuspensionTransition(nextPlayerPosition.transition)
				return
			} else suspension = null

			maybeStartRandomBattle(context)

			for (character in context.party) {
				if (character == null) continue
				val state = context.characterStates[character]!!
				for (effect in state.activeStatusEffects) {
					val walkDamage = effect.damageWhileWalking ?: continue
					if (context.totalSteps % walkDamage.period != 0L) continue

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
					context.characterStates,
					context.content.audio.fixedEffects,
					context.content.stats.defaultWeaponElement,
					context.soundQueue
				)
			), nextActions = incomingBattle.nextActions)
		}
	}

	private fun updateOpeningDoor(opening: AreaSuspensionOpeningDoor) {
		if (currentTime >= opening.finishTime) suspension = AreaSuspensionTransition(opening.door.destination)
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
			randomBattles.chance > 0 && context.stepsSinceLastBattle > randomBattles.minSteps &&
			rng.nextInt(150 - min(149, context.stepsSinceLastBattle)) <= randomBattles.chance
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

			val averagePlayerLevel = context.party.filterNotNull().map {
				context.characterStates[it]!!.currentLevel
			}.average()
			val averageMonsterLevel = enemies.filterNotNull().map { it.level }.average()
			val canAvoid = averagePlayerLevel > 2 + averageMonsterLevel

			suspension = AreaSuspensionIncomingRandomBattle(battle, currentTime + 1.seconds, canAvoid)
			context.soundQueue.insert(context.content.audio.fixedEffects.battle.encounter)
			context.stepsSinceLastBattle = 0
		} else context.stepsSinceLastBattle += 1
	}

	private fun checkTriggers(context: UpdateContext) {
		for (trigger in area.objects.walkTriggers) {
			if (trigger.walkOn != true) continue
			if (trigger.x != playerPositions[0].x || trigger.y != playerPositions[0].y) continue
			if (trigger.condition != null && !context.story.evaluate(trigger.condition!!)) continue
			if (!context.triggers.activateTrigger(trigger)) continue

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

	internal fun processMouseMove(event: MouseMoveEvent) {
		when (val suspension = this.suspension) {
			is AreaSuspensionBattle -> suspension.battle.processMouseMove(event)
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

	fun canWalkTo(position: AreaPosition) = canWalkTo(position.x, position.y)

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

	companion object {

		/**
		 * The time it takes to open a door in an area
		 */
		val DOOR_OPEN_DURATION = 500.milliseconds

		@Suppress("unused")
		@ReferenceField(stable = true, label = "area characters")
		private val CHARACTER_STATES_KEY_PROPERTIES = false
	}

	/**
	 * Instances of this class are needed as parameter in [AreaState.update]
	 */
	class UpdateContext(
		parent: GameStateUpdateContext,
		val party: Array<PlayableCharacter?>,
		val characterStates: Map<PlayableCharacter, CharacterState>,
		val discovery: AreaDiscoveryMap,
		val triggers: ActivatedTriggers,
		val story: StoryState,
		val openedChests: Set<Chest>,
		var stepsSinceLastBattle: Int,
		var totalSteps: Long,
	) : GameStateUpdateContext(parent)
}
