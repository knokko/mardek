package mardek.importer.battle

import mardek.content.battle.BattleContent

internal fun importLootTexts(content: BattleContent) {
	content.lootItemTexts.addAll(listOf(
		"arduously scrounged",
		"stole amongst the carnage",
		"find where your foes once stood",
		"found","have acquired",
		"see these items",
		"now have these",
		"find in the aftermath",
		"now own"
	))
	content.lootNoItemTexts.addAll(listOf(
		"You didn't find any items.",
		"You got no items this time.",
		"No spoils here!",
		"Today was not your lucky day, item-wise.",
		"These opponents were poor and carried nothing."
	))
}
