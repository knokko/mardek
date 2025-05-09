package mardek.state.ingame.battle

import mardek.content.battle.StrategyEntry
import mardek.content.battle.StrategyPool
import kotlin.random.Random

class MonsterStrategyCalculator(
	private val state: BattleState,
) {
	private val enemyIndex = state.onTurn!!.index
	private val monster = state.battle.enemies[enemyIndex]!!.monster
	private val myState = state.enemyStates[enemyIndex]!!

	init {
		if (state.onTurn!!.isPlayer) throw IllegalStateException("A monster must be on turn")
	}

	fun determineNextMove(): BattleMove {
		val pool = determineNextPool()
		myState.lastStrategy = pool
		if (pool == null) return BattleMoveWait

		myState.usedStrategies[pool] = myState.usedStrategies.getOrDefault(pool, 0) + 1

		val entry = chooseEntry(pool)

		val randomPlayerTarget = CombatantReference(
			isPlayer = true, index = state.players.withIndex().filter { it.value != null }.random().index, state
		)
		val item = entry.item
		if (item != null) return BattleMoveItem(item, randomPlayerTarget)

		val skill = entry.skill
		// TODO Support more target types and pick the right target
		if (skill != null) return BattleMoveSkill(skill, BattleSkillTargetSingle(randomPlayerTarget))

		return BattleMoveBasicAttack(randomPlayerTarget)
	}

	private fun chooseEntry(pool: StrategyPool): StrategyEntry {
		val totalChance = pool.entries.sumOf { it.chance }
		if (totalChance == 0) return pool.entries.random() else {
			val selectedChance = Random.Default.nextInt(totalChance)
			var currentChance = 0
			for (entry in pool.entries) {
				if (selectedChance < currentChance + entry.chance) return entry
				currentChance += entry.chance
			}
			throw Error("Should not happen: entries are ${pool.entries}")
		}
	}

	private fun determineNextPool(): StrategyPool? {
		for (pool in monster.strategies) {
			if (!areCriteriaSatisfied(pool)) continue

			val entries = pool.entries.filter { it.skill == null || it.skill!!.manaCost <= myState.currentMana }
			if (entries.isNotEmpty()) return pool
		}

		return null
	}

	internal fun areCriteriaSatisfied(pool: StrategyPool): Boolean {
		val criteria = pool.criteria

		val maxUses = criteria.maxUses
		val used = myState.usedStrategies[pool]
		if (maxUses != null && used != null && used >= maxUses) return false

		val myHpPercentage = 100 * myState.currentHealth / myState.maxHealth
		if (myHpPercentage > criteria.hpPercentageAtMost) return false
		if (myHpPercentage < criteria.hpPercentageAtLeast) return false
		// TODO targetHasEffect
		// TODO targetMissesEffect
		// TODO resistanceAtMost
		// TODO resistanceAtLeast
		val myElement = criteria.myElement
		if (myElement != null && myState.element !== myElement) return false

		if (criteria.freeAllySlots > 0 && state.enemyStates.count { it != null } < criteria.freeAllySlots) return false
		// TODO targetFainted

		if (!criteria.canUseOnOddTurns && myState.totalSpentTurns % 2 == 1) return false
		if (!criteria.canUseOnEvenTurns && myState.totalSpentTurns % 2 == 0) return false
		if (!criteria.canRepeat && myState.lastStrategy === pool) return false

		return true
	}
}
