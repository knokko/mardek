package mardek.state.ingame.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.CollectionField
import com.github.knokko.bitser.field.IntegerField
import mardek.assets.area.Direction
import mardek.assets.area.OptimizedArea
import mardek.input.InputKey
import mardek.input.InputManager
import kotlin.time.Duration
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
			lastPlayerDirection = Direction.delta(
				nextPlayerPosition.position.x - playerPositions[0].x,
				nextPlayerPosition.position.y - playerPositions[0].y
			)!!
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
			var moveDirection: Direction? = null
			if (input.isPressed(InputKey.MoveLeft)) moveDirection = Direction.Left
			if (input.isPressed(InputKey.MoveDown)) moveDirection = Direction.Down
			if (input.isPressed(InputKey.MoveRight)) moveDirection = Direction.Right
			if (input.isPressed(InputKey.MoveUp)) moveDirection = Direction.Up

			if (moveDirection != null) {
				val nextX = playerPositions[0].x + moveDirection.deltaX
				val nextY = playerPositions[0].y + moveDirection.deltaY
				if (area.canWalkAt(nextX, nextY)) {
					nextPlayerPosition = NextAreaPosition(
						AreaPosition(nextX, nextY), currentTime, currentTime + 0.2.seconds
					)
				} else {
					lastPlayerDirection = moveDirection
				}
			}
		}
	}
}
