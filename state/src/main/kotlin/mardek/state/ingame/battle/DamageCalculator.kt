package mardek.state.ingame.battle

import mardek.content.skill.PassiveSkill
import mardek.content.skill.ReactionSkill
import mardek.content.skill.ReactionSkillType
import mardek.content.stats.CombatStat
import kotlin.math.roundToInt

class DamageCalculator(
	private val state: BattleState,
	private val context: BattleUpdateContext,
) {

	fun computeBasicAttackDamage(
		attacker: CombatantReference, target: CombatantReference, passedChallenge: Boolean
	): Int {
		// TODO accuracy & critical hits
		val attack = attacker.getStat(CombatStat.Attack, context)
		val defense = target.getStat(CombatStat.MeleeDefense, context)
		if (attack <= defense) return 0

		val attackingPlayer = if (attacker.isPlayer) state.players[attacker.index]!! else null
		val attackerState = if (attackingPlayer != null) context.characterStates[attackingPlayer]!! else null
		val targetPlayer = if (target.isPlayer) state.players[target.index]!! else null
		val targetState = if (targetPlayer != null) context.characterStates[targetPlayer]!! else null

		val strength = attacker.getStat(CombatStat.Strength, context)
		var damage = (attack - defense) * strength * (attacker.getLevel(context) + 5)

		var element = (attacker.getState().equipment[0]?.element) ?: context.physicalElement
		if (passedChallenge && attackerState != null) {
			for (skill in attackerState.toggledSkills) {
				if (skill !is ReactionSkill || skill.type != ReactionSkillType.MeleeAttack) continue
				if (skill.soulStrike) element = attackingPlayer!!.element
			}
		}

		var extraDamageFraction = 0f
		var elementalBonus = 0f
		var creatureBonus = 0f
		var extraFlatDamage = 0
		if (passedChallenge && attackerState != null) {
			for (skill in attackerState.toggledSkills) {
				if (skill !is ReactionSkill || skill.type != ReactionSkillType.MeleeAttack) continue
				extraDamageFraction += skill.addDamageFraction
				extraFlatDamage += skill.addFlatDamage
				if (skill.smitePlus) TODO()
				elementalBonus += skill.getElementalBonus(element)
				creatureBonus += skill.getCreatureTypeBonus(ehm)
			}
		}

		var elementalResistance = target.getResistance(element, context)
		if (passedChallenge && targetState != null) {
			for (skill in targetState.toggledSkills) {
				if (skill !is ReactionSkill || skill.type != ReactionSkillType.MeleeDefense) continue
				elementalResistance -= skill.getElementalBonus(element)
			}
		}

		damage = (damage * (1f - elementalResistance)).roundToInt()
		return damage / 50
	}
}
