package mardek.importer.skills

import mardek.assets.combat.CombatAssets
import mardek.assets.skill.*
import mardek.importer.combat.addCombatStats
import mardek.importer.combat.addElements
import mardek.importer.combat.addStatusEffects
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
		combatAssets = CombatAssets()
		addCombatStats(combatAssets)
		addElements(combatAssets)
		addStatusEffects(combatAssets)
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
}
