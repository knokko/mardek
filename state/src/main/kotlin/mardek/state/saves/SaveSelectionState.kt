package mardek.state.saves

import mardek.content.Content
import mardek.input.InputKey
import mardek.state.SoundQueue

/**
 * When the user is selecting a `SaveFile` to load (in "Load Game") or overwrite (in a save crystal), this class
 * controls the `SaveFile` selection process: the game calls `saveSelectionState.pressKey(...)` whenever the user
 * presses a key, and the `saveSelectionState` tells the game (and the renderer) which file the user is selecting.
 */
class SaveSelectionState(val selectableCampaigns: Array<String>) {

	init {
		if (this.selectableCampaigns.isEmpty()) throw IllegalArgumentException()
	}

	/**
	 * The index (in `selectableCampaigns`) of the currently-selected campaign
	 */
	var selectedCampaignIndex = 0
		private set

	/**
	 * The index (in `selectableFiles`) of the currently-selected `SaveFile`
	 */
	var selectedFileIndex = -1
		private set

	private var campaignForFiles: String? = null

	/**
	 * All `SaveFile`s that belong to the currently-selected campaign
	 */
	var selectableFiles: Array<SaveFile> = emptyArray()
		private set

	/**
	 * Updates this `SaveSelectionState`: if the user has scrolled to another campaign, the selectable save files will
	 * be updated.
	 *
	 * This method returns true if and only if the user has switched campaign,
	 * and there are 0 save files in the new campaign. When this happens, the file system is probably out-of-date, so
	 * the saves directory should be rescanned.
	 */
	fun update(context: UpdateContext): Boolean {
		val selectedCampaign = this.selectableCampaigns[selectedCampaignIndex]
		if (this.campaignForFiles != selectedCampaign) {
			if (this.campaignForFiles != null) this.selectedFileIndex = 0
			this.campaignForFiles = selectedCampaign
			this.selectableFiles = context.savesFolderManager.getSaves(selectedCampaign)
			if (this.selectableFiles.isEmpty() && !context.canSelectNewSave) return true
		}

		if (this.selectedFileIndex == -1 && !context.canSelectNewSave) this.selectedFileIndex = 0
		return false
	}

	/**
	 * The game should call this method whenever the uses presses a key. This `SaveSelectionState` will potentially
	 * change its state (e.g. increment `selectedFileIndex` when the key is `InputKey.MoveDown`).
	 *
	 * This method will return an `Outcome`: this outcome tells among others whether the user has confirmed his choice.
	 */
	fun pressKey(context: UpdateContext, key: InputKey): Outcome {
		if (update(context)) return Outcome(failed = true)

		val sounds = context.content.audio.fixedEffects
		if (key == InputKey.Cancel || key == InputKey.Escape || selectableCampaigns.isEmpty()) {
			context.soundQueue.insert(sounds.ui.clickCancel)
			return Outcome(canceled = true)
		}

		if (key == InputKey.MoveLeft && selectedCampaignIndex > 0) {
			selectedCampaignIndex -= 1
			context.soundQueue.insert(sounds.ui.scroll2)
		}
		if (key == InputKey.MoveRight && selectedCampaignIndex + 1 < selectableCampaigns.size) {
			selectedCampaignIndex += 1
			context.soundQueue.insert(sounds.ui.scroll2)
		}

		if (key == InputKey.MoveUp) {
			if (selectedFileIndex == 0 && context.canSelectNewSave) {
				selectedFileIndex = -1
				context.soundQueue.insert(sounds.ui.scroll1)
			}
			if (selectedFileIndex > 0) {
				selectedFileIndex -= 1
				context.soundQueue.insert(sounds.ui.scroll1)
			}
		}
		if (key == InputKey.MoveDown && selectedFileIndex + 1 < selectableFiles.size) {
			selectedFileIndex += 1
			context.soundQueue.insert(sounds.ui.scroll1)
		}

		if (key == InputKey.Interact || key == InputKey.ToggleMenu) {
			return if (selectedFileIndex == -1) {
				Outcome(save = null, finished = true)
			} else  {
				Outcome(save = selectableFiles[selectedFileIndex], finished = true)
			}
		}

		update(context)
		return Outcome()
	}

	/**
	 * Gets the name of the currently-selected campaign
	 */
	fun getSelectedCampaign() = this.selectableCampaigns[this.selectedCampaignIndex]

	/**
	 * This class contains all the 'parameters' of the `update` and `pressKey` methods
	 */
	class UpdateContext(
		val savesFolderManager: SavesFolderManager,
		val content: Content,
		val soundQueue: SoundQueue,
		val canSelectNewSave: Boolean,
	)

	/**
	 * The result of `SaveSelectionState.pressKey`. Note that in most cases, all of `finished`, `canceled`, and
	 * `failed` will be false, which means that the user is still browsing.
	 */
	class Outcome(
		/**
		 * - When `finished` is `true`, this is the selected `SaveFile`, or `null` to create a new `SaveFile`.
		 * - When `finished` is `false`, this field is meaningless, and should be `null`
		 */
		val save: SaveFile? = null,

		/**
		 * When the user has pressed the Q (cancel) key or the Escape key, this field will be true
		 */
		val canceled: Boolean = false,

		/**
		 * When the user has confirmed a save, this field will be true
		 */
		val finished: Boolean = false,

		/**
		 * This field will be true when something went wrong, or the save directory was modified unexpectedly. When
		 * this happens, the caller should rescan the saves folder, and possibly create a new `SaveSelectionState`.
		 */
		val failed: Boolean = false,
	) {
		init {
			if (canceled && (finished || failed)) throw IllegalArgumentException()
			if (finished && failed) throw IllegalArgumentException()
		}

		override fun toString() = "Outcome($save, canceled=$canceled, finished=$finished), failed=$failed"
	}
}
