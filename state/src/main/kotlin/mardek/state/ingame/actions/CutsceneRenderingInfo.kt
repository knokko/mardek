package mardek.state.ingame.actions

import mardek.state.util.Rectangle

/**
 * This class is used by [CampaignActionsState] to track some rendering & interaction info for cutscenes.
 * It tracks the currently-shown subtitle and the position of the skip button.
 */
class CutsceneRenderingInfo {

	/**
	 * Some cutscenes (chapter 1 intro) have subtitles. This field tracks the current subtitle that should be rendered.
	 * The renderer is responsible for writing to this field.
	 */
	var subtitle = Pair(1, "")

	/**
	 * During short cutscenes, this field is meaningless.
	 *
	 * But, when the player is watching a sufficiently-long cutscene, the renderer will render a skip button,
	 * and set this field to the region where the skip button was rendered.
	 *
	 * This field is needed to check whether the current mouse position is on the skip button.
	 * To perform this check, use [isOnSkipButton].
	 * Don't use `cutsceneSkipButton.contains(...)` since that method assumes that the skip button is rectangular,
	 * which it is not.
	 */
	var skipButton: Rectangle = Rectangle(-1234, -1234, 1, 1)

	/**
	 * Assuming that the mouse cursor is at (mouseX, mouseY), this method computes whether the mouse is hovering over
	 * the skip button.
	 */
	fun isOnSkipButton(mouseX: Int, mouseY: Int): Boolean {
		if (!skipButton.contains(mouseX, mouseY)) return false

		val dx = (mouseX - skipButton.minX).toFloat() / skipButton.width
		val dy = (mouseY - skipButton.minY).toFloat() / skipButton.height

		return if (dy >= 0.5f) 2f * (1f - dy) >= dx else 2f * dy >= dx
	}
}
