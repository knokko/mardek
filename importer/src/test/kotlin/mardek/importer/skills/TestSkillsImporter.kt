package mardek.importer.skills

import mardek.content.Content
import mardek.content.skill.*
import mardek.importer.stats.importStatsContent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestSkillsImporter {

	private val margin = 1e-4f
	private val content = Content()

	@BeforeAll
	fun importSkills() {
		importStatsContent(content)
		importSkillsContent(content)
	}

	private fun getAction(className: String, skillName: String): ActiveSkill {
		val skillClass = content.skills.classes.find { it.name == className } ?: throw IllegalArgumentException(
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
		assertEquals(1.25f, damage.weaponModifier, margin)
		assertEquals(1, damage.bonusAgainstElements.size)
		val waterBonus = damage.bonusAgainstElements[0]
		assertEquals("WATER", waterBonus.element.properName)
		assertEquals(1.5f, waterBonus.modifier, margin)
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
		assertEquals(1f, damage.weaponModifier, margin)
		assertEquals(2, damage.bonusAgainstElements.size)
		for (bonus in damage.bonusAgainstElements) {
			assertEquals(2f, bonus.modifier, margin)
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
		assertEquals(1f, damage.weaponModifier, margin)

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
		assertEquals(1f, damage.weaponModifier, margin)
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
		assertEquals(0f, damage.weaponModifier, margin)
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
		assertEquals(0f, damage.weaponModifier, margin)
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
		assertEquals(0f, damage.weaponModifier, margin)
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
		assertEquals(0f, damage.weaponModifier, margin)
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
		assertEquals(0f, damage.weaponModifier, margin)
		assertEquals(0, damage.levelModifier)
		assertEquals(0.5f, damage.potionModifier, margin)
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
		assertEquals(0.8f, damage.weaponModifier, margin)

		assertEquals("DARK", bloodDrain.element.properName)
		assertTrue(bloodDrain.drainsBlood)
	}

	@Test
	fun testCoupDeGrace() {
		val coupDeGrace = getAction("Techniques", "Coup de Grace")
		val damage = coupDeGrace.damage!!
		assertEquals(0, damage.flatAttackValue)
		assertEquals(1f, damage.weaponModifier, margin)
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
		assertEquals(0.5f, resurrect.revive, margin)
	}

	@Test
	fun testSinstrike() {
		val sinstrike = getAction("Techniques", "Sinstrike")
		val damage = sinstrike.damage!!
		assertEquals(0, damage.flatAttackValue)
		assertEquals(0f, damage.weaponModifier, margin)
		assertEquals(1f, damage.killCountModifier, margin)

		assertEquals(0, sinstrike.manaCost)
	}

	@Test
	fun testCrescendoSlash() {
		val crescendoSlash = getAction("Techniques", "Crescendo Slash")
		val damage = crescendoSlash.damage!!
		assertEquals(0, damage.flatAttackValue)
		assertEquals(1f, damage.weaponModifier, margin)
		assertEquals(0.25f, damage.crescendoModifier, margin)
	}

	@Test
	fun testSureSlash() {
		val sureSlash = getAction("Techniques", "Sure Slash")
		assertEquals(0.5f, sureSlash.damage!!.weaponModifier, margin)
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
		assertEquals(0f, damage.weaponModifier, margin)
		assertEquals(1f, damage.lostHealthModifier, margin)
	}

	@Test
	fun testGemsplosion() {
		val gems = getAction("Mimicry", "Gemsplosion")
		val damage = gems.damage!!
		assertEquals(0f, damage.weaponModifier, margin)
		assertEquals(1f, damage.gemModifier, margin)

		assertEquals("THAUMA", gems.element.properName)
	}

	@Test
	fun testMoneyAttack() {
		val money = getAction("Mimicry", "Money Attack!")
		val damage = money.damage!!
		assertEquals(0f, damage.weaponModifier, margin)
		assertEquals(1f, damage.moneyModifier, margin)
	}

	@Test
	fun test1000needles() {
		val needles = getAction("Mimicry", "1000 Needles")
		val damage = needles.damage!!
		assertEquals(0f, damage.weaponModifier, margin)
		assertEquals(1000, damage.hardcodedDamage)
	}

	@Test
	fun testBalladOfLife() {
		val life = getAction("Siren Songs", "Ballad of Life")
		assertEquals("{RestoreHP:10,LvlMod:0.2}", life.rawSongPower)
	}

	@Test
	fun testDolorousDirge() {
		val dirge = content.skills.sirenSongs.find { it.name == "Dolorous Dirge" }!!
		assertEquals(1, dirge.time)
		assertEquals(0, dirge.tempo)
		assertEquals(SirenNote(0, 5), dirge.notes[0])
		assertNull(dirge.notes[1])
		assertEquals(SirenNote(1, 1), dirge.notes[7])
	}

	private fun getReactionSkill(name: String, type: ReactionSkillType) = content.skills.reactionSkills.find {
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

		assertEquals(0f, stunstrike.drainHp, margin)
		assertEquals(0f, stunstrike.absorbMp, margin)
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
		assertEquals(0.1f, plusTen.addDamageFraction, margin)
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
		assertEquals("BEAST", beast.type.flashName)
		assertEquals(0.5f, beast.modifier, margin)

		assertEquals("DARK", quarry.element.properName)
		assertFalse(quarry.soulStrike)
	}

	@Test
	fun testShieldBreakTenPercent() {
		val shieldBreak = getReactionSkill("Shield Break 10%", ReactionSkillType.MeleeAttack)

		assertEquals(1, shieldBreak.removeStatusEffects.size)
		val removeShield = shieldBreak.removeStatusEffects[0]
		assertEquals(0.5f, removeShield.effect.meleeDamageReduction, margin)
		assertEquals(10, removeShield.chance)

		assertEquals(0, shieldBreak.effectiveAgainst.size)
		assertEquals(8, shieldBreak.enablePoints)
		assertEquals(0f, shieldBreak.drainHp, margin)
	}

	@Test
	fun testDrainHp() {
		val drainHp = getReactionSkill("Drain HP 10%", ReactionSkillType.MeleeAttack)

		assertEquals(0.1f, drainHp.drainHp, margin)
		assertEquals(0, drainHp.removeStatusEffects.size)
	}

	@Test
	fun testNullifyPhysical() {
		val nullify = getReactionSkill("Nullify Physical", ReactionSkillType.MeleeDefense)
		assertEquals(-10f, nullify.addDamageFraction, margin)

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
		assertEquals(-0.1f, reduceDamage.addDamageFraction, margin)

		assertEquals(10, reduceDamage.masteryPoints)
		assertNull(reduceDamage.skillClass)
	}

	@Test
	fun testMinusFiftyPercentMeleeFireDamage() {
		val reduceDamage = getReactionSkill("FIRE-50%", ReactionSkillType.MeleeDefense)

		assertEquals(1, reduceDamage.elementalBonuses.size)
		val resistFire = reduceDamage.elementalBonuses[0]
		assertEquals("FIRE", resistFire.element.properName)
		assertEquals(-0.5f, resistFire.modifier, margin)

		assertEquals(0f, reduceDamage.addDamageFraction, margin)
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
		assertEquals(0.1f, increaseDamage.addDamageFraction, margin)

		assertEquals("THAUMA", increaseDamage.element.properName)
		assertEquals(0, increaseDamage.addFlatDamage)
	}

	@Test
	fun testMagicFireDamagePlusTwentyPercent() {
		val increaseDamage = getReactionSkill("M FIRE+20%", ReactionSkillType.RangedAttack)

		assertEquals(1, increaseDamage.elementalBonuses.size)
		val increaseFireDamage = increaseDamage.elementalBonuses[0]
		assertEquals("FIRE", increaseFireDamage.element.properName)
		assertEquals(0.2f, increaseFireDamage.modifier, margin)

		assertEquals(3, increaseDamage.enablePoints)
		assertEquals(0f, increaseDamage.addDamageFraction, margin)
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
		assertEquals(0.1f, drainHp.drainHp, margin)

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

		assertEquals(0f, magicPoison.drainHp, margin)
		assertEquals("This reaction has a 20% chance of inflicting Poison.", magicPoison.description)
	}

	@Test
	fun testNullifyMagic() {
		val nullify = getReactionSkill("Nullify Magic", ReactionSkillType.RangedDefense)
		assertEquals(-1f, nullify.addDamageFraction, margin)
		assertEquals("Hero", nullify.skillClass!!.key)

		assertEquals(20, nullify.masteryPoints)
		assertEquals("Resists magical damage completely.", nullify.description)
	}

	@Test
	fun testMagicDamageMinusTenPercent() {
		val reduceDamage = getReactionSkill("M DMG-10%", ReactionSkillType.RangedDefense)
		assertEquals(-0.1f, reduceDamage.addDamageFraction, margin)

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
		assertEquals(-0.5f, reduceFire.modifier, margin)

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
		assertEquals(1f, absorb.absorbMp, margin)

		assertEquals(0, absorb.addAccuracy)
		assertEquals(10, absorb.enablePoints)
	}

	@Test
	fun testRangedSurvivor() {
		val survivor = getReactionSkill("The One Who Lived", ReactionSkillType.RangedDefense)
		assertTrue(survivor.survivor)
		assertEquals("DIVINE", survivor.element.properName)

		assertEquals(0f, survivor.absorbMp, margin)
		assertNull(survivor.skillClass)
	}

	private fun getPassive(name: String) = content.skills.passiveSkills.find { it.name == name }!!

	@Test
	fun testNaturesFavour() {
		val favour = getPassive("Nature's Favour")

		assertEquals("Nature's power grants a 20% resistance to Poison, Silence, Sleep and Paralysis.", favour.description)
		assertEquals("EARTH", favour.element.properName)
		assertEquals(5, favour.masteryPoints)
		assertEquals(1, favour.enablePoints)

		assertEquals(0f, favour.hpModifier, margin)
		assertEquals(0f, favour.mpModifier, margin)
		assertEquals(0, favour.statModifiers.size)
		assertEquals(0, favour.resistances.elements.size)
		assertEquals(4, favour.resistances.effects.size)
		for (resistance in favour.resistances.effects) assertEquals(20, resistance.percentage)
		for (effectName in arrayOf("Poison", "Silence", "Sleep", "Paralysis")) {
			assertNotNull(favour.resistances.effects.find { it.effect.niceName == effectName })
		}
		assertEquals(0, favour.autoEffects.size)
		assertEquals(0, favour.sosEffects.size)
		assertEquals(0f, favour.experienceModifier, margin)
		assertEquals(0, favour.masteryModifier)
		assertEquals(0, favour.goldModifier)
		assertEquals(0, favour.addLootChance)
		assertEquals("Nature Magic", favour.skillClass!!.name)
	}

	@Test
	fun testHpPlusTenPercent() {
		val increaseHp = getPassive("HP+10%")
		assertEquals(0.1f, increaseHp.hpModifier, margin)
		assertEquals(4, increaseHp.enablePoints)

		assertEquals(0, increaseHp.resistances.effects.size)
	}

	@Test
	fun testMpPlusTenPercent() {
		val increaseMp = getPassive("MP+10%")
		assertEquals(0.1f, increaseMp.mpModifier, margin)
		assertEquals(10, increaseMp.masteryPoints)

		assertEquals(0f, increaseMp.hpModifier, margin)
	}

	@Test
	fun testStrengthPlusOne() {
		val increaseStrength = getPassive("STR+1")
		assertEquals(1, increaseStrength.statModifiers.size)
		val strengthModifier = increaseStrength.statModifiers[0]
		assertEquals("STR", strengthModifier.stat.flashName)
		assertEquals(1, strengthModifier.adder)

		assertEquals(0f, increaseStrength.mpModifier, margin)
	}

	@Test
	fun testRainbowAura() {
		val aura = getPassive("Rainbow Aura Lv.1")
		assertEquals(8, aura.resistances.elements.size)
		for (resistance in aura.resistances.elements) assertEquals(0.2f, resistance.modifier, margin)
		for (element in arrayOf("FIRE", "WATER", "LIGHT", "AETHER")) {
			assertNotNull(aura.resistances.elements.find { it.element.properName == element })
		}

		assertEquals(0, aura.statModifiers.size)
	}

	@Test
	fun testAutoShield() {
		val autoShield = getPassive("Auto-P.Shield")
		assertEquals(setOf(content.stats.statusEffects.find { it.flashName == "PSH" }!!), autoShield.autoEffects)
		assertEquals("LIGHT", autoShield.element.properName)

		assertEquals(0, autoShield.resistances.elements.size)
	}

	@Test
	fun testSosRegen() {
		val sosRegen = getPassive("SOS Regen")
		assertEquals(setOf(content.stats.statusEffects.find { it.niceName == "Regen" }!!), sosRegen.sosEffects)
		assertEquals("The Regen status effect triggers when the character is at 20% HP or less.", sosRegen.description)

		assertEquals(0, sosRegen.autoEffects.size)
	}

	@Test
	fun testExperiencePlusTwentyPercent() {
		val exp = getPassive("EXP+20%")
		assertEquals(0.2f, exp.experienceModifier, margin)
		assertEquals("DIVINE", exp.element.properName)

		assertEquals(0, exp.sosEffects.size)
	}

	@Test
	fun testDoubleMasteryPoints() {
		val doubleAp = getPassive("Double AP")
		assertEquals(doubleAp.masteryModifier, 1)
		assertEquals(200, doubleAp.masteryPoints)

		assertEquals(0f, doubleAp.experienceModifier, margin)
	}

	@Test
	fun testDoubleGold() {
		val doubleGold = getPassive("Double Gold")
		assertEquals(1, doubleGold.goldModifier)
		assertEquals(10, doubleGold.enablePoints)

		assertEquals(0, doubleGold.masteryModifier)
	}

	@Test
	fun testLootFinder() {
		val lootFinder = getPassive("Loot Finder Lv.1")
		assertEquals(5, lootFinder.addLootChance)
		assertEquals("DIVINE", lootFinder.element.rawName)

		assertEquals(0, lootFinder.goldModifier)
	}

	// TODO Test death
}
