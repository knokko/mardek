package mardek.state.area

import mardek.assets.area.Area
import mardek.input.InputKey
import mardek.input.InputManager
import mardek.state.GameState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class AreaState(val area: Area, initialPlayerPosition: AreaPosition): GameState {

	private var currentTime = Duration.ZERO

	private val playerPositions = Array(4) { initialPlayerPosition }
	private var nextPlayerPosition: NextAreaPosition? = null

	override fun update(input: InputManager, timeStep: Duration): GameState {
		updatePlayerPosition()
		processInput(input)
		currentTime += timeStep
		return this
	}

	private fun updatePlayerPosition() {
		val nextPlayerPosition = this.nextPlayerPosition
		if (nextPlayerPosition != null && nextPlayerPosition.arrivalTime <= currentTime) {
			for (index in 1 until playerPositions.size) {
				playerPositions[index] = playerPositions[index - 1]
			}
			playerPositions[0] = nextPlayerPosition.position
			this.nextPlayerPosition = null
		}
	}

	private fun processInput(input: InputManager) {
		while (true) {
			val event = input.consumeEvent() ?: break

			if (event.key == InputKey.Interact && event.didPress) {
				// TODO Interact
			}
		}

		if (nextPlayerPosition == null) {
			val moveUp = input.isPressed(InputKey.MoveUp)
			val moveRight = input.isPressed(InputKey.MoveRight)
			val moveDown = input.isPressed(InputKey.MoveDown)
			val moveLeft = input.isPressed(InputKey.MoveLeft)

			var moveX = 0
			var moveY = 0
			if (moveUp && !moveDown && !moveRight && !moveLeft) moveY = -1
			if (moveDown && !moveUp && !moveRight && !moveLeft) moveY = 1
			if (moveLeft && !moveRight && !moveUp && !moveDown) moveX = -1
			if (moveRight && !moveLeft && !moveUp && !moveDown) moveX = 1

			if (moveX != 0 || moveY != 0) {
				val nextX = playerPositions[0].x + moveX
				val nextY = playerPositions[0].y + moveY
				if (area.canWalkAt(nextX, nextY)) {
					nextPlayerPosition = NextAreaPosition(AreaPosition(nextX, nextY), currentTime + 1.seconds)
				}
			}
		}
	}
}
