package mardek.state.ingame.actions

import mardek.input.InputKey
import mardek.input.MouseMoveEvent
import mardek.state.ingame.CampaignState
import mardek.state.saves.SaveFile
import mardek.state.saves.SaveSelectionState
import mardek.state.util.Rectangle

/**
 * When the game state is a [CampaignActionsState] and its current action is an
 * [mardek.content.action.ActionEndOfChapter], this class is used to track the 'interaction state' of the player with
 * the End of Chapter X user interface. For instance, this class tracks whether the player is browsing the item storage,
 * or whether the player is hovering their mouse over e.g. the "Continue" button.
 */
class EndOfChapterState {

	/**
	 * The region where the "Save" button was rendered, or `null` before the first frame is rendered.
	 */
	var saveButton: Rectangle? = null

	/**
	 * The region where the "Item Storage" button was rendered, or `null` before the first frame is rendered.
	 */
	var itemStorageButton: Rectangle? = null

	/**
	 * The region where the "Continue" button was rendered, or `null` before the first frame is rendered.
	 */
	var continueButton: Rectangle? = null

	/**
	 * The currently-selected button:
	 * - -1 means that no button is selected
	 * - 0 means that the "Save" button is selected
	 * - 1 means that the "Item Storage" button is selected
	 * - 2 means that the "Continue" button is selected
	 */
	var selectedButtonIndex = -1
		private set

	/**
	 * This field will be non-null if and only if the player is currently browsing the item storage.
	 */
	var itemStorage: ItemStorageInteractionState? = null

	/**
	 * This field will be non-null if and only if the player is currently selecting a save (to overwrite).
	 */
	var saveSelectionState: SaveSelectionState? = null

	/**
	 * When this is `true`, the [CampaignActionsState] should move on to the next node/chapter
	 */
	var shouldContinue = false
		private set

	/**
	 * This method should be called during every game 'tick' while the current campaign action is an
	 * [mardek.content.action.ActionEndOfChapter]
	 */
	internal fun update(context: CampaignState.UpdateContext) {
		if (shouldContinue) return
		if (saveSelectionState != null) {
			val savesContext = SaveSelectionState.UpdateContext(
				context.saves, context.content, context.soundQueue, true
			)
			if (saveSelectionState!!.update(savesContext)) saveSelectionState = null
		}
	}

	/**
	 * This method should be called whenever a key is pressed while the current campaign action is an
	 * [mardek.content.action.ActionEndOfChapter]
	 */
	internal fun processKeyPress(context: CampaignState.UpdateContext, campaign: CampaignState, key: InputKey) {
		if (shouldContinue) return

		if (saveSelectionState != null) {
			val savesContext = SaveSelectionState.UpdateContext(
				context.saves, context.content, context.soundQueue, true
			)
			val outcome = saveSelectionState!!.pressKey(savesContext, key)
			if (outcome.canceled) saveSelectionState = null
			if (outcome.failed) {
				context.soundQueue.insert(context.content.audio.fixedEffects.ui.clickReject)
				saveSelectionState = null
			}
			if (outcome.finished) {
				if (outcome.save != null) {
					context.saves.writeSaveTo(context.content, campaign, outcome.save.file)
				} else {
					context.saves.createSave(
						context.content, campaign,
						context.campaignName, SaveFile.Type.EndOfChapter,
					)
				}
				saveSelectionState = null
			}
		} else if (itemStorage != null) {
			if (key == InputKey.Cancel) {
				context.soundQueue.insert(context.content.audio.fixedEffects.ui.clickCancel)
				itemStorage = null
			} else itemStorage!!.processKeyPress(context, campaign, key)
		} else {
			if (key == InputKey.MoveDown && selectedButtonIndex < 2) selectedButtonIndex += 1
			if (key == InputKey.MoveUp && selectedButtonIndex > 0) selectedButtonIndex -= 1
			if (key == InputKey.Interact || key == InputKey.Click) {
				if (selectedButtonIndex == 0) {
					saveSelectionState = SaveSelectionState(arrayOf(context.campaignName))
				}
				if (selectedButtonIndex == 1) {
					itemStorage = ItemStorageInteractionState()
				}
				if (selectedButtonIndex == 2) {
					shouldContinue = true
				}
			}
		}
	}

	/**
	 * This method should be called whenever the mouse is moved while the current campaign action is an
	 * [mardek.content.action.ActionEndOfChapter]
	 */
	internal fun processMouseMove(campaign: CampaignState, event: MouseMoveEvent) {
		if (shouldContinue) return

		if (itemStorage != null) {
			itemStorage!!.processMouseMove(campaign, event.newX, event.newY)
		} else if (saveSelectionState == null) {
			selectedButtonIndex = -1
			if (saveButton?.contains(event.newX, event.newY) == true) selectedButtonIndex = 0
			if (itemStorageButton?.contains(event.newX, event.newY) == true) selectedButtonIndex = 1
			if (continueButton?.contains(event.newX, event.newY) == true) selectedButtonIndex = 2
		}
	}
}
