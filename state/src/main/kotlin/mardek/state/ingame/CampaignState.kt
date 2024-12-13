package mardek.state.ingame

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import mardek.assets.Campaign
import mardek.assets.characters.PlayableCharacter
import mardek.input.InputKey
import mardek.input.InputKeyEvent
import mardek.input.InputManager
import mardek.state.SoundQueue
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.characters.CharacterSelectionState
import mardek.state.ingame.characters.CharacterState
import kotlin.time.Duration

@BitStruct(backwardCompatible = false)
class CampaignState(

	@BitField(ordering = 0, optional = true)
	var currentArea: AreaState?,

	@BitField(ordering = 1)
	val characterSelection: CharacterSelectionState,

	@BitField(ordering = 2)
	@NestedFieldSetting(path = "k", fieldName = "CHARACTER_STATES_KEY")
	val characterStates: HashMap<PlayableCharacter, CharacterState>,
) {

	@Suppress("unused")
	private constructor() : this(null, CharacterSelectionState(), HashMap())

	var shouldOpenMenu = false

	fun update(input: InputManager, timeStep: Duration, soundQueue: SoundQueue, assets: Campaign) {
		var shouldInteract = false
		while (true) {
			val event = input.consumeEvent() ?: break
			if (event !is InputKeyEvent) continue
			if (!event.didPress) continue

			val currentArea = this.currentArea ?: continue
			if (event.key == InputKey.ToggleMenu) {
				shouldOpenMenu = true
				soundQueue.insert("menu-open")
				continue
			}

			if (event.key == InputKey.Interact) {
				shouldInteract = true
				continue
			}

			if (event.key == InputKey.ScrollUp || event.key == InputKey.ScrollDown) {
				val currentIndex = assets.areas.areas.indexOf(currentArea.area)

				var nextIndex = currentIndex
				if (event.key == InputKey.ScrollUp) nextIndex -= 1
				else nextIndex += 1

				if (nextIndex < 0) nextIndex += assets.areas.areas.size
				if (nextIndex >= assets.areas.areas.size) nextIndex -= assets.areas.areas.size

				var nextPosition = currentArea.getPlayerPosition(0)
				val nextArea = assets.areas.areas[nextIndex]
				if (nextPosition.x > 5 + nextArea.width || nextPosition.y > 3 + nextArea.height) {
					nextPosition = AreaPosition(3, 3)
				}
				this.currentArea = AreaState(nextArea, nextPosition)
			}
		}

		currentArea?.update(input, timeStep, shouldInteract)
		val destination = currentArea?.nextTransition
		if (destination != null) {
			val destinationArea = destination.area
			if (destinationArea != null) {
				currentArea = AreaState(destinationArea, AreaPosition(destination.x, destination.y))
			} else currentArea!!.nextTransition = null
		}
	}

	companion object {

		@Suppress("unused")
		@ReferenceField(stable = true, label = "playable characters")
		private val CHARACTER_STATES_KEY = false
	}
}
