package mardek.state.ingame.menu

import mardek.input.InputKey
import mardek.state.ingame.story.QuestsSnapshot
import kotlin.math.max

class QuestsTab(val quests: QuestsSnapshot): InGameMenuTab() {

	var questIndex = 0
	var showCompleted = false
	
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
