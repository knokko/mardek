package mardek.state.ingame.battle

import mardek.content.battle.StrategyCriteria
import mardek.content.battle.StrategyEntry
import mardek.content.battle.StrategyPool
import mardek.content.battle.StrategyTarget
import kotlin.random.Random

class MonsterStrategyCalculator(
	private val state: BattleState,
	private val context: BattleUpdateContext,
) {
	private val myState = state.onTurn as MonsterCombatantState
	private val monster = myState.monster

	fun determineNextMove(): BattleMove {
		val nextMove = determineNextMoveRaw()
		myState.lastMove = nextMove
		return nextMove
	}

	private fun determineNextMoveRaw(): BattleMove {
		val pool = determineNextPool() ?: return BattleMoveWait()

		myState.usedStrategies[pool] = myState.usedStrategies.getOrDefault(pool, 0) + 1

		val entry = chooseEntry(pool)

		val item = entry.item
		if (item != null) {
			val target = chooseSingleTarget(entry, pool.criteria, "item ${item.flashName}")
			return BattleMoveItem(item, target)
		}

		val skill = entry.skill
		if (skill != null) {
			val skillTarget = when (entry.target) {
				StrategyTarget.Self -> BattleSkillTargetSingle(state.onTurn!!)
				StrategyTarget.AllEnemies -> BattleSkillTargetAllEnemies
				StrategyTarget.AllAllies -> BattleSkillTargetAllAllies
				else -> BattleSkillTargetSingle(chooseSingleTarget(entry, pool.criteria, "skill ${skill.name}"))
			}
			val nextElement = if (skill.changeElement) monster.elementalShiftResistances.keys.random() else null
			return BattleMoveSkill(skill, skillTarget, nextElement)
		}

		val target = chooseSingleTarget(entry, pool.criteria, "basic attack")
		return BattleMoveBasicAttack(target)
	}

	private fun chooseSingleTarget(
		entry: StrategyEntry, criteria: StrategyCriteria, description: String
	): CombatantState {
		val potentialTargets = when (entry.target) {
			StrategyTarget.Self -> listOf(state.onTurn!!)
			StrategyTarget.AnyAlly -> if (myState.isOnPlayerSide) state.allPlayers() else state.livingOpponents()
			StrategyTarget.AnyEnemy -> if (myState.isOnPlayerSide) state.livingOpponents() else state.allPlayers()
			else -> throw IllegalStateException("Unexpected strategy target ${entry.target} for single-target $description")
		}
		return potentialTargets.filter { areCriteriaSatisfied(criteria, it) }.random()
	}

	private fun chooseEntry(pool: StrategyPool): StrategyEntry {
		val entries = pool.entries.filter { canChooseEntry(pool.criteria, it) }
		val totalChance = entries.sumOf { it.chance }
		if (totalChance == 0) return entries.random() else {
			val selectedChance = Random.Default.nextInt(totalChance)
			var currentChance = 0
			for (entry in entries) {
				if (selectedChance < currentChance + entry.chance) return entry
				currentChance += entry.chance
			}
			throw Error("Should not happen: entries are $entries out of ${pool.entries}")
		}
	}

	private fun determineNextPool(): StrategyPool? {
		for (pool in monster.strategies) {
			if (!areCriteriaSatisfied(pool)) continue
			val totalChance = pool.entries.sumOf { it.chance }
			if (totalChance <= Random.Default.nextInt(100)) continue
			return pool
		}

		return null
	}

	private fun areCriteriaSatisfied(pool: StrategyPool): Boolean {
		val criteria = pool.criteria

		val maxUses = criteria.maxUses
		val used = myState.usedStrategies[pool]
		if (maxUses != null && used != null && used >= maxUses) return false

		val myHpPercentage = 100 * myState.currentHealth / myState.maxHealth
		if (myHpPercentage > criteria.myHpPercentageAtMost) return false
		if (myHpPercentage < criteria.myHpPercentageAtLeast) return false

		val myElement = criteria.myElement
		if (myElement != null && myState.element !== myElement) return false

		if (criteria.freeAllySlots > 0) {
			if (myState.isOnPlayerSide && criteria.freeAllySlots != state.players.count { it == null }) return false
			if (!myState.isOnPlayerSide && criteria.freeAllySlots != state.opponents.size - state.livingOpponents().size) return false
		}

		if (!criteria.canUseOnOddTurns && myState.totalSpentTurns % 2 == 1) return false
		if (!criteria.canUseOnEvenTurns && myState.totalSpentTurns % 2 == 0) return false

		for (entry in pool.entries) {
			val allEnemies = if (myState.isOnPlayerSide) state.livingOpponents() else state.allPlayers()
			val allAllies = if (myState.isOnPlayerSide) state.allPlayers() else state.livingOpponents()
			val potentialTargets = when (entry.target) {
				StrategyTarget.AnyEnemy -> allEnemies
				StrategyTarget.AllEnemies -> allEnemies
				StrategyTarget.Self -> listOf(state.onTurn!!)
				StrategyTarget.AnyAlly -> allAllies
				StrategyTarget.AllAllies -> allAllies
			}
			if (potentialTargets.none { areCriteriaSatisfied(pool.criteria, it) }) return false
		}

		return pool.entries.any { canChooseEntry(criteria, it) }
	}

	private fun canChooseEntry(criteria: StrategyCriteria, entry: StrategyEntry): Boolean {
		val lastMove = myState.lastMove
		if (!criteria.canRepeat) {
			if (entry.skill != null && lastMove is BattleMoveSkill && entry.skill === lastMove.skill) return false
			if (entry.item != null && lastMove is BattleMoveItem && entry.item === lastMove.item) return false
			if (entry.skill == null && entry.item == null && lastMove is BattleMoveBasicAttack) return false
		}

		if (entry.skill != null && entry.skill!!.manaCost > myState.currentMana) return false

		return true
	}

	private fun areCriteriaSatisfied(criteria: StrategyCriteria, target: CombatantState): Boolean {
		if (criteria.targetFainted) return !target.isAlive()
		else if (!target.isAlive()) return false

		val targetHpPercentage = 100 * target.currentHealth / target.maxHealth
		if (targetHpPercentage > criteria.targetHpPercentageAtMost) return false
		if (targetHpPercentage < criteria.targetHpPercentageAtLeast) return false

		val targetHasEffect = criteria.targetHasEffect
		if (targetHasEffect != null && !target.statusEffects.contains(targetHasEffect)) return false

		val targetMissesEffect = criteria.targetMissesEffect
		if (targetMissesEffect != null && target.statusEffects.contains(targetMissesEffect)) return false

		val maxResistance = criteria.resistanceAtMost
		if (maxResistance != null && target.getResistance(maxResistance.element, context) > maxResistance.modifier) {
			return false
		}

		val minResistance = criteria.resistanceAtLeast
		if (minResistance != null && target.getResistance(minResistance.element, context) < minResistance.modifier) {
			return false
		}

		return true
	}
}
