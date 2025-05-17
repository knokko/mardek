package mardek.state.ingame.battle

import mardek.content.skill.ReactionSkill
import mardek.content.skill.ReactionSkillType
import mardek.content.stats.CombatStat
import mardek.content.stats.StatusEffect
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

class MoveResultCalculator(
	private val state: BattleState,
	private val context: BattleUpdateContext,
) {

	fun computeBasicAttackResult(
		attacker: CombatantReference, target: CombatantReference, passedChallenge: Boolean
	): MoveResult {
		val attack = attacker.getStat(CombatStat.Attack, context)
		val defense = target.getStat(CombatStat.MeleeDefense, context)
		var sound = context.sounds.battle.punch

		val attackingPlayer = if (attacker.isPlayer) state.players[attacker.index]!! else null
		val attackerState = if (attackingPlayer != null) context.characterStates[attackingPlayer]!! else null
		val targetPlayer = if (target.isPlayer) state.players[target.index]!! else null
		val targetState = if (targetPlayer != null) context.characterStates[targetPlayer]!! else null

		val strength = attacker.getStat(CombatStat.Strength, context)
		var damage = max(0, attack - defense) * strength * (attacker.getLevel(context) + 5)

		val rawWeapon = attacker.getState().equipment[0]
		val weapon = rawWeapon?.equipment?.weapon
		var attackElement = (rawWeapon?.element) ?: context.physicalElement
		var hasSmitePlus = false
		var extraDamageFractionAttacker = 0f
		var extraDamageFractionTarget = 0f
		var attackerCreatureBonus = 0f
		var targetCreatureBonus = 0f
		var attackerEffectBonus = 0f
		var targetEffectBonus = 0f
		var extraFlatDamage = 0
		var hpDrain = 0f
		var mpAbsorption = 0f
		var criticalChance = 0
		var hitChance = 100

		for (effect in attacker.getState().statusEffects) {
			attackerEffectBonus += effect.meleeDamageModifier
			hitChance -= effect.missChance
		}

		for (effect in target.getState().statusEffects) {
			targetEffectBonus -= effect.meleeDamageReduction
		}

		val removeCandidateEffects = mutableMapOf<StatusEffect, Int>()
		for (effect in target.getState().statusEffects) {
			if (effect.disappearAfterHitChance > 0) {
				removeCandidateEffects[effect] = removeCandidateEffects.getOrDefault(effect, 0) + effect.disappearAfterHitChance
			}
		}

		val addCandidateEffects = mutableMapOf<StatusEffect, Int>()
		if (passedChallenge && attackerState != null) {
			for (skill in attackerState.toggledSkills) {
				if (skill !is ReactionSkill || skill.type != ReactionSkillType.MeleeAttack) continue
				if (skill.soulStrike) attackElement = attackingPlayer!!.element
				if (skill.smitePlus) hasSmitePlus = true
				extraDamageFractionAttacker += skill.addDamageFraction
				extraFlatDamage += skill.addFlatDamage
				attackerCreatureBonus += skill.getCreatureTypeBonus(target.getCreatureType())
				mpAbsorption += skill.absorbMp
				hitChance += skill.addAccuracy
				for (maybeRemove in skill.removeStatusEffects) {
					if (target.getState().statusEffects.contains(maybeRemove.effect)) {
						removeCandidateEffects[maybeRemove.effect] = removeCandidateEffects.getOrDefault(maybeRemove.effect, 0) + maybeRemove.chance
					}
				}
				for (maybeAdd in skill.addStatusEffects) {
					addCandidateEffects[maybeAdd.effect] = addCandidateEffects.getOrDefault(maybeAdd.effect, 0) + maybeAdd.chance
				}
			}
		}

		var attackElementBonus = 0f
		if (weapon != null) {
			criticalChance += weapon.critChance
			hitChance = weapon.hitChance
			hpDrain += weapon.hpDrain
			for (effective in weapon.effectiveAgainstElements) {
				if (effective.element === target.getState().element) attackElementBonus += effective.modifier
			}
			for (effective in weapon.effectiveAgainstCreatureTypes) {
				if (effective.type === target.getCreatureType()) attackerCreatureBonus += effective.modifier
			}
			for (addEffect in weapon.addEffects) {
				addCandidateEffects[addEffect.effect] = addCandidateEffects.getOrDefault(addEffect.effect, 0) + addEffect.chance
			}
			if (weapon.hitSound != null) sound = weapon.hitSound!!
			else if (weapon.type.soundEffect != null) sound = weapon.type.soundEffect!!
		}

		for (potentialEquipment in attacker.getState().equipment) {
			val equipment = potentialEquipment?.equipment ?: continue
			for (bonus in equipment.elementalBonuses) {
				if (bonus.element === attackElement) attackElementBonus += bonus.modifier
			}
		}

		if (passedChallenge && attackerState != null) {
			for (skill in attackerState.toggledSkills) {
				if (skill !is ReactionSkill || skill.type != ReactionSkillType.MeleeAttack) continue
				attackElementBonus += skill.getElementalBonus(attackElement)
				hitChance += skill.addAccuracy
			}
		}

		var elementalResistance = target.getResistance(attackElement, context)
		var hasSurvivor = false
		if (passedChallenge && targetState != null) {
			for (skill in targetState.toggledSkills) {
				if (skill !is ReactionSkill || skill.type != ReactionSkillType.MeleeDefense) continue
				elementalResistance -= skill.getElementalBonus(attackElement)
				hitChance += skill.addAccuracy // Will be negative for evasion skills
				extraFlatDamage += skill.addFlatDamage
				criticalChance += skill.addCritChance
				extraDamageFractionTarget += skill.addDamageFraction
				targetCreatureBonus += skill.getCreatureTypeBonus(attacker.getCreatureType())
				if (skill.survivor) hasSurvivor = true
			}
		}

		hitChance -= target.getStat(CombatStat.Evasion, context)
		val missed = hitChance <= Random.Default.nextInt(100)
		val criticalHit = criticalChance > Random.Default.nextInt(100)

		run {
			var floatDamage = damage.toDouble()
			floatDamage *= (1.0 + extraDamageFractionAttacker)
			floatDamage *= (1.0 + attackerCreatureBonus)
			floatDamage *= (1.0 + attackerEffectBonus)
			floatDamage *= (1.0 + attackElementBonus)

			floatDamage *= (1.0 + extraDamageFractionTarget)
			floatDamage *= (1.0 + targetCreatureBonus)
			floatDamage *= (1.0 + targetEffectBonus)
			floatDamage *= (1.0 - elementalResistance)

			if (criticalHit) {
				floatDamage *= 2.0
				sound = context.sounds.battle.critical
			}

			if (hasSmitePlus) TODO()

			damage = (floatDamage * 0.02).roundToInt()
		}

		if (damage > 0) damage = max(0, damage + extraFlatDamage)

		var restoreAttackerHealth = (hpDrain * damage).roundToInt()
		if (restoreAttackerHealth > 0 && target.getCreatureType().countersHealthDrain) restoreAttackerHealth = -restoreAttackerHealth
		if (restoreAttackerHealth > 0) restoreAttackerHealth = min(restoreAttackerHealth, target.getState().currentHealth)
		if (restoreAttackerHealth < 0) {
			restoreAttackerHealth = -min(-restoreAttackerHealth, attacker.getState().currentHealth)
			damage += restoreAttackerHealth
		}

		if (hasSurvivor && target.getState().currentHealth > 1) {
			damage = min(damage, target.getState().currentHealth - 1)
		}

		var restoreAttackerMana = (mpAbsorption * damage).roundToInt()
		if (restoreAttackerMana > 0) restoreAttackerMana = min(restoreAttackerMana, target.getState().currentHealth)
		if (restoreAttackerMana < 0) restoreAttackerMana = -min(-restoreAttackerMana, attacker.getState().currentMana)

		val removedEffects = removeCandidateEffects.filter {
			val effect = it.key
			val chance = it.value
			if (chance <= Random.Default.nextInt(100)) return@filter false
			if (target.getAutoEffects(context).contains(effect)) return@filter false
			true
		}.keys
		val addedEffects = addCandidateEffects.filter {
			val effect = it.key
			val chance = it.value
			if (chance <= Random.Default.nextInt(100)) return@filter false
			if (target.getState().statusEffects.contains(effect) && !removedEffects.contains(effect)) return@filter false
			val resistance = target.getResistance(effect, context)
			if (resistance > Random.Default.nextInt(100)) return@filter false
			true
		}.keys

		if (missed) sound = context.sounds.battle.miss

		return MoveResult(
			element = attackElement,
			sound = sound,
			damage = damage,
			missed = missed,
			criticalHit = criticalHit,
			addedEffects = addedEffects,
			removedEffects = removedEffects,
			addedStatModifiers = emptyMap(),
			restoreAttackerHealth = restoreAttackerHealth,
			restoreAttackerMana = restoreAttackerMana,
		)
	}
}
