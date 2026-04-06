package mardek.state.ingame.menu

import mardek.input.InputKey
import mardek.state.ingame.encyclopedia.EncyclopediaSnapshot

/**
 * An instance of the "Encyclopedia" tab of the in-game menu. An instance of this class will be created whenever the
 * player scrolls to this tab.
 */
class EncyclopediaTab(

	/**
	 * A snapshot of the [mardek.state.ingame.encyclopedia.EncyclopediaState]
	 */
	val encyclopedia: EncyclopediaSnapshot
) : InGameMenuTab() {

	/**
	 * The currently-selected encyclopedia section:
	 * - 0 means People
	 * - 1 means Places
	 * - 2 means Artefacts
	 * - 3 means Bestiary
	 * - 4 means Dreamstones
	 */
	var currentSection = 0
		private set

	/**
	 * The currently-selected entry of the currently-selected encyclopedia section. This field is only meaningful
	 * when [inside] is `true`.
	 *
	 * Examples:
	 * - `currentSection == 0 && currentEntry == 0` means that the first person in the People section is selected
	 * - `currentSection == 3 && currentEntry == encyclopedia.monsters.size - 1` means that the last monster in the
	 * Bestiary section is selected.
	 */
	var currentEntry = 0
		private set

	/**
	 * - When [inside] is `false`, this field is meaningless.
	 * - When `inside && !showDetailsOfCurrentEntry`, the player is browsing the entries of one of the encyclopedia
	 * sections (e.g. People or Bestiary).
	 * - When `inside && showDetailsOfCurrentEntry`, the player is looking at/zooming in on *one* entry of one of the
	 * sections (e.g. the player is looking at the details of Fungoblin).
	 */
	var showDetailsOfCurrentEntry = false

	/**
	 * When `inside && !showDetailsOfCurrentEntry`, the renderer will render all the entries of one of the encyclopedia
	 * sections. The renderer will set this field to the number of *rows* that would fit on the screen.
	 *
	 * Consider the following example:
	 * - The player is browsing the Bestiary section.
	 * - There are 50 monsters in the current chapter.
	 * - The renderer can fit 30 rows on the screen.
	 *
	 * The renderer would:
	 * - Render 2 columns of monster entries.
	 * - Render the first 30 monsters in the first/left column.
	 * - Render the remaining 20 monsters in the second column.
	 * - **Set `numRenderedRows` to 30**.
	 *
	 * This variable is used by the state to determine by how much it needs to increase [currentEntry] when the player
	 * presses the `MoveLeft` or `MoveRight` key.
	 */
	var numRenderedRows = 0

	override fun getText() = "Encyclopaedia"

	override fun canGoInside() = true

	override fun shouldShowLowerBarClock() = !inside

	override fun processKeyPress(key: InputKey, context: UiUpdateContext) {
		if (inside) {
			if (showDetailsOfCurrentEntry) {
				if (key == InputKey.Cancel) {
					showDetailsOfCurrentEntry = false
					context.soundQueue.insert(context.sounds.ui.clickCancel)
				}
			} else {
				if (key == InputKey.Cancel) {
					inside = false
					context.soundQueue.insert(context.sounds.ui.clickCancel)
				}

				val elements = when (currentSection) {
					0 -> encyclopedia.people
					1 -> encyclopedia.places
					2 -> encyclopedia.artefacts
					3 -> encyclopedia.monsters
					else -> throw Error("Unexpected encyclopedia section $currentSection")
				}

				if (key == InputKey.Interact && elements[currentEntry].entry != null) {
					showDetailsOfCurrentEntry = true
					context.soundQueue.insert(context.sounds.ui.clickConfirm)
				}

				if (key == InputKey.MoveUp || key == InputKey.MoveLeft) {
					currentEntry -= if (key == InputKey.MoveLeft && numRenderedRows > 0) numRenderedRows else 1

					if (currentEntry < 0) currentEntry = elements.size - 1
					context.soundQueue.insert(context.sounds.ui.scroll1)
				}
				if (key == InputKey.MoveDown || key == InputKey.MoveRight) {
					currentEntry += if (key == InputKey.MoveRight && numRenderedRows > 0) numRenderedRows else 1
					if (currentEntry >= elements.size) currentEntry = 0
					context.soundQueue.insert(context.sounds.ui.scroll1)
				}
			}
		} else {
			val numSections = 4 // TODO CHAP3 Increase to 5 in chapter 3
			if (key == InputKey.MoveLeft) {
				currentSection -= 1
				if (currentSection < 0) currentSection += numSections
				context.soundQueue.insert(context.sounds.ui.scroll1)
			}
			if (key == InputKey.MoveRight) {
				currentSection += 1
				if (currentSection >= numSections) currentSection -= numSections
				context.soundQueue.insert(context.sounds.ui.scroll1)
			}
			if (key == InputKey.Interact && canGoInside()) {
				inside = true
				currentEntry = 0
				showDetailsOfCurrentEntry = false
				context.soundQueue.insert(context.sounds.ui.clickConfirm)
			}
		}
	}

	override fun shouldShowSectionList() = !inside
}
