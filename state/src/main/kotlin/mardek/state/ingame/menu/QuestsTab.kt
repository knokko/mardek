package mardek.state.ingame.menu

import mardek.input.InputKey
import mardek.state.ingame.story.QuestsSnapshot
import kotlin.math.max

/**
 * The "Quests" tab of the in-game menu.
 *
 * This class tracks at which quest the player is looking.
 */
class QuestsTab(

	/**
	 * When the player opens the "Quests" tab, a *snapshot* of the current state of the quests is made, and stored in
	 * this field.
	 */
	val quests: QuestsSnapshot
): InGameMenuTab() {

	/**
	 * The index of the currently-selected (highlighted) quest, into either `quests.active` or `quests.completed`
	 * (depending on [showCompleted]).
	 *
	 * **Note that this index can be out of bounds!** This is unavoidable, since it is possible that there are 0 open
	 * quests or 0 completed quests.
	 */
	var questIndex = 0
		private set

	/**
	 * - When `true`, the player is looking at the quests that are already completed
	 * - When `false`, the player is looking at the active quests
	 */
	var showCompleted = false
		private set
	
	override fun getText() = "Quests"

	override fun canGoInside() = if (showCompleted) quests.completed.isNotEmpty() else quests.active.isNotEmpty()

	override fun processKeyPress(key: InputKey, context: UiUpdateContext) {
		super.processKeyPress(key, context)

		if (key == InputKey.MoveLeft && showCompleted && !inside) {
			showCompleted = false
			context.soundQueue.insert(context.sounds.ui.scroll2)
		}
		if (key == InputKey.MoveRight && !showCompleted && !inside) {
			showCompleted = true
			context.soundQueue.insert(context.sounds.ui.scroll2)
		}

		val limit = if (showCompleted) quests.completed.size else quests.active.size
		if (questIndex >= limit) questIndex = max(0, limit - 1)

		if (key == InputKey.MoveUp && inside && questIndex > 0) {
			questIndex -= 1
			context.soundQueue.insert(context.sounds.ui.scroll1)
		}

		if (key == InputKey.MoveDown && inside) {
			if (questIndex + 1 < limit) {
				questIndex += 1
				context.soundQueue.insert(context.sounds.ui.scroll1)
			}
		}
	}
}
