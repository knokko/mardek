package mardek.state.ingame

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import mardek.assets.GameAssets
import mardek.input.InputKey
import mardek.input.InputManager
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.characters.CharactersState
import kotlin.time.Duration

@BitStruct(backwardCompatible = false)
class GameProgression(

	@BitField(ordering = 0, optional = true)
	var currentArea: AreaState?,

	@BitField(ordering = 1)
	val characters: CharactersState,
) {

	@Suppress("unused")
	private constructor() : this(null, CharactersState())

	fun update(input: InputManager, timeStep: Duration, assets: GameAssets) {
		var shouldInteract = false
		while (true) {
			val event = input.consumeEvent() ?: break
			if (!event.didPress) continue

			val currentArea = this.currentArea ?: continue
			if (event.key == InputKey.Interact) {
				shouldInteract = true
				continue
			}
			if (event.key == InputKey.ScrollUp || event.key == InputKey.ScrollDown) {
				val currentIndex = assets.areas.indexOf(currentArea.area)

				var nextIndex = currentIndex
				if (event.key == InputKey.ScrollUp) nextIndex -= 1
				else nextIndex += 1

				if (nextIndex < 0) nextIndex += assets.areas.size
				if (nextIndex >= assets.areas.size) nextIndex -= assets.areas.size

				var nextPosition = currentArea.getPlayerPosition(0)
				val nextArea = assets.areas[nextIndex]
				if (nextPosition.x > 5 + nextArea.width || nextPosition.y > 3 + nextArea.height) {
					nextPosition = AreaPosition(3, 3)
				}
				this.currentArea = AreaState(nextArea, nextPosition)
			}
		}

		currentArea?.update(input, timeStep, shouldInteract)
		val destination = currentArea?.nextTransition
		if (destination != null) {
			val nextArea = assets.areas.find { it.properties.rawName == destination.areaName }
			if (nextArea != null) {
				currentArea = AreaState(nextArea, AreaPosition(destination.x, destination.y))
			} else currentArea!!.nextTransition = null
		}
	}
}
