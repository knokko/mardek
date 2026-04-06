package mardek.content.encyclopedia

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceFieldTarget

/**
 * Everything that goes into the encyclopedia (which can be found in the "Encyclopedia" tab of the in-game menu).
 */
@BitStruct(backwardCompatible = true)
class EncyclopediaContent {

	/**
	 * All the people that can be shown in the "People" section of the encyclopedia.
	 *
	 * The order of appearance in the encyclopedia is the order of appearance in this list.
	 * (So `people[0]` is the person that shows up on the top-left of the encyclopedia.)
	 */
	@BitField(id = 0)
	@ReferenceFieldTarget(label = "encyclopedia people")
	val people = ArrayList<EncyclopediaPerson>()

	/**
	 * All the areas that can be shown in the "Places" section of the encyclopedia.
	 *
	 * The order of appearance in the encyclopedia is the order of appearance in this list.
	 * (So `places[0]` is the area that shows up on the top-left of the encyclopedia.)
	 */
	@BitField(id = 1)
	@ReferenceFieldTarget(label = "encyclopedia places")
	val places = ArrayList<EncyclopediaArea>()

	/**
	 * All the artefacts/treasures that can be shown in the "Artefacts" section of the encyclopedia.
	 *
	 * The order of appearance in the encyclopedia is the order of appearance in this list.
	 * (So `artefacts[0]` is the artefact that shows up on the top-left of the encyclopedia.)
	 */
	@BitField(id = 2)
	@ReferenceFieldTarget(label = "encyclopedia artefacts")
	val artefacts = ArrayList<EncyclopediaArtefact>()

	/**
	 * All the monsters that can be shown in the "Bestiary" section of the encyclopedia (basically all monsters
	 * except Security Demon).
	 *
	 * The order of appearance in the encyclopedia is the order of appearance in this list.
	 * (So `monsters[0]` is the monster that shows up on the top-left of the encyclopedia.)
	 */
	@BitField(id = 3)
	@ReferenceFieldTarget(label = "encyclopedia monsters")
	val monsters = ArrayList<EncyclopediaMonster>()

	// TODO CHAP3 DreamStones
}
