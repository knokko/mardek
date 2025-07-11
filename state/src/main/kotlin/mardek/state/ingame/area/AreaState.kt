package mardek.state.ingame.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.ReferenceFieldTarget
import mardek.content.area.*
import mardek.content.characters.PlayableCharacter
import mardek.input.InputKey
import mardek.input.InputManager
import mardek.state.GameStateUpdateContext
import mardek.state.ingame.area.loot.BattleLoot
import mardek.state.ingame.area.loot.ObtainedGold
import mardek.state.ingame.area.loot.ObtainedItemStack
import mardek.state.ingame.battle.Battle
import mardek.state.ingame.battle.BattleState
import mardek.state.ingame.battle.BattleUpdateContext
import mardek.state.ingame.battle.Enemy
import mardek.state.ingame.characters.CharacterState
import java.lang.Math.clamp
import kotlin.math.max
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
	initialPlayerPosition: AreaPosition
) {

	@BitField(id = 1)
	@IntegerField(expectUniform = true)
	var currentTime = ZERO
		private set

	@BitField(id = 2)
	@NestedFieldSetting(path = "", sizeField = IntegerField(expectUniform = true, minValue = 4, maxValue = 4))
	private val playerPositions = Array(4) { initialPlayerPosition }

	@BitField(id = 3)
	var lastPlayerDirection = Direction.Down

	@BitField(id = 4, optional = true)
	var nextPlayerPosition: NextAreaPosition? = null
		private set

	@BitField(id = 5, optional = true)
	var incomingRandomBattle: IncomingRandomBattle? = null

	@BitField(id = 6, optional = true)
	@ReferenceFieldTarget(label = "battle state")
	var activeBattle: BattleState? = null

	@BitField(id = 7, optional = true)
	var battleLoot: BattleLoot? = null

	private val rng = Random.Default

	var nextTransition: TransitionDestination? = null

	var openedChest: Chest? = null

	var openingDoor: OpeningDoor? = null
		private set

	var obtainedGold: ObtainedGold? = null

	var obtainedItemStack: ObtainedItemStack? = null

	private var shouldInteract = false

	@Suppress("unused")
	private constructor() : this(Area(), AreaPosition())

	fun processKeyPress(key: InputKey) {
		if (key == InputKey.Interact) shouldInteract = true
	}

	fun update(context: UpdateContext) {
		if (obtainedItemStack != null) return
		if (currentTime == ZERO && !area.flags.hasClearMap) {
			context.discovery.readWrite(area).discover(playerPositions[0].x, playerPositions[0].y)
		}

		updatePlayerPosition(context)
		processInput(context.input)
		if (shouldInteract) {
			interact()
			shouldInteract = false
		}

		val openingDoor = this.openingDoor
		if (openingDoor != null && currentTime >= openingDoor.finishTime) {
			nextTransition = openingDoor.door.destination
			this.openingDoor = null
		}
		if (obtainedGold != null && currentTime >= obtainedGold!!.showUntil) {
			this.obtainedGold = null
		}

		if (incomingRandomBattle != null && currentTime >= incomingRandomBattle!!.startAt) {
			val physicalElement = context.content.stats.elements.find { it.rawName == "NONE" }!!
			context.soundQueue.insert(context.content.audio.fixedEffects.battle.engage)
			activeBattle = BattleState(
				battle = incomingRandomBattle!!.battle,
				players = context.party,
				playerLayout = context.content.battle.enemyPartyLayouts.find { it.name == "DEFAULT" }!!,
				context = BattleUpdateContext(
					context.characterStates,
					context.content.audio.fixedEffects,
					physicalElement,
					context.soundQueue
				)
			)
			incomingRandomBattle = null
		}

		currentTime += context.timeStep
	}

	private fun interact() {
		if (nextTransition != null || nextPlayerPosition != null || openingDoor != null) return

		val x = playerPositions[0].x + lastPlayerDirection.deltaX
		val y = playerPositions[0].y + lastPlayerDirection.deltaY

		for (chest in area.chests) {
			if (x == chest.x && y == chest.y) {
				openedChest = chest
				return
			}
		}

		for (character in area.objects.characters) {
			if (x == character.startX && y == character.startY) {
				println("interact with $character")
			}
		}

		for (door in area.objects.doors) {
			if (x == door.x && y == door.y) {
				openingDoor = OpeningDoor(door, currentTime + DOOR_OPEN_DURATION)
				return
			}
		}

		for (areaObject in area.objects.objects) {
			if (x == areaObject.x && y == areaObject.y) {
				println("interact with $areaObject")
			}
		}

		for (shop in area.objects.shops) {
			if (x == shop.x && y == shop.y) println("open shop $shop")
		}

		for (orb in area.objects.switchOrbs) {
			if (x == orb.x && y == orb.y) println("switch color " + orb.color)
		}

		for (trigger in area.objects.talkTriggers) {
			if (x == trigger.x && y == trigger.y) println("trigger $trigger")
		}
	}

	fun getPlayerPosition(index: Int) = playerPositions[index]

	private fun checkTransitions() {
		for (portal in area.objects.portals) {
			if (playerPositions[0].x == portal.x && playerPositions[0].y == portal.y) {
				nextTransition = portal.destination
			}
		}

		for (transition in area.objects.transitions) {
			if (playerPositions[0].x == transition.x && playerPositions[0].y == transition.y) {
				nextTransition = transition.destination
			}
		}
	}

	private fun updatePlayerPosition(context: UpdateContext) {
		val nextPlayerPosition = this.nextPlayerPosition
		if (nextPlayerPosition != null && nextPlayerPosition.arrivalTime <= currentTime) {
			context.totalSteps += 1
			for (index in 1 until playerPositions.size) {
				playerPositions[index] = playerPositions[index - 1]
			}
			lastPlayerDirection = Direction.delta(
				nextPlayerPosition.position.x - playerPositions[0].x,
				nextPlayerPosition.position.y - playerPositions[0].y
			)!!
			playerPositions[0] = nextPlayerPosition.position
			this.nextPlayerPosition = null
			if (!area.flags.hasClearMap) {
				context.discovery.readWrite(area).discover(
					nextPlayerPosition.position.x, nextPlayerPosition.position.y
				)
			}
			checkTransitions()
			if (nextTransition != null) return

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

	private fun maybeStartRandomBattle(context: UpdateContext) {
		val randomBattles = area.randomBattles ?: return
		if (
			randomBattles.chance > 0 && context.stepsSinceLastBattle > randomBattles.minSteps &&
			rng.nextInt(150 - context.stepsSinceLastBattle) <= randomBattles.chance
		) {
			fun chooseLevel(range: LevelRange) = range.min + rng.nextInt(1 + range.max - range.min)

			val possibleEnemySelections = randomBattles.getEnemySelections()
			val selection = possibleEnemySelections[rng.nextInt(possibleEnemySelections.size)]
			val enemies = selection.enemies.map {
				if (it != null) Enemy(it, chooseLevel(randomBattles.getLevelRange())) else null
			}.toTypedArray()

			val battle = Battle(
				enemies, selection.enemyLayout, "battle",
				randomBattles.specialBackground ?: randomBattles.defaultBackground
			)

			val averagePlayerLevel = context.party.filterNotNull().map {
				context.characterStates[it]!!.currentLevel
			}.average()
			val averageMonsterLevel = enemies.filterNotNull().map { it.level }.average()
			val canAvoid = averagePlayerLevel > 2 + averageMonsterLevel

			incomingRandomBattle = IncomingRandomBattle(battle, currentTime + 1.seconds, canAvoid)
			context.soundQueue.insert(context.content.audio.fixedEffects.battle.encounter)
			context.stepsSinceLastBattle = 0
		} else context.stepsSinceLastBattle += 1
	}

	private fun processInput(input: InputManager) {
		if (nextPlayerPosition == null && incomingRandomBattle == null) {
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
				if (canWalkTo(input, nextX, nextY)) {
					nextPlayerPosition = NextAreaPosition(
						AreaPosition(nextX, nextY), currentTime, currentTime + 0.2.seconds
					)
				} else {
					lastPlayerDirection = moveDirection
				}
			}
		}

		if (incomingRandomBattle != null) {
			if (input.isPressed(InputKey.Cheat)) incomingRandomBattle = null
			else if (incomingRandomBattle!!.canAvoid && input.isPressed(InputKey.Cancel)) incomingRandomBattle = null
		}
	}

	private fun canWalkTo(input: InputManager, x: Int, y: Int): Boolean {
		if (x < 0 || y < 0) return false
		if (input.isPressed(InputKey.Cheat)) return true
		if (!area.canWalkOnTile(x, y)) return false

		// TODO Movable characters
		for (character in area.objects.characters) {
			if (x == character.startX && y == character.startY) return false
		}

		for (areaObject in area.objects.objects) {
			if (x == areaObject.x && y == areaObject.y) return false
		}

		// TODO Switch gates and platforms
		return true
	}

	companion object {
		val DOOR_OPEN_DURATION = 500.milliseconds
	}

	class UpdateContext(
		parent: GameStateUpdateContext,
		val party: Array<PlayableCharacter?>,
		val characterStates: Map<PlayableCharacter, CharacterState>,
		val discovery: AreaDiscoveryMap,
		var stepsSinceLastBattle: Int,
		var totalSteps: Long,
	) : GameStateUpdateContext(parent)
}
