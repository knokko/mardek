package mardek.state.ingame.menu

import mardek.input.InputKey
import mardek.state.VideoSettings

/**
 * The "Video Settings" tab.
 *
 * This tab only exists in this MARDEK engine; not in the original game.
 * It can be used to customize some (mostly Vulkan-related) video settings.
 *
 * This class tracks which video setting property the player *has selected*,
 * but not the *values*.
 * The actual values are stored in the `MardekWindow`, and on disk.
 */
class VideoSettingsTab : InGameMenuTab() {

	/**
	 * The current video settings.
	 *
	 * This is a late-init property that will be initialized by the renderer.
	 */
	lateinit var settings: VideoSettings

	/**
	 * The 'index' of the currently-selected property.
	 *
	 * The player can change this index by pressing [InputKey.MoveUp] or [InputKey.MoveDown].
	 * The player can change the *value* of the selected property by pressing [InputKey.MoveLeft], [InputKey.MoveRight],
	 * or [InputKey.Interact].
	 */
	var selectedProperty = 0

	override fun getText() = "Video  Settings"

	override fun canGoInside() = true

	override fun processKeyPress(key: InputKey, context: UiUpdateContext) {
		if (inside) {
			if (key == InputKey.MoveDown && selectedProperty < 4) {
				selectedProperty += 1
				context.soundQueue.insert(context.sounds.ui.scroll1)
			}
			if (key == InputKey.MoveUp && selectedProperty > 0) {
				selectedProperty -= 1
				context.soundQueue.insert(context.sounds.ui.scroll1)
			}
		}

		val wasInside = inside
		super.processKeyPress(key, context)
		if (!this::settings.isInitialized) return

		if (selectedProperty == 0 && settings.availableDevices.size > 1) {
			if (key == InputKey.MoveLeft) {
				settings.preferredDevice -= 1
				if (settings.preferredDevice < 0) settings.preferredDevice = settings.availableDevices.size - 1
				context.soundQueue.insert(context.sounds.ui.scroll2)
				settings.save()
			}
			if (key == InputKey.MoveRight) {
				settings.preferredDevice += 1
				if (settings.preferredDevice >= settings.availableDevices.size) settings.preferredDevice = 0
				context.soundQueue.insert(context.sounds.ui.scroll2)
				settings.save()
			}
		}

		val isToggle = (key == InputKey.MoveLeft || key == InputKey.MoveRight || (inside && key == InputKey.Interact))
		if (selectedProperty == 1 && isToggle) {
			if (settings.canUncapFps) {
				settings.capFps = !settings.capFps
				context.soundQueue.insert(context.sounds.ui.toggleSkill)
			} else context.soundQueue.insert(context.sounds.ui.clickReject)
		}

		if (selectedProperty == 2 && isToggle) {
			settings.showFps = !settings.showFps
		}

		if (selectedProperty == 3) {
			if (key == InputKey.MoveLeft && settings.framesInFlight > 1) {
				settings.framesInFlight -= 1
				context.soundQueue.insert(context.sounds.ui.scroll2)
			}
			if (key == InputKey.MoveRight && settings.framesInFlight < 3) {
				settings.framesInFlight += 1
				context.soundQueue.insert(context.sounds.ui.scroll2)
			}
		}

		if (selectedProperty == 4 && isToggle) {
			settings.delayRendering = !settings.delayRendering
		}

		if (wasInside && !inside) settings.save()
	}
}
