package mardek.state.ingame.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.battle.BattleBackground
import mardek.content.battle.Monster
import mardek.content.battle.PartyLayout
import mardek.content.inventory.Dreamstone
import mardek.content.inventory.Item
import mardek.content.inventory.ItemStack
import mardek.content.inventory.PlotItem
import mardek.content.stats.CombatStat
import mardek.content.stats.StatusEffect
import mardek.state.ingame.area.loot.BattleLoot
import mardek.state.ingame.characters.CharacterState
import java.lang.IllegalArgumentException
import kotlin.random.Random

@BitStruct(backwardCompatible = true)
class Battle(

	@BitField(id = 0)
	@NestedFieldSetting(path = "c", optional = true)
	@NestedFieldSetting(path = "", sizeField = IntegerField(expectUniform = true, minValue = 4, maxValue = 4))
	val startingEnemies: Array<Enemy?>,

	@BitField(id = 1)
	@ReferenceField(stable = true, label = "enemy party layouts")
	val enemyLayout: PartyLayout,

	@BitField(id = 2)
	val music: String,

	@BitField(id = 3)
	@ReferenceField(stable = true, label = "battle backgrounds")
	val background: BattleBackground,
) {
	internal constructor() : this(arrayOf(null, null, null, null), PartyLayout(), "", BattleBackground())

	fun generateLoot(): BattleLoot {
		val items = mutableMapOf<Item, Int>()
		val plotItems = mutableSetOf<PlotItem>()
		val dreamStones = mutableSetOf<Dreamstone>()
		var gold = 0 // TODO what about the gold?
		for (enemy in startingEnemies) {
			val monster = enemy?.monster ?: continue
			for (potentialItem in monster.loot) {
				if (potentialItem.chance > Random.Default.nextInt(100)) {
					val item = potentialItem.item ?: throw IllegalArgumentException(
						"${monster.name} has invalid loot"
					)
					items[item] = items.getOrDefault(item, 0) + 1
				}
			}
			for (potentialItem in monster.plotLoot) {
				if (potentialItem.chance > Random.Default.nextInt(100)) {
					plotItems.add(potentialItem.item)
				}
			}
			dreamStones.addAll(monster.dreamLoot)
		}

		return BattleLoot(
			gold, ArrayList(items.entries.map { ItemStack(it.key, it.value) }),
			ArrayList(plotItems), ArrayList(dreamStones)
		)
	}
}

@BitStruct(backwardCompatible = true)
class Enemy(
	@BitField(id = 0)
	@ReferenceField(stable = true, label = "monsters")
	val monster: Monster,

	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 1)
	val level: Int
) {
	@Suppress("unused")
	private constructor() : this(Monster(), 0)
}
