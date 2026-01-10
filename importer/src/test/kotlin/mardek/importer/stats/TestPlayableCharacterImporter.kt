package mardek.importer.stats

import com.github.knokko.bitser.Bitser
import mardek.content.skill.ReactionSkillType
import mardek.importer.importVanillaContent
import mardek.state.ingame.CampaignState
import mardek.content.inventory.ItemStack
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestPlayableCharacterImporter {

	private val bitser = Bitser(false)
	private val content = importVanillaContent(bitser, true)

	@Test
	fun testImportHeroMardek() {
		val mardek = content.playableCharacters.find { it.areaSprites.name == "mardek_hero" }!!

		assertEquals("Mardek", mardek.name)
		assertEquals("LIGHT", mardek.element.properName)
		assertEquals(4, mardek.baseStats.size)
		assertEquals(20, mardek.baseStats.find { it.stat.flashName == "STR" }!!.adder)
		assertEquals(18, mardek.baseStats.find { it.stat.flashName == "VIT" }!!.adder)
		assertEquals(10, mardek.baseStats.find { it.stat.flashName == "SPR" }!!.adder)
		assertEquals(12, mardek.baseStats.find { it.stat.flashName == "AGL" }!!.adder)

		val hero = mardek.characterClass
		assertEquals("mardek_hero", hero.rawName)
		assertEquals("Hero", hero.displayName)
		assertEquals("WEAPON: SWORD", hero.equipmentSlots[0].itemTypes.first().displayName)
		assertEquals("SHIELD", hero.equipmentSlots[1].itemTypes.joinToString())
		assertEquals("HELMET: FULL HELM", hero.equipmentSlots[2].itemTypes.joinToString())
		assertTrue(hero.equipmentSlots[3].itemTypes.any { it.displayName == "ARMOUR: HEAVY" })

		val skills = hero.skillClass
		assertEquals("Hero", skills.key)
		assertEquals("Powers", skills.name)
		assertEquals(6, skills.actions.size)

		fun activeSkill(name: String) = skills.actions.find { it.name == name }!!
		activeSkill("Smite Evil")

		val chapter1 = CampaignState.loadChapter(content, 1)

		val state = chapter1.characterStates[mardek]!!
		assertEquals(50, state.currentLevel)
		assertEquals(4299, state.currentHealth)
		assertEquals(122, state.currentMana)
		assertEquals(0, state.activeStatusEffects.size)

		fun item(name: String) = content.items.items.find { it.displayName == name }!!
		val expectedEquipment = mapOf(
			Pair(hero.equipmentSlots[0], item("M Blade")),
			Pair(hero.equipmentSlots[1], item("Hero's Shield")),
			Pair(hero.equipmentSlots[3], item("Hero's Armour")),
			Pair(hero.equipmentSlots[4], item("Dragon Amulet")),
		)
		assertEquals(expectedEquipment, state.equipment)

		val expectedInventory = Array<ItemStack?>(64) { null }
		expectedInventory[0] = ItemStack(item("Elixir"), 9)
		assertArrayEquals(expectedInventory, state.inventory)

		fun passiveSkill(name: String) = content.skills.passiveSkills.find { it.name == name }!!
		fun reactionSkill(type: ReactionSkillType, name: String) = content.skills.reactionSkills.find {
			it.type == type && it.name == name
		}!!

		assertEquals(13, state.skillMastery.size)
		assertEquals(20, state.skillMastery[activeSkill("Shock")])
		assertEquals(50, state.skillMastery[passiveSkill("Auto-Regen")])
		assertEquals(20, state.skillMastery[reactionSkill(ReactionSkillType.RangedDefense, "Nullify Magic")])

		assertEquals(10, state.toggledSkills.size)
		assertTrue(state.toggledSkills.contains(passiveSkill("HP+50%")))
		assertTrue(state.toggledSkills.contains(reactionSkill(ReactionSkillType.MeleeAttack, "Quarry: DRAGON")))
	}
}
