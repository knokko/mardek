package mardek.importer.characters

internal class FlashCreatureStats(
	val displayName: String,
	val areaSpritesName: String,
	val characterClassName: String,
	val elementName: String,
	val startingLevel: Int,
	val startingStats: IntArray,
	val startingEquipment: Array<String>,
	val startingInventory: Array<String>,
	val startingActionSkills: Array<String>,
	val startingPassivesAndReactions: Array<Array<String>>,
) {
}
