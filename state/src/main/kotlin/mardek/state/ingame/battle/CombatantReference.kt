package mardek.state.ingame.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.characters.PlayableCharacter
import mardek.content.skill.PassiveSkill
import mardek.content.stats.CombatStat
import mardek.content.stats.Element
import mardek.content.stats.StatusEffect
import mardek.state.ingame.characters.CharacterState

@BitStruct(backwardCompatible = true)
class CombatantReference(

	@BitField(id = 0)
	val isPlayer: Boolean,

	@BitField(id = 1)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 4)
	val index: Int,

	@BitField(id = 2)
	@ReferenceField(stable = false, label = "battle state")
	private val battleState: BattleState
) {

	internal constructor() : this(false, 0, BattleState())

	override fun equals(other: Any?) = other is CombatantReference && this.isPlayer == other.isPlayer &&
			this.index == other.index && this.battleState === other.battleState

	override fun hashCode() = 13 * isPlayer.hashCode() + index

	override fun toString() = "CombatantReference(${if (isPlayer) "player" else "enemy"} $index)"

	fun isAlive() = if (isPlayer) {
		val state = battleState.playerStates[index]
		state != null && state.currentHealth > 0
	} else battleState.enemyStates[index] != null

	fun getStat(stat: CombatStat, characterStates: Map<PlayableCharacter, CharacterState>) = if (isPlayer) {
		val player = battleState.players[index]!!
		val combatState = battleState.playerStates[index]!!
		val extra = combatState.statModifiers[stat] ?: 0

		val characterState = characterStates[player]!!
		characterState.computeStatValue(player.baseStats, combatState.statusEffects, stat) + extra
	} else {
		val monster = battleState.enemies[index]!!.monster
		val state = battleState.enemyStates[index]!!
		val base = monster.baseStats[stat] ?: 0
		val extra = state.statModifiers[stat] ?: 0
		base + extra
	}

	fun getState() = if (isPlayer) battleState.playerStates[index]!!
	else battleState.enemyStates[index]!!

	fun getResistance(element: Element, characterStates: Map<PlayableCharacter, CharacterState>): Float {
		var resistance = 0f
		val combatState = getState()
		for (item in combatState.equipment) {
			if (item?.equipment != null) resistance += item.equipment!!.resistances.get(element)
		}
		for (effect in combatState.statusEffects) resistance += effect.resistances.get(element)

		if (isPlayer) {
			if (element === combatState.element) resistance += 0.2f
			if (element === combatState.element.weakAgainst) resistance -= 0.2f

			val characterState = characterStates[battleState.players[index]]!!
			for (skill in characterState.toggledSkills) {
				if (skill is PassiveSkill) resistance += skill.resistances.get(element)
			}
		} else {
			val monster = battleState.enemies[index]!!.monster
			val shiftResistances = monster.elementalShiftResistances[combatState.element]
			resistance += shiftResistances?.get(element) ?: monster.resistances.get(element)
		}

		return resistance
	}

	fun getResistance(effect: StatusEffect, characterStates: Map<PlayableCharacter, CharacterState>): Int {
		var resistance = 0
		val combatState = getState()
		for (item in combatState.equipment) {
			if (item?.equipment != null) resistance += item.equipment!!.resistances.get(effect)
		}
		for (otherEffect in combatState.statusEffects) resistance += otherEffect.resistances.get(effect)

		if (isPlayer) {
			val characterState = characterStates[battleState.players[index]]!!
			for (skill in characterState.toggledSkills) {
				if (skill is PassiveSkill) resistance += skill.resistances.get(effect)
			}
		} else {
			val monster = battleState.enemies[index]!!.monster
			val shiftResistances = monster.elementalShiftResistances[combatState.element]
			resistance += shiftResistances?.get(effect) ?: monster.resistances.get(effect)
		}

		return resistance
	}
}
