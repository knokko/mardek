package mardek.state.ingame.battle

import mardek.content.battle.StrategyCriteria
import mardek.content.battle.StrategyEntry
import mardek.content.battle.StrategyPool
import mardek.content.battle.StrategyTarget
import mardek.content.characters.PlayableCharacter
import mardek.state.ingame.characters.CharacterState
import kotlin.random.Random

class MonsterStrategyCalculator(
	private val state: BattleState,
	private val characterStates: Map<PlayableCharacter, CharacterState>,
) {
	private val enemyIndex = state.onTurn!!.index
	private val monster = state.enemies[enemyIndex]!!.monster
	private val myState = state.enemyStates[enemyIndex]!!

	init {
		if (state.onTurn!!.isPlayer) throw IllegalStateException("A monster must be on turn")
	}

	fun determineNextMove(): BattleMove {
		val nextMove = determineNextMoveRaw()
		myState.lastMove = nextMove
		return nextMove
	}

	private fun determineNextMoveRaw(): BattleMove {
		val pool = determineNextPool() ?: return BattleMoveWait

		myState.usedStrategies[pool] = myState.usedStrategies.getOrDefault(pool, 0) + 1

		val entry = chooseEntry(pool)

		val item = entry.item
		if (item != null) {
			return BattleMoveItem(item, chooseSingleTarget(entry, pool.criteria, "item ${item.flashName}"))
		}

		val skill = entry.skill
		if (skill != null) {
			val skillTarget = when (entry.target) {
				StrategyTarget.Self -> BattleSkillTargetSingle(state.onTurn!!)
				StrategyTarget.AllPlayers -> BattleSkillTargetAllEnemies
				StrategyTarget.AllAllies -> BattleSkillTargetAllAllies
				else -> BattleSkillTargetSingle(chooseSingleTarget(entry, pool.criteria, "skill ${skill.name}"))
			}
			val nextElement = if (skill.changeElement) monster.elementalShiftResistances.keys.random() else null
			return BattleMoveSkill(skill, skillTarget, nextElement)
		}

		return BattleMoveBasicAttack(chooseSingleTarget(entry, pool.criteria, "basic attack"))
	}

	private fun chooseSingleTarget(
		entry: StrategyEntry, criteria: StrategyCriteria, description: String
	): CombatantReference {
		val potentialTargets = when (entry.target) {
			StrategyTarget.Self -> listOf(state.onTurn!!)
			StrategyTarget.AnyAlly -> state.livingEnemies()
			StrategyTarget.AnyPlayer -> state.allPlayers()
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

		if (criteria.freeAllySlots > 0 && criteria.freeAllySlots != state.enemyStates.count { it == null }) return false

		if (!criteria.canUseOnOddTurns && myState.totalSpentTurns % 2 == 1) return false
		if (!criteria.canUseOnEvenTurns && myState.totalSpentTurns % 2 == 0) return false

		for (entry in pool.entries) {
			val potentialTargets = when (entry.target) {
				StrategyTarget.AnyPlayer -> state.allPlayers()
				StrategyTarget.AllPlayers -> state.allPlayers()
				StrategyTarget.Self -> listOf(state.onTurn!!)
				StrategyTarget.AnyAlly -> state.livingEnemies()
				StrategyTarget.AllAllies -> state.livingEnemies()
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

	private fun areCriteriaSatisfied(criteria: StrategyCriteria, target: CombatantReference): Boolean {
		if (criteria.targetFainted) return !target.isAlive()
		else if (!target.isAlive()) return false
		val targetState = target.getState()

		val targetHpPercentage = 100 * targetState.currentHealth / targetState.maxHealth
		if (targetHpPercentage > criteria.targetHpPercentageAtMost) return false
		if (targetHpPercentage < criteria.targetHpPercentageAtLeast) return false

		val targetHasEffect = criteria.targetHasEffect
		if (targetHasEffect != null && !targetState.statusEffects.contains(targetHasEffect)) return false

		val targetMissesEffect = criteria.targetMissesEffect
		if (targetMissesEffect != null && targetState.statusEffects.contains(targetMissesEffect)) return false

		val maxResistance = criteria.resistanceAtMost
		if (maxResistance != null && target.getResistance(maxResistance.element, characterStates) > maxResistance.modifier) {
			return false
		}

		val minResistance = criteria.resistanceAtLeast
		if (minResistance != null && target.getResistance(minResistance.element, characterStates) < minResistance.modifier) {
			return false
		}

		return true
	}
}
