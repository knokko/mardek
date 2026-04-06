package mardek.state.ingame.encyclopedia

import mardek.content.encyclopedia.EncyclopediaArea
import mardek.content.encyclopedia.EncyclopediaArtefact
import mardek.content.encyclopedia.EncyclopediaMonster
import mardek.content.encyclopedia.EncyclopediaPerson

/**
 * A 'snapshot' of the [EncyclopediaState]: this captures:
 * - which encyclopedia entries should be visible
 * - which encyclopedia entries should be marked as unknown (greyed out and name replaced by "-----")
 * - which encyclopedia entries should be completely invisible (because it only occurs in future chapters)
 */
class EncyclopediaSnapshot(

	/**
	 * The entries for the "People" section: the renderer should render `people.size` (possibly greyed-out) entries.
	 */
	val people: Array<OptionalEntry<EncyclopediaPerson.Snapshot>>,

	/**
	 * The entries for the "Places" section: the renderer should render `people.size` (possibly greyed-out) entries.
	 */
	val places: Array<OptionalEntry<EncyclopediaArea>>,

	/**
	 * The entries for the "Artefacts" section: the renderer should render `people.size` (possibly greyed-out) entries.
	 */
	val artefacts: Array<OptionalEntry<EncyclopediaArtefact>>,

	/**
	 * The entries for the "Bestiary" section: the renderer should render `people.size` (possibly greyed-out) entries.
	 */
	val monsters: Array<OptionalEntry<EncyclopediaMonster>>,
) {

	/**
	 * Represents an (optional) entry in the encyclopedia that occupies space. Exactly 1 of [entry] and [lengthOfName]
	 * must be non-null:
	 * - When `entry != null`, the entry is visible in the encyclopedia
	 * - When `lengthOfName != null`, the entry is not yet discovered in the encyclopedia, and we should show
	 * `"-".repeat(lengthOfName)` as the 'display name' of the entry.
	 */
	class OptionalEntry<T>(

		/**
		 * When this is non-null, this is the encyclopedia entry that should be shown.
		 */
		val entry: T?,

		/**
		 * When this is non-null, the encyclopedia entry should be available in this chapter, but has not yet been
		 * discovered by the player. It should be greyed out, and we should only show `"-".repeat(lengthOfName)`.
		 */
		val lengthOfName: Int?,

		/**
		 * This field is only used for the bestiary: it counts how many times the corresponding monster has been slain.
		 */
		val amount: Int?,
	) {
		init {
			if ((entry == null) == (lengthOfName == null)) throw RuntimeException(
				"Exactly 1 of entry and lengthOfName must be null, but found $entry, $lengthOfName"
			)
		}
	}
}
