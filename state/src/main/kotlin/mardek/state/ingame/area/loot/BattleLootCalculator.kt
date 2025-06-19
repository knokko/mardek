package mardek.state.ingame.area.loot

import mardek.content.Content
import mardek.content.characters.PlayableCharacter
import mardek.content.inventory.Dreamstone
import mardek.content.inventory.Item
import mardek.content.inventory.ItemStack
import mardek.content.inventory.PlotItem
import mardek.content.skill.PassiveSkill
import mardek.state.ingame.battle.Battle
import mardek.state.ingame.characters.CharacterState
import java.lang.IllegalArgumentException
import kotlin.math.roundToInt
import kotlin.random.Random

private fun getModifiers(party: List<Pair<PlayableCharacter, CharacterState>>): Pair<Int, Int> {
	var goldModifier = 1
	var extraLootChance = 0
	for ((_, state) in party) {
		for (skill in state.toggledSkills) {
			if (skill is PassiveSkill) {
				goldModifier += skill.goldModifier
				extraLootChance += skill.addLootChance
			}
		}
	}
	return Pair(goldModifier, extraLootChance)
}

fun generateBattleLoot(
	content: Content, battle: Battle,
	party: List<Pair<PlayableCharacter, CharacterState>>
): BattleLoot {
	val (goldModifier, extraLootChance) = getModifiers(party)

	val items = mutableMapOf<Item, Int>()
	val plotItems = mutableSetOf<PlotItem>()
	val dreamStones = mutableSetOf<Dreamstone>()
	var gold = 0

	for (enemy in battle.startingEnemies) {
		val monster = enemy?.monster ?: continue
		for (potentialItem in monster.loot) {
			if (potentialItem.chance + extraLootChance > Random.Default.nextInt(100)) {
				val item = potentialItem.item ?: throw IllegalArgumentException(
					"${monster.name} has invalid loot"
				)
				items[item] = items.getOrDefault(item, 0) + 1
			}
		}
		for (potentialItem in monster.plotLoot) {
			if (potentialItem.chance + extraLootChance > Random.Default.nextInt(100)) {
				plotItems.add(potentialItem.item)
			}
		}
		dreamStones.addAll(monster.dreamLoot)

		gold += (Random.Default.nextInt(
			enemy.level * enemy.level + 1 + Random.Default.nextInt(10)
		) * (Random.Default.nextDouble() + 0.5) * goldModifier).roundToInt()
	}

	val foundItems = items.isNotEmpty() || plotItems.isNotEmpty() || dreamStones.isNotEmpty()
	val itemText = if (foundItems) "You ${content.battle.lootItemTexts.random()}:"
	else content.battle.lootNoItemTexts.random()
	return BattleLoot(
		gold, ArrayList(items.entries.map { ItemStack(it.key, it.value) }),
		ArrayList(plotItems), ArrayList(dreamStones), itemText, party
	)
}
