package mardek.importer.stats

import com.github.knokko.bitser.io.BitInputStream
import com.github.knokko.bitser.serialize.Bitser
import mardek.content.skill.ReactionSkillType
import mardek.importer.importVanillaContent
import mardek.state.ingame.CampaignState
import mardek.content.inventory.ItemStack
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.ByteArrayInputStream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestPlayableCharacterImporter {

	private val bitser = Bitser(false)
	private val campaign = importVanillaContent(bitser, true)

	@Test
	fun testImportHeroMardek() {
		val mardek = campaign.playableCharacters.find { it.areaSprites.name == "mardek_hero" }!!

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
		assertEquals("SWORD", hero.weaponType!!.flashName)
		assertEquals(4, hero.armorTypes.size)
		assertNotNull(hero.armorTypes.find { it.name == "FULL HELM" })
		assertNotNull(hero.armorTypes.find { it.name == "HEAVY ARMOUR" })

		val skills = hero.skillClass
		assertEquals("Hero", skills.key)
		assertEquals("Powers", skills.name)
		assertEquals(6, skills.actions.size)

		fun activeSkill(name: String) = skills.actions.find { it.name == name }!!
		activeSkill("Smite Evil")

		val input1 = BitInputStream(ByteArrayInputStream(campaign.checkpoints["chapter1"]!!))
		val chapter1 = bitser.deserialize(CampaignState::class.java, input1, campaign, Bitser.BACKWARD_COMPATIBLE)

		val state = chapter1.characterStates[mardek]!!
		assertEquals(50, state.currentLevel)
		assertEquals(4299, state.currentHealth)
		assertEquals(122, state.currentMana)
		assertEquals(0, state.activeStatusEffects.size)

		fun item(name: String) = campaign.items.items.find { it.flashName == name }!!
		val expectedEquipment = arrayOf(
			item("M Blade"), item("Hero's Shield"), null,
			item("Hero's Armour"), item("Dragon Amulet"), null
		)
		assertArrayEquals(expectedEquipment, state.equipment)

		val expectedInventory = Array<ItemStack?>(64) { null }
		expectedInventory[0] = ItemStack(item("Elixir"), 9)
		assertArrayEquals(expectedInventory, state.inventory)

		fun passiveSkill(name: String) = campaign.skills.passiveSkills.find { it.name == name }!!
		fun reactionSkill(type: ReactionSkillType, name: String) = campaign.skills.reactionSkills.find {
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
