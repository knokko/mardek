package mardek.state.area

import mardek.assets.area.OptimizedArea
import mardek.input.InputKey
import mardek.input.InputManager
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class AreaState(val area: OptimizedArea, initialPlayerPosition: AreaPosition) {

	var currentTime = Duration.ZERO
		private set

	private val playerPositions = Array(4) { initialPlayerPosition }
	var lastPlayerDirectionX = 0
		private set
	var lastPlayerDirectionY = 0
		private set
	var nextPlayerPosition: NextAreaPosition? = null
		private set

	fun update(input: InputManager, timeStep: Duration) {
		updatePlayerPosition()
		processInput(input)
		currentTime += timeStep
	}

	fun getPlayerPosition(index: Int) = playerPositions[index]

	private fun updatePlayerPosition() {
		val nextPlayerPosition = this.nextPlayerPosition
		if (nextPlayerPosition != null && nextPlayerPosition.arrivalTime <= currentTime) {
			for (index in 1 until playerPositions.size) {
				playerPositions[index] = playerPositions[index - 1]
			}
			lastPlayerDirectionX = nextPlayerPosition.position.x - playerPositions[0].x
			lastPlayerDirectionY = nextPlayerPosition.position.y - playerPositions[0].y
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
					nextPlayerPosition = NextAreaPosition(
						AreaPosition(nextX, nextY), currentTime, currentTime + 0.2.seconds
					)
				} else {
					lastPlayerDirectionX = moveX
					lastPlayerDirectionY = moveY
				}
			}
		}
	}
}
