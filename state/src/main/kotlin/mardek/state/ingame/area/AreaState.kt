package mardek.state.ingame.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.CollectionField
import com.github.knokko.bitser.field.IntegerField
import mardek.assets.area.Direction
import mardek.assets.area.OptimizedArea
import mardek.assets.area.TransitionDestination
import mardek.input.InputKey
import mardek.input.InputManager
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@BitStruct(backwardCompatible = false)
class AreaState(
	@BitField(ordering = 0)
	val area: OptimizedArea, // TODO Find a way to serialize this
	initialPlayerPosition: AreaPosition
) {

	@BitField(ordering = 1)
	@IntegerField(expectUniform = true)
	var currentTime = Duration.ZERO
		private set

	@BitField(ordering = 2)
	@CollectionField(size = IntegerField(expectUniform = true, minValue = 4, maxValue = 4))
	private val playerPositions = Array(4) { initialPlayerPosition }

	@BitField(ordering = 3)
	var lastPlayerDirection = Direction.Down

	@BitField(ordering = 4, optional = true)
	var nextPlayerPosition: NextAreaPosition? = null
		private set

	var nextTransition: TransitionDestination? = null

	var openingDoor: OpeningDoor? = null
		private set

	fun update(input: InputManager, timeStep: Duration, shouldInteract: Boolean) {
		updatePlayerPosition()
		processInput(input)
		if (shouldInteract) interact()

		val openingDoor = this.openingDoor
		if (openingDoor != null && currentTime >= openingDoor.finishTime) {
			nextTransition = openingDoor.door.destination
			this.openingDoor = null
		}
		currentTime += timeStep
	}

	private fun interact() {
		if (nextTransition != null || nextPlayerPosition != null || openingDoor != null) return

		val x = playerPositions[0].x + lastPlayerDirection.deltaX
		val y = playerPositions[0].y + lastPlayerDirection.deltaY

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
				nextTransition = transition.destination;
			}
		}
	}

	private fun updatePlayerPosition() {
		val nextPlayerPosition = this.nextPlayerPosition
		if (nextPlayerPosition != null && nextPlayerPosition.arrivalTime <= currentTime) {
			for (index in 1 until playerPositions.size) {
				playerPositions[index] = playerPositions[index - 1]
			}
			lastPlayerDirection = Direction.delta(
				nextPlayerPosition.position.x - playerPositions[0].x,
				nextPlayerPosition.position.y - playerPositions[0].y
			)!!
			playerPositions[0] = nextPlayerPosition.position
			this.nextPlayerPosition = null
			checkTransitions()
		}
	}

	private fun processInput(input: InputManager) {
		if (nextPlayerPosition == null) {
			var moveDirection: Direction? = null
			if (input.isPressed(InputKey.MoveLeft)) moveDirection = Direction.Left
			if (input.isPressed(InputKey.MoveDown)) moveDirection = Direction.Down
			if (input.isPressed(InputKey.MoveRight)) moveDirection = Direction.Right
			if (input.isPressed(InputKey.MoveUp)) moveDirection = Direction.Up

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
	}

	private fun canWalkTo(input: InputManager, x: Int, y: Int): Boolean {
		if (x < 0 || y < 0) return false
		if (input.isPressed(InputKey.Cheat)) return true
		if (!area.canWalkOnTime(x, y)) return false

		// TODO Movable characters
		for (character in area.objects.characters) {
			if (x == character.startX && y == character.startY) return false
		}

		for (door in area.objects.doors) {
			if (x == door.x && y == door.y) return false
		}

		for (areaObject in area.objects.objects) {
			if (x == areaObject.x && y == areaObject.y) return false
		}

		for (orb in area.objects.switchOrbs) {
			if (x == orb.x && y == orb.y) return false
		}
		// TODO Switch gates and platforms
		return true
	}

	companion object {
		val DOOR_OPEN_DURATION = 500.milliseconds
	}
}
