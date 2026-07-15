package mardek.state.ingame.menu

import mardek.input.InputKey
import mardek.state.settings.UserSettings
import kotlin.math.max
import kotlin.math.min

/**
 * The "Options" tab.
 *
 * This tab also exists in the original game, but this engine has different settings.
 *
 * This class tracks which setting property the player *has selected*, but not the *values*.
 * The actual values are stored in the `MardekWindow.userSettings`, and on disk.
 */
class SettingsTab : InGameMenuTab() {

	/**
	 * The current user settings.
	 *
	 * This is a late-init property that will be initialized by the renderer.
	 */
	lateinit var settings: UserSettings

	/**
	 * The 'index' of the currently-selected 'settings type':
	 * - 0 means video settings
	 * - 1 means audio settings
	 *
	 * The player can change this index by pressing [InputKey.MoveLeft] or [InputKey.MoveRight] while *not* [inside]
	 */
	var selectedType = 0

	/**
	 * The 'index' of the currently-selected property.
	 *
	 * The player can change this index by pressing [InputKey.MoveUp] or [InputKey.MoveDown] while [inside].
	 * The player can change the *value* of the selected property by pressing [InputKey.MoveLeft], [InputKey.MoveRight],
	 * or [InputKey.Interact] while [inside].
	 */
	var selectedProperty = 0

	override fun getText() = "Options"

	override fun canGoInside() = true

	override fun processKeyPress(key: InputKey, context: UiUpdateContext) {
		if (!this::settings.isInitialized) return

		if (inside) {
			val numProperties = when (selectedType) {
				0 -> 5
				1 -> 3
				else -> throw RuntimeException("Unexpected settings type $selectedType")
			}

			if (key == InputKey.MoveDown && selectedProperty < numProperties - 1) {
				selectedProperty += 1
				context.soundQueue.insert(context.sounds.ui.scroll1)
			}
			if (key == InputKey.MoveUp && selectedProperty > 0) {
				selectedProperty -= 1
				context.soundQueue.insert(context.sounds.ui.scroll1)
			}

			val isToggle = (key == InputKey.MoveLeft || key == InputKey.MoveRight || (inside && key == InputKey.Interact))
			if (selectedType == 0) {
				val videoSettings = settings.videoSettings
				if (selectedProperty == 0 && videoSettings.availableDevices.size > 1) {
					if (key == InputKey.MoveLeft) {
						videoSettings.preferredDevice -= 1
						if (videoSettings.preferredDevice < 0) {
							videoSettings.preferredDevice = videoSettings.availableDevices.size - 1
						}
						context.soundQueue.insert(context.sounds.ui.scroll2)
						settings.save()
					}
					if (key == InputKey.MoveRight) {
						videoSettings.preferredDevice += 1
						if (videoSettings.preferredDevice >= videoSettings.availableDevices.size) {
							videoSettings.preferredDevice = 0
						}
						context.soundQueue.insert(context.sounds.ui.scroll2)
						settings.save()
					}
				}

				if (selectedProperty == 1 && isToggle) {
					if (videoSettings.canUncapFps) {
						videoSettings.capFps = !videoSettings.capFps
						context.soundQueue.insert(context.sounds.ui.toggleSkill)
					} else context.soundQueue.insert(context.sounds.ui.clickReject)
				}

				if (selectedProperty == 2 && isToggle) {
					videoSettings.showFps = !videoSettings.showFps
				}

				if (selectedProperty == 3) {
					if (key == InputKey.MoveLeft && videoSettings.framesInFlight > 1) {
						videoSettings.framesInFlight -= 1
						context.soundQueue.insert(context.sounds.ui.scroll2)
					}
					if (key == InputKey.MoveRight && videoSettings.framesInFlight < 3) {
						videoSettings.framesInFlight += 1
						context.soundQueue.insert(context.sounds.ui.scroll2)
					}
				}

				if (selectedProperty == 4 && isToggle) {
					videoSettings.delayRendering = !videoSettings.delayRendering
				}
			}

			if (selectedType == 1) {
				val audioSettings = settings.audioSettings
				if (selectedProperty in 0..3) {
					val oldValue = when (selectedProperty) {
						0 -> audioSettings.masterVolume
						1 -> audioSettings.musicVolume
						2 -> audioSettings.soundEffectVolume
						else -> throw RuntimeException("Unexpected property $selectedProperty")
					}

					var newValue = oldValue
					if (key == InputKey.MoveLeft) newValue = max(0, newValue - 5)
					if (key == InputKey.MoveRight) newValue = min(100, newValue + 5)

					when (selectedProperty) {
						0 -> audioSettings.masterVolume = newValue
						1 -> audioSettings.musicVolume = newValue
						2 -> audioSettings.soundEffectVolume = newValue
					}
				}
			}
		} else {
			if (key == InputKey.MoveLeft) {
				selectedType -= 1
				if (selectedType < 0) selectedType += 2
				context.soundQueue.insert(context.sounds.ui.scroll2)
				selectedProperty = 0
			}
			if (key == InputKey.MoveRight) {
				selectedType = (selectedType + 1) % 2
				context.soundQueue.insert(context.sounds.ui.scroll2)
				selectedProperty = 0
			}
		}

		val wasInside = inside
		super.processKeyPress(key, context)
		if (wasInside && !inside) settings.save()
	}
}
