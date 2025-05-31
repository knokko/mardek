package mardek.state.ingame.battle

import mardek.content.audio.SoundEffect
import mardek.content.skill.ActiveSkill
import mardek.content.skill.ReactionSkill
import mardek.content.skill.ReactionSkillType
import mardek.content.skill.SkillSpiritModifier
import mardek.content.stats.CombatStat
import mardek.content.stats.Element
import mardek.content.stats.StatusEffect
import kotlin.collections.set
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

class MoveResultCalculator(
	private val state: BattleState,
	private val context: BattleUpdateContext,
) {

	private fun computeMeleeAttackResult(
		attacker: CombatantState, target: CombatantState, passedChallenge: Boolean,
		baseFlatDamage: Int, attackValue: Int, basicElementalBonus: Float,
		basicSound: SoundEffect?, basicHitChance: Int, basicCritChance: Int,
		basicHealthDrain: Float, basicManaDrain: Float, attackElement: Element,
		basicAddStatModifiers: MutableMap<CombatStat, Int>,
		basicAddEffects: MutableMap<StatusEffect, Int>,
		basicRemoveEffects: MutableMap<StatusEffect, Int>,
	): MoveResult {
		val defense = target.getStat(CombatStat.MeleeDefense, context)
		var sound = basicSound

		val strength = attacker.getStat(CombatStat.Strength, context)
		var damage = max(0, attackValue - defense) * strength * (attacker.getLevel(context) + 5)

		val rawWeapon = attacker.getEquipment(context)[0]
		val weapon = rawWeapon?.equipment?.weapon
		var hasSmitePlus = false
		var extraDamageFractionAttacker = 0f
		var extraDamageFractionTarget = 0f
		var attackerCreatureBonus = 0f
		var targetCreatureBonus = 0f
		var attackerEffectBonus = 0f
		var targetEffectBonus = 0f
		var extraFlatDamage = baseFlatDamage
		var hpDrain = basicHealthDrain
		var mpAbsorption = basicManaDrain
		var criticalChance = basicCritChance
		var hitChance = basicHitChance

		for (effect in attacker.statusEffects) {
			attackerEffectBonus += effect.meleeDamageModifier
			hitChance -= effect.missChance
		}

		for (effect in target.statusEffects) {
			targetEffectBonus -= effect.meleeDamageReduction
		}

		val removeCandidateEffects = basicRemoveEffects
		for (effect in target.statusEffects) {
			if (effect.disappearAfterHitChance > 0) {
				removeCandidateEffects[effect] =
					removeCandidateEffects.getOrDefault(effect, 0) + effect.disappearAfterHitChance
			}
		}

		val addCandidateEffects = basicAddEffects
		var attackElementBonus = basicElementalBonus
		if (passedChallenge) {
			for (skill in attacker.getToggledSkills(context)) {
				if (skill !is ReactionSkill || skill.type != ReactionSkillType.MeleeAttack) continue
				if (skill.smitePlus) hasSmitePlus = true
				extraDamageFractionAttacker += skill.addDamageFraction
				extraFlatDamage += skill.addFlatDamage
				attackElementBonus += skill.getElementalBonus(attackElement)
				attackerCreatureBonus += skill.getCreatureTypeBonus(target.getCreatureType())
				hpDrain += skill.drainHp
				mpAbsorption += skill.absorbMp
				hitChance += skill.addAccuracy
				for (maybeRemove in skill.removeStatusEffects) {
					if (target.statusEffects.contains(maybeRemove.effect)) {
						removeCandidateEffects[maybeRemove.effect] =
							removeCandidateEffects.getOrDefault(maybeRemove.effect, 0) + maybeRemove.chance
					}
				}
				for (maybeAdd in skill.addStatusEffects) {
					addCandidateEffects[maybeAdd.effect] =
						addCandidateEffects.getOrDefault(maybeAdd.effect, 0) + maybeAdd.chance
				}
			}
		}

		if (weapon != null) {
			for (effective in weapon.effectiveAgainstElements) {
				if (effective.element === target.element) attackElementBonus += effective.modifier
			}
			for (effective in weapon.effectiveAgainstCreatureTypes) {
				if (effective.type === target.getCreatureType()) attackerCreatureBonus += effective.modifier
			}
			for (addEffect in weapon.addEffects) {
				addCandidateEffects[addEffect.effect] =
					addCandidateEffects.getOrDefault(addEffect.effect, 0) + addEffect.chance
			}
		}

		if (attacker is MonsterCombatantState) {
			for (addEffect in attacker.monster.attackEffects) {
				addCandidateEffects[addEffect.effect] =
					addCandidateEffects.getOrDefault(addEffect.effect, 0) + addEffect.chance
			}
		}

		for (potentialEquipment in attacker.getEquipment(context)) {
			val equipment = potentialEquipment?.equipment ?: continue
			for (bonus in equipment.elementalBonuses) {
				if (bonus.element === attackElement) attackElementBonus += bonus.modifier
			}
		}

		var elementalResistance = target.getResistance(attackElement, context)
		var hasSurvivor = false
		if (passedChallenge) {
			for (skill in target.getToggledSkills(context)) {
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
			floatDamage *= Random.Default.nextDouble(0.9, 1.1)

			floatDamage *= max(0.0, 1.0 + extraDamageFractionTarget)
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

		if (damage > 0 && extraFlatDamage < 0) damage = max(0, damage + extraFlatDamage)

		var restoreAttackerHealth = (hpDrain * damage).roundToInt()
		if (restoreAttackerHealth > 0) restoreAttackerHealth = min(restoreAttackerHealth, target.currentHealth)
		if (restoreAttackerHealth < 0) restoreAttackerHealth = -min(-restoreAttackerHealth, attacker.currentHealth)

		if (hasSurvivor && target.currentHealth > 1) damage = min(damage, target.currentHealth - 1)

		var restoreAttackerMana = (mpAbsorption * damage).roundToInt()
		if (restoreAttackerMana > 0) restoreAttackerMana = min(restoreAttackerMana, target.currentHealth)
		if (restoreAttackerMana < 0) restoreAttackerMana = -min(-restoreAttackerMana, attacker.currentMana)

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
			if (target.statusEffects.contains(effect) && !removedEffects.contains(effect)) return@filter false
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
			addedStatModifiers = basicAddStatModifiers,
			restoreAttackerHealth = restoreAttackerHealth,
			restoreAttackerMana = restoreAttackerMana,
		)
	}

	fun computeMeleeSkillResult(
		skill: ActiveSkill, attacker: CombatantState, target: CombatantState, passedChallenge: Boolean
	): MoveResult {
		val rawWeapon = attacker.getEquipment(context)[0]
		val weapon = rawWeapon?.equipment?.weapon

		val skillDamage = skill.damage!!

		var extraFlatDamage = 0
		var elementalBonus = 0f
		var attackValue = skillDamage.flatAttackValue
		attackValue += (skillDamage.weaponModifier * attacker.getStat(CombatStat.Attack, context)).roundToInt()
		attackValue += (skillDamage.levelModifier * attacker.getLevel(context))

		if (skillDamage.ignoresDefense) TODO("ignores defense")
		if (skillDamage.ignoresShield) TODO("ignores shield")
		if (skillDamage.spiritModifier != SkillSpiritModifier.None) TODO("spirit modifier")
		for (bonus in skillDamage.bonusAgainstElements) {
			if (bonus.element == skill.element) elementalBonus += bonus.modifier
		}
		if (skillDamage.moneyModifier != 0f) TODO("money modifier")
		if (skillDamage.gemModifier != 0f) TODO("gem modifier")
		extraFlatDamage += (skillDamage.lostHealthModifier * (attacker.maxHealth - attacker.currentHealth)).roundToInt()
		if (skillDamage.statusEffectModifier != 0f) TODO("status effect modifier")
		if (skillDamage.killCountModifier != 0f) TODO("kill count modifier")
		extraFlatDamage += skillDamage.hardcodedDamage
		extraFlatDamage += (skillDamage.remainingTargetHpModifier * target.currentHealth).roundToInt()
		if (skillDamage.potionModifier != 0f) TODO("potion modifier")
		if (skillDamage.crescendoModifier != 0f) TODO("crescendo modifier")

		val addStatModifiers = mutableMapOf<CombatStat, Int>()
		for (modifier in skill.statModifiers) {
			val adder = Random.Default.nextInt(modifier.minAdder, modifier.maxAdder + 1)
			addStatModifiers[modifier.stat] = addStatModifiers.getOrDefault(modifier.stat, 0) + adder
		}

		val addEffects = mutableMapOf<StatusEffect, Int>()
		for (effect in skill.addStatusEffects) {
			addEffects[effect.effect] = addEffects.getOrDefault(effect.effect, 0) + effect.chance
		}

		val removeEffects = mutableMapOf<StatusEffect, Int>()
		for (effect in skill.removeStatusEffects) {
			removeEffects[effect.effect] = removeEffects.getOrDefault(effect.effect, 0) + effect.chance
		}

		if (skill.revive != 0f) TODO("revive")
		if (skill.changeElement) TODO("change element")

		val sound = if (skill.particleEffect?.initialSound != null) null else
			weapon?.hitSound ?: weapon?.type?.soundEffect ?: context.sounds.battle.punch

		return computeMeleeAttackResult(
			attacker, target, passedChallenge,
			baseFlatDamage = extraFlatDamage,
			attackValue = attackValue,
			basicElementalBonus = elementalBonus,
			basicSound = sound,
			basicHitChance = skill.accuracy,
			basicCritChance = skillDamage.critChance ?: weapon?.critChance ?: 0,
			basicHealthDrain = skill.healthDrain,
			basicManaDrain = 0f,
			attackElement = skill.element,
			basicAddStatModifiers = addStatModifiers,
			basicAddEffects = addEffects,
			basicRemoveEffects = removeEffects,
		)
	}

	fun computeBasicAttackResult(
		attacker: CombatantState, target: CombatantState, passedChallenge: Boolean
	): MoveResult {
		val rawWeapon = attacker.getEquipment(context)[0]
		val weapon = rawWeapon?.equipment?.weapon

		val hitChance: Int
		var critChance: Int
		val healthDrain: Float
		val manaDrain: Float
		var sound = context.sounds.battle.punch
		var attackElement: Element
		if (weapon != null) {
			hitChance = weapon.hitChance
			critChance = weapon.critChance
			healthDrain = weapon.hpDrain
			manaDrain = weapon.mpDrain
			attackElement = rawWeapon.element ?: context.physicalElement
			if (weapon.hitSound != null) sound = weapon.hitSound!!
			else if (weapon.type.soundEffect != null) sound = weapon.type.soundEffect!!
		} else {
			hitChance = 100
			critChance = 0
			healthDrain = 0f
			manaDrain = 0f
			attackElement = context.physicalElement
		}

		if (attacker is MonsterCombatantState) critChance += attacker.monster.critChance

		if (passedChallenge && attacker.getToggledSkills(context).any {
			it is ReactionSkill && it.soulStrike && it.type == ReactionSkillType.MeleeAttack
		}) {
			attackElement = attacker.element
		}

		return computeMeleeAttackResult(
			attacker, target, passedChallenge,
			baseFlatDamage = 0,
			attackValue = attacker.getStat(CombatStat.Attack, context),
			basicElementalBonus = 0f,
			basicSound = sound,
			basicHitChance = hitChance,
			basicCritChance = critChance,
			basicHealthDrain = healthDrain,
			basicManaDrain = manaDrain,
			attackElement = attackElement,
			basicAddStatModifiers = mutableMapOf(),
			basicAddEffects = mutableMapOf(),
			basicRemoveEffects = mutableMapOf(),
		)
	}
}
