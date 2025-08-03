package mardek.state.ingame.battle

import mardek.content.audio.SoundEffect
import mardek.content.inventory.Item
import mardek.content.skill.ActiveSkill
import mardek.content.skill.ReactionSkill
import mardek.content.skill.ReactionSkillType
import mardek.content.skill.SkillSpiritModifier
import mardek.content.stats.CombatStat
import mardek.content.stats.Element
import mardek.content.stats.PossibleStatusEffect
import mardek.content.stats.StatModifierRange
import mardek.content.stats.StatusEffect
import kotlin.collections.set
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

class MoveResultCalculator(private val context: BattleUpdateContext) {

	private fun computeAttackResult(
		attacker: CombatantState, target: CombatantState, passedChallenge: Boolean,
		multiplierStat: CombatStat?, defenseStat: CombatStat?, isMelee: Boolean, isHealing: Boolean,
		attackReactionType: ReactionSkillType, defenseReactionType: ReactionSkillType,
		baseFlatDamage: Int, attackValue: Int, basicElementalBonus: Float,
		basicHitChance: Int, basicCritChance: Int, applyDamageSplit: Boolean,
		basicHealthDrain: Float, basicManaDrain: Float, attackElement: Element,
		basicAddStatModifiers: MutableMap<CombatStat, Int>,
		basicAddEffects: MutableMap<StatusEffect, Int>,
		basicRemoveEffects: MutableMap<StatusEffect, Int>,
	): Entry {
		val defense = if (defenseStat != null && !isHealing) target.getStat(defenseStat, context) else 0

		val multiplier = if (multiplierStat != null) attacker.getStat(multiplierStat, context) else 1
		var damage = max(0, attackValue - defense) * multiplier * (attacker.getLevel(context) + 5)

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
			if (isMelee) {
				attackerEffectBonus += effect.meleeDamageModifier
				hitChance -= effect.missChance
			}
		}

		val removeCandidateEffects = basicRemoveEffects
		for (effect in target.statusEffects) {
			targetEffectBonus -= if (isMelee) effect.meleeDamageReduction else effect.rangedDamageReduction
			if (isMelee && effect.disappearAfterHitChance > 0) {
				removeCandidateEffects[effect] =
					removeCandidateEffects.getOrDefault(effect, 0) + effect.disappearAfterHitChance
			}
		}

		val addCandidateEffects = basicAddEffects
		var attackElementBonus = basicElementalBonus
		if (passedChallenge) {
			for (skill in attacker.getToggledSkills(context)) {
				if (skill !is ReactionSkill || skill.type != attackReactionType) continue
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

		if (attacker is MonsterCombatantState && isMelee) {
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
		if (passedChallenge && !isHealing) {
			for (skill in target.getToggledSkills(context)) {
				if (skill !is ReactionSkill || skill.type != defenseReactionType) continue
				elementalResistance -= skill.getElementalBonus(attackElement)
				hitChance += skill.addAccuracy // Will be negative for evasion skills
				extraFlatDamage += skill.addFlatDamage
				criticalChance += skill.addCritChance
				extraDamageFractionTarget += skill.addDamageFraction
				targetCreatureBonus += skill.getCreatureTypeBonus(attacker.getCreatureType())
				if (skill.survivor) hasSurvivor = true
			}
		}

		if (isMelee) hitChance -= target.getStat(CombatStat.Evasion, context)
		val missed = hitChance <= Random.Default.nextInt(100)
		val criticalHit = criticalChance > Random.Default.nextInt(100)

		run {
			var floatDamage = damage.toDouble()
			floatDamage *= (1.0 + extraDamageFractionAttacker)
			floatDamage *= (1.0 + attackerCreatureBonus)
			floatDamage *= (1.0 + attackerEffectBonus)
			floatDamage *= (1.0 + attackElementBonus)
			floatDamage *= if (isMelee) Random.Default.nextDouble(0.9, 1.1)
			else Random.Default.nextDouble(0.7, 1.3)

			if (!isHealing) {
				floatDamage *= max(0.0, 1.0 + extraDamageFractionTarget)
				floatDamage *= (1.0 + targetCreatureBonus)
				floatDamage *= (1.0 + targetEffectBonus)
				floatDamage *= (1.0 - elementalResistance)
			}

			if (criticalHit) floatDamage *= 2.0

			if (hasSmitePlus) TODO()

			if (applyDamageSplit) floatDamage /= 2.0
			damage = (floatDamage * 0.02).roundToInt()
		}

		if (damage > 0 && extraFlatDamage < 0) damage = max(0, damage + extraFlatDamage)
		else damage += extraFlatDamage
		if (isHealing) damage = -damage

		var restoreAttackerHealth = (hpDrain * damage).roundToInt()
		if (restoreAttackerHealth > 0) restoreAttackerHealth = min(restoreAttackerHealth, target.currentHealth)
		if (restoreAttackerHealth < 0) restoreAttackerHealth = -min(-restoreAttackerHealth, attacker.currentHealth)

		if (hasSurvivor && target.currentHealth > 1) damage = min(damage, target.currentHealth - 1)

		var restoreAttackerMana = (mpAbsorption * damage).roundToInt()
		if (restoreAttackerMana > 0) restoreAttackerMana = min(restoreAttackerMana, target.currentHealth)
		if (restoreAttackerMana < 0) restoreAttackerMana = -min(-restoreAttackerMana, attacker.currentMana)

		val removedEffects = determineRemovedEffects(removeCandidateEffects, target)
		val addedEffects = determineAddedEffects(addCandidateEffects, target, removedEffects)

		return Entry(
			result = MoveResult.Entry(
				target = target,
				damage = damage,
				damageMana = 0,
				missed = missed,
				criticalHit = criticalHit,
				addedEffects = addedEffects,
				removedEffects = removedEffects,
				addedStatModifiers = basicAddStatModifiers,
			),
			restoredHealth = restoreAttackerHealth,
			restoredMana = restoreAttackerMana,
		)
	}

	private fun determineRemovedEffects(candidates: Map<StatusEffect, Int>, target: CombatantState) = candidates.filter {
		val effect = it.key
		val chance = it.value
		if (!target.statusEffects.contains(effect)) return@filter false
		if (chance <= Random.Default.nextInt(100)) return@filter false
		if (target.getAutoEffects(context).contains(effect)) return@filter false
		true
	}.keys

	private fun determineAddedEffects(
		candidates: Map<StatusEffect, Int>, target: CombatantState, removedEffects: Set<StatusEffect>
	) = candidates.filter {
		val effect = it.key
		val chance = it.value
		if (chance <= Random.Default.nextInt(100)) return@filter false
		if (target.statusEffects.contains(effect) && !removedEffects.contains(effect)) return@filter false
		val resistance = target.getResistance(effect, context)
		if (resistance > Random.Default.nextInt(100)) return@filter false
		true
	}.keys

	private fun effectListToMap(list: Collection<PossibleStatusEffect>): MutableMap<StatusEffect, Int> {
		val map = mutableMapOf<StatusEffect, Int>()
		for (candidate in list) {
			map[candidate.effect] = map.getOrDefault(candidate.effect, 0) + candidate.chance
		}
		return map
	}

	private fun statListToMap(list: Collection<StatModifierRange>): MutableMap<CombatStat, Int> {
		val map = mutableMapOf<CombatStat, Int>()
		for (entry in list) {
			val adder = Random.Default.nextInt(entry.minAdder, 1 + entry.maxAdder)
			map[entry.stat] = map.getOrDefault(entry.stat, 0) + adder
		}
		return map
	}

	fun computeSkillResult(
		skill: ActiveSkill, attacker: CombatantState, targets: List<CombatantState>, passedChallenge: Boolean
	): MoveResult {
		val rawWeapon = attacker.getEquipment(context)[0]
		val weapon = rawWeapon?.equipment?.weapon

		val skillDamage = skill.damage

		var attackReactionType = ReactionSkillType.RangedAttack
		var defenseReactionType = ReactionSkillType.RangedDefense
		var multiplierStat = CombatStat.Spirit
		var defenseStat: CombatStat? = CombatStat.RangedDefense
		if (skill.isMelee) {
			if (skillDamage != null && skillDamage.spiritModifier != SkillSpiritModifier.SpiritBlade) {
				multiplierStat = CombatStat.Strength
				defenseStat = CombatStat.MeleeDefense
			}
			attackReactionType = ReactionSkillType.MeleeAttack
			defenseReactionType = ReactionSkillType.MeleeDefense
		}

		var extraFlatDamage = 0
		var elementalBonus = 0f
		var attackValue = skillDamage?.flatAttackValue ?: 0
		if (skillDamage != null) {
			attackValue += (skillDamage.weaponModifier * attacker.getStat(CombatStat.Attack, context)).roundToInt()
			attackValue += (skillDamage.levelModifier * attacker.getLevel(context))
			if (skillDamage.spiritModifier == SkillSpiritModifier.GreenLightning) {
				attackValue += 2 * attacker.getStat(CombatStat.Spirit, context)
			}
			if (skillDamage.spiritModifier == SkillSpiritModifier.DivineGlory) {
				extraFlatDamage += attacker.getLevel(context) * attacker.getStat(CombatStat.Spirit, context)
			}
			if (skillDamage.spiritModifier == SkillSpiritModifier.LayOnHands) {
				extraFlatDamage -= 2 * attacker.getLevel(context) * attacker.getStat(CombatStat.Spirit, context)
			}

			if (skillDamage.ignoresDefense) defenseStat = null
			if (skillDamage.ignoresShield) TODO("ignores shield")
			for (bonus in skillDamage.bonusAgainstElements) {
				if (bonus.element == skill.element) elementalBonus += bonus.modifier
			}
			if (skillDamage.moneyModifier != 0f) TODO("money modifier")
			if (skillDamage.gemModifier != 0f) TODO("gem modifier")
			extraFlatDamage += (skillDamage.lostHealthModifier * (attacker.maxHealth - attacker.currentHealth)).roundToInt()
			if (skillDamage.statusEffectModifier != 0f) TODO("status effect modifier")
			if (skillDamage.killCountModifier != 0f) TODO("kill count modifier")
			extraFlatDamage += skillDamage.hardcodedDamage
			if (skillDamage.potionModifier != 0f) TODO("potion modifier")
			if (skillDamage.crescendoModifier != 0f) TODO("crescendo modifier")
		}

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

		var sound = skill.particleEffect?.damageSound
		if (sound == null && skill.isMelee) {
			sound = skill.particleEffect?.initialSound
			if (sound == null && weapon != null) sound = weapon.hitSound ?: weapon.type.soundEffect
			if (sound == null) sound = context.sounds.battle.punch
		}

		val critChance = if (skillDamage?.critChance != null) skillDamage.critChance!!
		else if (skill.isMelee && weapon?.critChance != null) weapon.critChance else 0

		val rawEntries = targets.map { target ->
			var revengeDamage = 0
			if (skillDamage != null) {
				revengeDamage += (skillDamage.remainingTargetHpModifier * target.currentHealth).roundToInt()
			}
			computeAttackResult(
				attacker, target, passedChallenge,
				multiplierStat = multiplierStat,
				defenseStat = defenseStat,
				isMelee = skill.isMelee,
				isHealing = skill.isPositive() && !target.revertsHealing(),
				attackReactionType = attackReactionType,
				defenseReactionType = defenseReactionType,
				baseFlatDamage = extraFlatDamage + revengeDamage,
				attackValue = attackValue,
				basicElementalBonus = elementalBonus,
				basicHitChance = skill.accuracy,
				basicCritChance = critChance,
				applyDamageSplit = targets.size > 1 && skillDamage != null && skillDamage.splitDamage,
				basicHealthDrain = skill.healthDrain,
				basicManaDrain = 0f,
				attackElement = skill.element,
				basicAddStatModifiers = addStatModifiers,
				basicAddEffects = addEffects,
				basicRemoveEffects = removeEffects,
		) }

		val sounds = mutableListOf<SoundEffect>()
		if (rawEntries.any { it.result.missed }) sounds.add(context.sounds.battle.miss)
		if (sound != null && rawEntries.any { !it.result.missed }) sounds.add(sound)
		if (rawEntries.any { !it.result.missed && it.result.criticalHit }) sounds.add(context.sounds.battle.critical)
		return MoveResult(
			element = skill.element,
			sounds = sounds,
			targets = rawEntries.map { it.result },
			restoreAttackerHealth = rawEntries.sumOf { it.restoredHealth },
			restoreAttackerMana = rawEntries.sumOf { it.restoredMana },
			overrideBlinkColor = 0,
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

		val rawEntry = computeAttackResult(
			attacker, target, passedChallenge,
			multiplierStat = CombatStat.Strength,
			defenseStat = CombatStat.MeleeDefense,
			isMelee = true,
			isHealing = false,
			attackReactionType = ReactionSkillType.MeleeAttack,
			defenseReactionType = ReactionSkillType.MeleeDefense,
			baseFlatDamage = 0,
			attackValue = attacker.getStat(CombatStat.Attack, context),
			basicElementalBonus = 0f,
			basicHitChance = hitChance,
			basicCritChance = critChance,
			applyDamageSplit = false,
			basicHealthDrain = healthDrain,
			basicManaDrain = manaDrain,
			attackElement = attackElement,
			basicAddStatModifiers = mutableMapOf(),
			basicAddEffects = mutableMapOf(),
			basicRemoveEffects = mutableMapOf(),
		)

		val sounds = mutableListOf<SoundEffect>()
		if (rawEntry.result.missed) {
			sounds.add(context.sounds.battle.miss)
		} else {
			sounds.add(sound)
			if (rawEntry.result.criticalHit) sounds.add(context.sounds.battle.critical)
		}
		return MoveResult(
			element = attackElement,
			sounds = sounds,
			targets = listOf(rawEntry.result),
			restoreAttackerHealth = rawEntry.restoredHealth,
			restoreAttackerMana = rawEntry.restoredMana,
			overrideBlinkColor = 0,
		)
	}

	fun computeItemResult(item: Item, thrower: CombatantState, target: CombatantState): MoveResult {
		val consumable = item.consumable ?: throw IllegalArgumentException("Item ${item.flashName} is not consumable")
		var (restoreHealth, restoreMana) = if (consumable.isFullCure) {
			Pair(target.maxHealth - target.currentHealth, target.maxMana - target.currentMana)
		} else Pair(consumable.restoreHealth, consumable.restoreMana)

		if (target.currentHealth == 0) {
			restoreHealth += (consumable.revive * target.maxHealth).roundToInt()
		}

		if (target.revertsHealing()) restoreHealth = 0

		val effectsToRemove = effectListToMap(consumable.removeStatusEffects)
		if (consumable.removeNegativeStatusEffects) {
			for (effect in target.statusEffects) {
				if (!effect.isPositive) effectsToRemove[effect] = 100
			}
		}

		val damage = consumable.damage
		if (damage != null) TODO("Damaging consumables")

		val removedEffects = determineRemovedEffects(effectsToRemove, target)
		val entry = MoveResult.Entry(
			target = target,
			damage = -restoreHealth,
			damageMana = -restoreMana,
			missed = false,
			criticalHit = false,
			removedEffects = removedEffects,
			addedEffects = determineAddedEffects(
				effectListToMap(consumable.addStatusEffects), target, removedEffects
			),
			addedStatModifiers = statListToMap(consumable.statModifiers)
		)

		val particleEffect = consumable.particleEffect
		val sounds = mutableListOf<SoundEffect>()
		if (particleEffect != null) {
			particleEffect.initialSound()?.let { sounds.add(it) }
			particleEffect.damageSound()?.let { sounds.add(it) }
		}
		return MoveResult(
			element = item.element ?: thrower.element,
			sounds = sounds,
			targets = listOf(entry),
			restoreAttackerHealth = 0,
			restoreAttackerMana = 0,
			overrideBlinkColor = consumable.blinkColor,
		)
	}

	private class Entry(
		val result: MoveResult.Entry,
		val restoredHealth: Int,
		val restoredMana: Int,
	)
}
