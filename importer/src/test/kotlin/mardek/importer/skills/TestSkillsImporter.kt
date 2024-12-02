package mardek.importer.skills

import mardek.assets.combat.CombatAssets
import mardek.assets.skill.*
import mardek.importer.combat.importCombatAssets
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestSkillsImporter {

	private lateinit var combatAssets: CombatAssets
	private lateinit var skillAssets: SkillAssets

	@BeforeAll
	fun importSkills() {
		combatAssets = importCombatAssets()
		skillAssets = importSkills(combatAssets, "mardek/importer/combat/skills.txt")
	}

	private fun getAction(className: String, skillName: String): ActiveSkill {
		val skillClass = skillAssets.classes.find { it.name == className } ?: throw IllegalArgumentException(
				"Can't find class with name $className"
		)
		return skillClass.actions.find { it.name == skillName } ?: throw IllegalArgumentException(
				"Can't find skill with name $skillName"
		)
	}

	@Test
	fun testMardekEarthSlash() {
		val earthSlash = getAction("Magic Sword", "Earth Slash")
		val damage = earthSlash.damage!!
		assertEquals(0, damage.flatAttackValue)
		assertEquals(1.25f, damage.weaponModifier, 1e-4f)
		assertEquals(1, damage.bonusAgainstElements.size)
		val waterBonus = damage.bonusAgainstElements[0]
		assertEquals("WATER", waterBonus.element.properName)
		assertEquals(1.5f, waterBonus.modifier, 1e-4f)
		assertNull(damage.critChance)
		assertEquals(12, earthSlash.manaCost)
		assertEquals(100, earthSlash.accuracy)
		assertEquals(20, earthSlash.masteryPoints)
		assertEquals(ActiveSkillMode.Melee, earthSlash.mode)
		assertEquals("EARTH", earthSlash.element.properName)
		assertEquals(SkillTargetType.Single, earthSlash.targetType)
		assertEquals("slash_earth", earthSlash.particleEffect)
		assertEquals("Magically enchant your sword to inflict Earth elemental damage.", earthSlash.description)
	}

	@Test
	fun testImmortalInjustice() {
		val ii = getAction("Techniques", "Immoral Injustice")
		val damage = ii.damage!!
		assertEquals(1f, damage.weaponModifier, 1e-4f)
		assertEquals(2, damage.bonusAgainstElements.size)
		for (bonus in damage.bonusAgainstElements) {
			assertEquals(2f, bonus.modifier, 1e-4f)
			if (bonus.element.properName != "DARK") assertEquals("LIGHT", bonus.element.properName)
		}
		assertEquals(10, ii.manaCost)
		assertEquals(20, ii.masteryPoints)

		assertFalse(ii.allParticleEffects)
		assertFalse(ii.centered)
		assertTrue(ii.arena)
	}

	@Test
	fun testBoost() {
		val boost = getAction("Spellbladery", "Boost")
		assertNull(boost.damage)
		assertEquals(5, boost.manaCost)
		assertEquals(100, boost.accuracy)

		assertEquals(1, boost.statModifiers.size)
		val strengthBoost = boost.statModifiers[0]
		assertEquals("STR", strengthBoost.stat.flashName)
		assertEquals(1, strengthBoost.minAdder)
		assertEquals(6, strengthBoost.maxAdder)

		assertEquals(ActiveSkillMode.Self, boost.mode)
		assertEquals("FIRE", boost.element.properName)
		assertEquals(SkillTargetType.Self, boost.targetType)
		assertEquals("boost", boost.particleEffect)
		assertTrue(boost.isHealing)
	}

	@Test
	fun testBarrierBreak() {
		val barrierBreak = getAction("Spellbladery", "Barrier Break")
		val damage = barrierBreak.damage!!
		assertEquals(0, damage.flatAttackValue)
		assertEquals(1f, damage.weaponModifier, 1e-4f)

		assertEquals(10, barrierBreak.manaCost)
		assertEquals(1, barrierBreak.removeStatusEffects.size)
		val removeShield = barrierBreak.removeStatusEffects[0]
		assertEquals("Shield", removeShield.effect.niceName)
		assertEquals(100, removeShield.chance)
		assertEquals("DARK", barrierBreak.element.properName)
	}

	@Test
	fun testSpiritBlade() {
		val spiritBlade = getAction("Spellbladery", "Spiritblade")
		val damage = spiritBlade.damage!!
		assertEquals(1f, damage.weaponModifier, 1e-4f)
		assertEquals(SkillSpiritModifier.SpiritBlade, damage.spiritModifier)
		assertEquals("AETHER", spiritBlade.element.properName)
		assertEquals(SkillTargetType.Single, spiritBlade.targetType)
	}

	@Test
	fun testRagingInferno() {
		val inferno = getAction("Mimicry", "Raging Inferno")
		val damage = inferno.damage!!
		assertEquals(100, damage.flatAttackValue)
		assertEquals(0, damage.critChance)
		assertTrue(damage.splitDamage)

		assertEquals(ActiveSkillMode.Ranged, inferno.mode)
		assertEquals(20, inferno.manaCost)
		assertEquals(0, inferno.masteryPoints)
		assertEquals(SkillTargetType.AllEnemies, inferno.targetType)

		assertTrue(inferno.allParticleEffects)
		assertTrue(inferno.centered)
		assertFalse(inferno.arena)
	}

	@Test
	fun testDivineGlory() {
		val glory = getAction("Holy Arts", "Divine Glory")
		val damage = glory.damage!!
		assertEquals(0, damage.flatAttackValue)
		assertEquals(0f, damage.weaponModifier, 1e-4f)
		assertEquals(SkillSpiritModifier.DivineGlory, damage.spiritModifier)
		assertTrue(damage.ignoresDefense)
		assertFalse(damage.ignoresShield)

		assertEquals(25, glory.manaCost)
		assertEquals(ActiveSkillMode.Melee, glory.mode)
		assertEquals("smiteevil", glory.particleEffect)
	}

	@Test
	fun testLayOnHands() {
		val hands = getAction("Holy Arts", "Lay on Hands")
		val damage = hands.damage!!
		assertEquals(0, damage.flatAttackValue)
		assertEquals(0f, damage.weaponModifier, 1e-4f)
		assertEquals(SkillSpiritModifier.LayOnHands, damage.spiritModifier)
		assertTrue(damage.ignoresDefense)

		assertEquals(2, hands.removeStatusEffects.size)
		for (effect in hands.removeStatusEffects) {
			assertEquals(100, effect.chance)
		}
		assertNotNull(hands.removeStatusEffects.find { it.effect.niceName == "Curse" })
		assertNotNull(hands.removeStatusEffects.find { it.effect.niceName == "Zombie" })
		assertTrue(hands.isHealing)
	}

	@Test
	fun testHealingWind() {
		val healingWind = getAction("Aeromancy", "Healing Wind")
		val damage = healingWind.damage!!
		assertEquals(50, damage.flatAttackValue)

		assertEquals(1, healingWind.removeStatusEffects.size)
		val removeConfusion = healingWind.removeStatusEffects[0]
		assertEquals("Confusion", removeConfusion.effect.niceName)
		assertEquals(100, removeConfusion.chance)
		assertEquals(ActiveSkillMode.Ranged, healingWind.mode)
		assertEquals(SkillTargetType.Any, healingWind.targetType)

		assertTrue(healingWind.isHealing)
		assertEquals(SkillCombatRequirement.Always, healingWind.combatRequirement)
	}

	@Test
	fun testNullAirOnce() {
		val nullAir = getAction("Aeromancy", "Null Air Once")
		assertNull(nullAir.damage)
		assertTrue(nullAir.isBuff)
		assertEquals(1, nullAir.addStatusEffects.size)

		val addEffect = nullAir.addStatusEffects[0]
		assertEquals(100, addEffect.chance)
		assertEquals("AIR", addEffect.effect.nullifiesElement!!.properName)

		assertEquals(SkillTargetType.Any, nullAir.targetType)
	}

	@Test
	fun testAcid() {
		val acid = getAction("Mimicry", "Acid")
		val damage = acid.damage!!
		assertEquals(30, damage.flatAttackValue)
		assertEquals(0f, damage.weaponModifier, 1e-4f)
		assertEquals(0, damage.critChance)

		assertEquals(2, acid.addStatusEffects.size)
		val addPoison = acid.addStatusEffects[0]
		assertEquals("Poison", addPoison.effect.niceName)
		assertEquals(30, addPoison.chance)
		val addNumb = acid.addStatusEffects[1]
		assertEquals("Numbness", addNumb.effect.niceName)
		assertEquals(6, addNumb.chance)

		assertEquals(1, acid.statModifiers.size)
		val reduceDef = acid.statModifiers[0]
		assertEquals("DEF", reduceDef.stat.flashName)
		assertEquals(-6, reduceDef.minAdder)
		assertEquals(-1, reduceDef.maxAdder)

		assertEquals(10, acid.manaCost)
		assertEquals(100, acid.accuracy)
		assertEquals(ActiveSkillMode.Ranged, acid.mode)
		assertEquals(SkillTargetType.Single, acid.targetType)
	}

	@Test
	fun testDrillOMatic() {
		val drill = getAction("Inventions", "Drill-O-Matic")
		val damage = drill.damage!!
		assertEquals(0, damage.flatAttackValue)
		assertEquals(0f, damage.weaponModifier, 1e-4f)
		assertEquals(2, damage.levelModifier)
		assertTrue(damage.ignoresDefense)

		assertEquals(0, drill.manaCost)
		assertEquals(100, drill.accuracy)

		assertEquals(1, drill.statModifiers.size)
		val reduceDef = drill.statModifiers[0]
		assertEquals("DEF", reduceDef.stat.flashName)
		assertEquals(-6, reduceDef.minAdder)
		assertEquals(-1, reduceDef.maxAdder)

		assertEquals(-1, drill.masteryPoints)
		assertEquals("PHYSICAL", drill.element.properName)
	}

	@Test
	fun testPotionSpray() {
		val potionSpray = getAction("Inventions", "Potion Spray")
		val damage = potionSpray.damage!!
		assertEquals(0, damage.flatAttackValue)
		assertEquals(0f, damage.weaponModifier, 1e-4f)
		assertEquals(0, damage.levelModifier)
		assertEquals(0.5f, damage.potionModifier, 1e-4f)
		assertTrue(damage.ignoresShield)
		assertFalse(damage.ignoresDefense)

		assertTrue(potionSpray.isHealing)
		assertEquals(-1, potionSpray.masteryPoints)
		assertEquals(ActiveSkillMode.Self, potionSpray.mode)
		assertEquals(SkillTargetType.AllAllies, potionSpray.targetType)
	}

	@Test
	fun testBloodDrain() {
		val bloodDrain = getAction("Mimicry", "Blood Drain")
		val damage = bloodDrain.damage!!
		assertEquals(0, damage.flatAttackValue)
		assertEquals(0.8f, damage.weaponModifier, 1e-4f)

		assertEquals("DARK", bloodDrain.element.properName)
		assertTrue(bloodDrain.drainsBlood)
	}

	@Test
	fun testCoupDeGrace() {
		val coupDeGrace = getAction("Techniques", "Coup de Grace")
		val damage = coupDeGrace.damage!!
		assertEquals(0, damage.flatAttackValue)
		assertEquals(1f, damage.weaponModifier, 1e-4f)
		assertEquals(1f, damage.statusEffectModifier)

		assertEquals(0, coupDeGrace.manaCost)
	}

	@Test
	fun testResurrect() {
		val resurrect = getAction("Astral Magic", "Resurrect")
		assertNull(resurrect.damage)
		assertEquals(32, resurrect.manaCost)
		assertEquals(ActiveSkillMode.Ranged, resurrect.mode)
		assertEquals(SkillTargetType.Single, resurrect.targetType)
		assertTrue(resurrect.isHealing)
		assertEquals(0.5f, resurrect.revive, 1e-4f)
	}

	@Test
	fun testSinstrike() {
		val sinstrike = getAction("Techniques", "Sinstrike")
		val damage = sinstrike.damage!!
		assertEquals(0, damage.flatAttackValue)
		assertEquals(0f, damage.weaponModifier, 1e-4f)
		assertEquals(1f, damage.killCountModifier, 1e-4f)

		assertEquals(0, sinstrike.manaCost)
	}

	@Test
	fun testCrescendoSlash() {
		val crescendoSlash = getAction("Techniques", "Crescendo Slash")
		val damage = crescendoSlash.damage!!
		assertEquals(0, damage.flatAttackValue)
		assertEquals(1f, damage.weaponModifier, 1e-4f)
		assertEquals(0.25f, damage.crescendoModifier, 1e-4f)
	}

	@Test
	fun testSureSlash() {
		val sureSlash = getAction("Techniques", "Sure Slash")
		assertEquals(0.5f, sureSlash.damage!!.weaponModifier, 1e-4f)
		assertEquals(255, sureSlash.accuracy)
	}

	@Test
	fun testAqualung() {
		val aqualung = getAction("Elemancy", "Aqualung")
		assertNull(aqualung.damage)

		assertEquals(1, aqualung.addStatusEffects.size)
		val addEffect = aqualung.addStatusEffects[0]
		assertTrue(addEffect.effect.canWaterBreathe)
		assertEquals(100, addEffect.chance)

		assertTrue(aqualung.isHealing)
		assertEquals(SkillCombatRequirement.OutsideCombat, aqualung.combatRequirement)
	}

	@Test
	fun testNeedleflare() {
		val needleflare = getAction("Mimicry", "Needleflare")
		val damage = needleflare.damage!!
		assertEquals(60, damage.flatAttackValue)

		assertEquals(1, needleflare.addStatusEffects.size)
		val maybePoison = needleflare.addStatusEffects[0]
		assertEquals("Poison", maybePoison.effect.niceName)
		assertEquals(50, maybePoison.chance)

		assertEquals(SkillTargetType.Single, needleflare.targetType)
		assertEquals(ActiveSkillMode.Ranged, needleflare.mode)
		assertEquals("PHYSICAL", needleflare.element.properName)
		assertEquals(20, needleflare.delay)
	}

	@Test
	fun testGasOMatic() {
		val gas = getAction("Inventions", "Gas-O-Matic")
		val damage = gas.damage!!
		assertEquals(0, damage.flatAttackValue)
		assertEquals(1, damage.levelModifier)
		assertFalse(damage.splitDamage)

		assertTrue(gas.isBreath)
		assertEquals(SkillTargetType.AllEnemies, gas.targetType)
	}

	@Test
	fun testRevengeStrike() {
		val revengeStrike = getAction("Mimicry", "Revenge Strike")
		val damage = revengeStrike.damage!!
		assertEquals(0, damage.flatAttackValue)
		assertEquals(0f, damage.weaponModifier, 1e-4f)
		assertEquals(1f, damage.lostHealthModifier, 1e-4f)
	}

	@Test
	fun testGemsplosion() {
		val gems = getAction("Mimicry", "Gemsplosion")
		val damage = gems.damage!!
		assertEquals(0f, damage.weaponModifier, 1e-4f)
		assertEquals(1f, damage.gemModifier, 1e-4f)

		assertEquals("THAUMA", gems.element.properName)
	}

	@Test
	fun testMoneyAttack() {
		val money = getAction("Mimicry", "Money Attack!")
		val damage = money.damage!!
		assertEquals(0f, damage.weaponModifier, 1e-4f)
		assertEquals(1f, damage.moneyModifier, 1e-4f)
	}

	@Test
	fun test1000needles() {
		val needles = getAction("Mimicry", "1000 Needles")
		val damage = needles.damage!!
		assertEquals(0f, damage.weaponModifier, 1e-4f)
		assertEquals(1000, damage.hardcodedDamage)
	}

	@Test
	fun testBalladOfLife() {
		val life = getAction("Siren Songs", "Ballad of Life")
		assertEquals("{RestoreHP:10,LvlMod:0.2}", life.rawSongPower)
	}

	@Test
	fun testDolorousDirge() {
		val dirge = skillAssets.sirenSongs.find { it.name == "Dolorous Dirge" }!!
		assertEquals(1, dirge.time)
		assertEquals(0, dirge.tempo)
		assertEquals(SirenNote(0, 5), dirge.notes[0])
		assertNull(dirge.notes[1])
		assertEquals(SirenNote(1, 1), dirge.notes[7])
	}

	private fun getReactionSkill(name: String, type: ReactionSkillType) = skillAssets.reactionSkills.find {
		it.name == name && it.type == type
	}!!

	@Test
	fun testStunstrike() {
		val stunstrike = getReactionSkill("Stunstrike", ReactionSkillType.MeleeAttack)

		assertEquals("Powers", stunstrike.skillClass!!.name)
		assertEquals(1, stunstrike.addStatusEffects.size)
		val stun = stunstrike.addStatusEffects[0]
		assertEquals(50, stun.effect.skipTurnChance)
		assertEquals(100, stun.chance)

		assertEquals("This reaction has a 100% chance of inflicting Paralysis.", stunstrike.description)
		assertEquals("AIR", stunstrike.element.properName)
		assertEquals(20, stunstrike.masteryPoints)
		assertEquals(5, stunstrike.enablePoints)

		assertEquals(0f, stunstrike.drainHp, 1e-4f)
		assertEquals(0f, stunstrike.absorbMp, 1e-4f)
	}

	@Test
	fun testPoison20() {
		val poisonAttack = getReactionSkill("P+Poison 20%", ReactionSkillType.MeleeAttack)

		assertEquals(1, poisonAttack.addStatusEffects.size)
		val addPoison = poisonAttack.addStatusEffects[0]
		assertEquals("Poison", addPoison.effect.niceName)
		assertEquals(20, addPoison.chance)

		assertEquals(20, poisonAttack.masteryPoints)
	}

	@Test
	fun testSmitePlus() {
		val smitePlus = getReactionSkill("Smite +", ReactionSkillType.MeleeAttack)
		assertTrue(smitePlus.smitePlus)
		assertEquals("LIGHT", smitePlus.element.properName)
		assertEquals("Holy Arts", smitePlus.skillClass!!.name)
		assertEquals(5, smitePlus.enablePoints)
	}

	@Test
	fun testDamagePlus1() {
		val plusOne = getReactionSkill("DMG+1", ReactionSkillType.MeleeAttack)
		assertEquals("Imagination", plusOne.skillClass!!.name)
		assertEquals(1, plusOne.addFlatDamage)
		assertEquals(1, plusOne.enablePoints)
		assertEquals("PHYSICAL", plusOne.element.properName)
		assertEquals("Adds an additional point of damage to your attack.", plusOne.description)
	}

	@Test
	fun testDamagePlus10Percent() {
		val plusTen = getReactionSkill("DMG+10%", ReactionSkillType.MeleeAttack)
		assertEquals(0.1f, plusTen.addDamageFraction, 1e-4f)
		assertEquals(10, plusTen.masteryPoints)
		assertNull(plusTen.skillClass)
	}

	@Test
	fun testCriticalPlus20() {
		val critical = getReactionSkill("Critical+20%", ReactionSkillType.MeleeAttack)
		assertEquals(20, critical.addCritChance)
		assertEquals("AIR", critical.element.properName)
	}

	@Test
	fun testAccuracyPlus50() {
		val accuracy = getReactionSkill("Accuracy+50%", ReactionSkillType.MeleeAttack)
		assertEquals(50, accuracy.addAccuracy)
		assertEquals(0, accuracy.addCritChance)
		assertEquals(4, accuracy.enablePoints)
	}

	@Test
	fun testSoulstrike() {
		val soulstrike = getReactionSkill("Soulstrike", ReactionSkillType.MeleeAttack)
		assertTrue(soulstrike.soulStrike)
		assertEquals("AETHER", soulstrike.element.properName)
	}

	@Test
	fun testQuarryBeast() {
		val quarry = getReactionSkill("Quarry: BEAST", ReactionSkillType.MeleeAttack)

		assertEquals(1, quarry.effectiveAgainst.size)
		val beast = quarry.effectiveAgainst[0]
		assertEquals("BEAST", beast.race.flashName)
		assertEquals(0.5f, beast.bonusFraction, 1e-4f)

		assertEquals("DARK", quarry.element.properName)
		assertFalse(quarry.soulStrike)
	}

	@Test
	fun testShieldBreakTenPercent() {
		val shieldBreak = getReactionSkill("Shield Break 10%", ReactionSkillType.MeleeAttack)

		assertEquals(1, shieldBreak.removeStatusEffects.size)
		val removeShield = shieldBreak.removeStatusEffects[0]
		assertEquals(0.5f, removeShield.effect.meleeDamageReduction, 1e-4f)
		assertEquals(10, removeShield.chance)

		assertEquals(0, shieldBreak.effectiveAgainst.size)
		assertEquals(8, shieldBreak.enablePoints)
		assertEquals(0f, shieldBreak.drainHp, 1e-4f)
	}

	@Test
	fun testDrainHp() {
		val drainHp = getReactionSkill("Drain HP 10%", ReactionSkillType.MeleeAttack)

		assertEquals(0.1f, drainHp.drainHp, 1e-4f)
		assertEquals(0, drainHp.removeStatusEffects.size)
	}

	@Test
	fun testNullifyPhysical() {
		val nullify = getReactionSkill("Nullify Physical", ReactionSkillType.MeleeDefense)
		assertEquals(-10f, nullify.addDamageFraction, 1e-4f)

		assertEquals(10, nullify.enablePoints)
		assertEquals("Powers", nullify.skillClass!!.name)
	}

	@Test
	fun testBlock() {
		val block = getReactionSkill("Block", ReactionSkillType.MeleeDefense)
		assertEquals(-1, block.addFlatDamage)

		assertEquals(5, block.masteryPoints)
		assertEquals("Child", block.skillClass!!.key)
		assertEquals("PHYSICAL", block.element.properName)
	}

	@Test
	fun testMinusTenPercentMeleeDamage() {
		val reduceDamage = getReactionSkill("DMG-10%", ReactionSkillType.MeleeDefense)
		assertEquals(-0.1f, reduceDamage.addDamageFraction, 1e-4f)

		assertEquals(10, reduceDamage.masteryPoints)
		assertNull(reduceDamage.skillClass)
	}

	@Test
	fun testMinusFiftyPercentMeleeFireDamage() {
		val reduceDamage = getReactionSkill("FIRE-50%", ReactionSkillType.MeleeDefense)

		assertEquals(1, reduceDamage.elementalBonuses.size)
		val resistFire = reduceDamage.elementalBonuses[0]
		assertEquals("FIRE", resistFire.element.properName)
		assertEquals(-0.5f, resistFire.modifier, 1e-4f)

		assertEquals(0f, reduceDamage.addDamageFraction, 1e-4f)
		assertEquals("Increases FIRE resistance by 50%.", reduceDamage.description)
	}

	@Test
	fun testTwentyPercentMeleeEvasion() {
		val evasion = getReactionSkill("Evasion 20%", ReactionSkillType.MeleeDefense)
		assertEquals(-20, evasion.addAccuracy)

		assertEquals(0, evasion.elementalBonuses.size)
		assertEquals("AIR", evasion.element.properName)
	}

	@Test
	fun testMeleeSurvivor() {
		val survivor = getReactionSkill("Survivor", ReactionSkillType.MeleeDefense)
		assertTrue(survivor.survivor)
		assertEquals("DIVINE", survivor.element.properName)

		assertEquals(40, survivor.masteryPoints)
		assertEquals(0, survivor.addAccuracy)
	}

	@Test
	fun testMagicDamagePlusTenPercent() {
		val increaseDamage = getReactionSkill("M DMG+10%", ReactionSkillType.RangedAttack)
		assertEquals(0.1f, increaseDamage.addDamageFraction, 1e-4f)

		assertEquals("THAUMA", increaseDamage.element.properName)
		assertEquals(0, increaseDamage.addFlatDamage)
	}

	@Test
	fun testMagicFireDamagePlusTwentyPercent() {
		val increaseDamage = getReactionSkill("M FIRE+20%", ReactionSkillType.RangedAttack)

		assertEquals(1, increaseDamage.elementalBonuses.size)
		val increaseFireDamage = increaseDamage.elementalBonuses[0]
		assertEquals("FIRE", increaseFireDamage.element.properName)
		assertEquals(0.2f, increaseFireDamage.modifier, 1e-4f)

		assertEquals(3, increaseDamage.enablePoints)
		assertEquals(0f, increaseDamage.addDamageFraction, 1e-4f)
	}

	@Test
	fun testMagicShieldBreak() {
		val shieldBreak = getReactionSkill("M Shield Break 10%", ReactionSkillType.RangedAttack)

		assertEquals(1, shieldBreak.removeStatusEffects.size)
		val removeMagicShield = shieldBreak.removeStatusEffects[0]
		assertEquals("M.Shield", removeMagicShield.effect.niceName)
		assertEquals(10, removeMagicShield.chance)

		assertEquals(40, shieldBreak.masteryPoints)
		assertEquals(0, shieldBreak.elementalBonuses.size)
	}

	@Test
	fun testMagicDrainHp() {
		val drainHp = getReactionSkill("M Drain HP 10%", ReactionSkillType.RangedAttack)
		assertEquals(0.1f, drainHp.drainHp, 1e-4f)

		assertEquals("DARK", drainHp.element.properName)
		assertEquals(0, drainHp.removeStatusEffects.size)
	}

	@Test
	fun testMagicPoison() {
		val magicPoison = getReactionSkill("M+Poison 20%", ReactionSkillType.RangedAttack)

		assertEquals(1, magicPoison.addStatusEffects.size)
		val addPoison = magicPoison.addStatusEffects[0]
		assertEquals("Poison", addPoison.effect.niceName)
		assertEquals(20, addPoison.chance)

		assertEquals(0f, magicPoison.drainHp, 1e-4f)
		assertEquals("This reaction has a 20% chance of inflicting Poison.", magicPoison.description)
	}

	@Test
	fun testNullifyMagic() {
		val nullify = getReactionSkill("Nullify Magic", ReactionSkillType.RangedDefense)
		assertEquals(-1f, nullify.addDamageFraction, 1e-4f)
		assertEquals("Hero", nullify.skillClass!!.key)

		assertEquals(20, nullify.masteryPoints)
		assertEquals("Resists magical damage completely.", nullify.description)
	}

	@Test
	fun testMagicDamageMinusTenPercent() {
		val reduceDamage = getReactionSkill("M DMG-10%", ReactionSkillType.RangedDefense)
		assertEquals(-0.1f, reduceDamage.addDamageFraction, 1e-4f)

		assertEquals(1, reduceDamage.enablePoints)
		assertEquals(10, reduceDamage.masteryPoints)
		assertNull(reduceDamage.skillClass)
	}

	@Test
	fun testMagicDamageSoak() {
		val soak = getReactionSkill("M DMG Soak 10", ReactionSkillType.RangedDefense)
		assertEquals(-10, soak.addFlatDamage)

		assertEquals(0f, soak.addDamageFraction)
		assertEquals("THAUMA", soak.element.properName)
	}

	@Test
	fun testRangedFireResistance() {
		val fireResistance = getReactionSkill("M FIRE-50%", ReactionSkillType.RangedDefense)

		assertEquals(1, fireResistance.elementalBonuses.size)
		val reduceFire = fireResistance.elementalBonuses[0]
		assertEquals("FIRE", reduceFire.element.properName)
		assertEquals(-0.5f, reduceFire.modifier, 1e-4f)

		assertEquals(0, fireResistance.addFlatDamage)
		assertEquals(25, fireResistance.masteryPoints)
	}

	@Test
	fun testRangedEvasion() {
		val evasion = getReactionSkill("Spell Resist: 30%", ReactionSkillType.RangedDefense)
		assertEquals(-30, evasion.addAccuracy)

		assertEquals(0, evasion.elementalBonuses.size)
		assertEquals("Adds a 30% chance of taking no damage at all from spells.", evasion.description)
	}

	@Test
	fun testAbsorbMp() {
		val absorb = getReactionSkill("Absorb MP", ReactionSkillType.RangedDefense)
		assertEquals(1f, absorb.absorbMp, 1e-4f)

		assertEquals(0, absorb.addAccuracy)
		assertEquals(10, absorb.enablePoints)
	}

	@Test
	fun testRangedSurvivor() {
		val survivor = getReactionSkill("The One Who Lived", ReactionSkillType.RangedDefense)
		assertTrue(survivor.survivor)
		assertEquals("DIVINE", survivor.element.properName)

		assertEquals(0f, survivor.absorbMp, 1e-4f)
		assertNull(survivor.skillClass)
	}

	// Passive
	// TODO {skill:"Nature\'s Favour",effect:{RESIST:{PSN:20,SIL:20,SLP:20,PAR:20}},AP:5,RP:1,elem:"EARTH",desc:"Nature\'s power grants a 20% resistance to Poison, Silence, Sleep and Paralysis.",only:{Shm:true}}
	// TODO {skill:"HP+10%",effect:{hpmult:0.1},AP:10,RP:4,elem:"EARTH",desc:"Increases Max HP by 10%."}
	// TODO {skill:"MP+10%",effect:{mpmult:0.1},AP:10,RP:4,elem:"THAUMA",desc:"Increases Max MP by 10%."}
	// TODO {skill:"Antibody",effect:{RESIST:{PSN:100}},AP:20,RP:4,elem:"EARTH",desc:"Grants immunity to Poison."}
	// TODO {skill:"STR+1",effect:{statmod:{STR:1}},AP:15,RP:2,elem:"FIRE",desc:"Increases Strength by 1 point."}
	// TODO {skill:"Resist FIRE",effect:{RESIST:{FIRE:50}},AP:50,RP:8,elem:"FIRE",desc:"Increases FIRE resistance by 50%."}
	// TODO {skill:"Rainbow Aura Lv.1",effect:{RESIST:{FIRE:20,WATER:20,AIR:20,EARTH:20,LIGHT:20,DARK:20,ETHER:20,FIG:20}},AP:50,RP:10,elem:"LIGHT",desc:"Increases resistance to the natural, moral and spiritual elements by 20%."}
	// TODO {skill:"Auto-P.Shield",effect:{autoSTFX:{PSH:1}},AP:50,RP:10,elem:"LIGHT",desc:"Grants P.Shield at all times."}
	// TODO {skill:"SOS Regen",effect:{SOS:{RGN:1}},AP:20,RP:6,elem:"LIGHT",desc:"The Regen status effect triggers when the character is at 20% HP or less."}
	// TODO {skill:"EXP+20%",effect:{expmult:0.2},AP:40,RP:4,elem:"DIVINE",desc:"Increases all Experience earned by 20%."}
	// TODO {skill:"Double AP",effect:{apmult:2},AP:200,RP:10,elem:"DIVINE",desc:"Doubles AP earned for all skills."}
	// TODO {skill:"Double Gold",effect:{goldmult:1},AP:100,RP:10,elem:"DIVINE",desc:"Doubles gold earned from battle. If multiple characters have it, it adds +100% for each one; two characters gives 300% gold, for example."}
	// TODO {skill:"Loot Finder Lv.1",effect:{lootmod:5},AP:25,RP:4,elem:"DIVINE",desc:"Adds 5% to the chance of acquiring any item after battle."}
}
