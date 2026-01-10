package mardek.state.ingame.inventory

import mardek.content.animation.CombatantAnimations
import mardek.content.audio.FixedSoundEffects
import mardek.content.characters.PlayableCharacter
import mardek.content.stats.CharacterClass
import mardek.content.stats.Element
import mardek.content.stats.Resistances
import mardek.content.inventory.*
import mardek.content.portrait.PortraitInfo
import mardek.content.skill.SkillClass
import mardek.content.skill.SkillsContent
import mardek.content.sprite.DirectionalSprites
import mardek.content.stats.CreatureType
import mardek.input.InputKey
import mardek.state.SoundQueue
import mardek.state.ingame.CampaignState
import mardek.content.characters.CharacterState
import mardek.content.sprite.KimSprite
import mardek.state.ingame.menu.inventory.InventoryTab
import mardek.state.ingame.menu.UiUpdateContext
import mardek.state.ingame.menu.inventory.EquipmentSlotReference
import mardek.state.ingame.menu.inventory.InventorySlotReference
import mardek.state.ingame.menu.inventory.ItemSlotReference
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

private fun createState(): CampaignState {
	val mardekClass = CharacterClass(
		rawName = "knight",
		displayName = "Royal Knight",
		skillClass = SkillClass(),
		equipmentSlots = arrayOf(
			EquipmentSlot(
				id = UUID.randomUUID(),
				displayName = "",
				itemTypes = arrayOf(ItemType()),
				canBeEmpty = false,
			),
			EquipmentSlot(
				id = UUID.randomUUID(),
				displayName = "",
				itemTypes = arrayOf(ItemType()),
				canBeEmpty = true,
			),
			EquipmentSlot(
				id = UUID.randomUUID(),
				displayName = "",
				itemTypes = arrayOf(ItemType()),
				canBeEmpty = true,
			),
			EquipmentSlot(
				id = UUID.randomUUID(),
				displayName = "",
				itemTypes = arrayOf(ItemType()),
				canBeEmpty = true,
			),
		),
	)
	val mardek = PlayableCharacter(
		"Mardek", mardekClass, Element(),
		ArrayList(), DirectionalSprites(),
		CombatantAnimations(), CreatureType(), PortraitInfo(), UUID.randomUUID(),
	)
	val mardekState = CharacterState()

	val campaignState = CampaignState()
	campaignState.party[0] = mardek
	campaignState.characterStates[mardek] = mardekState
	return campaignState
}

class TestInventoryTab {

	private val state = createState()
	private val mardek = state.characterStates.keys.first()
	private val mardekState = state.characterStates.values.first()
	private val soundQueue = SoundQueue()
	private val tab = InventoryTab()
	private val context = UiUpdateContext(
		state.usedPartyMembers(), state.allPartyMembers(),
		soundQueue, FixedSoundEffects(), SkillsContent(),
		{ state.cursorItemStack }, { newStack -> state.cursorItemStack = newStack },
	)

	init {
		tab.inside = true
	}

	private fun putStack(index: Int, stack: ItemStack): ItemSlotReference {
		mardekState.inventory[index] = stack
		return InventorySlotReference(mardekState.inventory, index)
	}

	private fun putEquipment(index: Int, item: Item): EquipmentSlotReference {
		mardekState.equipment[mardek.characterClass.equipmentSlots[index]] = item
		return EquipmentSlotReference(
			mardek, mardekState.equipment, mardek.characterClass.equipmentSlots[index]
		)
	}

	private fun createEquipmentItem(equipment: EquipmentProperties, type: ItemType) = Item(
		id = UUID.randomUUID(),
		displayName = "some-equipment",
		sprite = KimSprite(),
		description = "the description",
		type = type,
		element = Element(),
		cost = 123,
		equipment = equipment,
		consumable = null,
	)

	private fun createEquipment(
		weapon: WeaponProperties? = null,
		armorType: ItemType,
		onlyUser: PlayableCharacter? = null
	) = createEquipmentItem(EquipmentProperties(
		skills = ArrayList(0),
		stats = ArrayList(0),
		elementalBonuses = ArrayList(0),
		resistances = Resistances(),
		autoEffects = ArrayList(0),
		weapon = weapon,
		gem = null,
		onlyUser = onlyUser,
		charismaticPerformanceChance = 0
	), armorType)

	private fun createWeapon(type: ItemType) = createEquipment(weapon = WeaponProperties(
		hitChance = 100,
		critChance = 0,
		hpDrain = 0f,
		mpDrain = 0f,
		effectiveAgainstCreatureTypes = ArrayList(0),
		effectiveAgainstElements = ArrayList(0),
		addEffects = ArrayList(0),
		hitSound = null
	), type)

	@Test
	fun testInsertItemIntoEmptySlot() {
		val item = Item()

		state.cursorItemStack = ItemStack(item, 3)
		tab.interaction.hoveredSlot = InventorySlotReference(mardekState.inventory, 11)
		tab.processKeyPress(InputKey.Interact, context)
		assertSame(context.sounds.ui.clickCancel, soundQueue.take())

		assertNull(state.cursorItemStack)
		assertEquals(tab.interaction.hoveredSlot, InventorySlotReference(mardekState.inventory, 11))
		assertNull(mardekState.inventory[10])
		assertEquals(ItemStack(item, 3), mardekState.inventory[11])
	}

	@Test
	fun testPickItemWithNothingOnCursor() {
		val item = Item()

		tab.interaction.hoveredSlot = putStack(5, ItemStack(item, 1))
		tab.processKeyPress(InputKey.Interact, context)
		assertSame(context.sounds.ui.clickConfirm, soundQueue.take())
		assertNull(tab.interaction.hoveredSlot!!.get())
		assertEquals(ItemStack(item, 1), state.cursorItemStack)
	}

	@Test
	fun testCanUnEquipShield() {
		val shield = createEquipment(armorType = mardek.characterClass.equipmentSlots[1].itemTypes.first())

		tab.interaction.hoveredSlot = putEquipment(1, shield)
		tab.processKeyPress(InputKey.Interact, context)
		assertSame(context.sounds.ui.clickConfirm, soundQueue.take())
		assertNull(mardekState.equipment[mardek.characterClass.equipmentSlots[0]])
		assertNull(tab.interaction.hoveredSlot!!.get())
		assertEquals(ItemStack(shield, 1), state.cursorItemStack)

		tab.processKeyPress(InputKey.Interact, context)
		assertSame(context.sounds.ui.clickCancel, soundQueue.take())
		assertNull(state.cursorItemStack)
		assertEquals(ItemStack(shield, 1), tab.interaction.hoveredSlot!!.get())

		tab.processKeyPress(InputKey.Interact, context)
		assertSame(context.sounds.ui.clickConfirm, soundQueue.take())
		assertNull(tab.interaction.hoveredSlot!!.get())
		assertEquals(ItemStack(shield, 1), state.cursorItemStack)

		tab.interaction.hoveredSlot = InventorySlotReference(mardekState.inventory, 5)
		tab.processKeyPress(InputKey.Interact, context)
		assertSame(context.sounds.ui.clickCancel, soundQueue.take())
		assertNull(state.cursorItemStack)
		assertEquals(ItemStack(shield, 1), mardekState.inventory[5])
		assertNull(mardekState.equipment[mardek.characterClass.equipmentSlots[1]])
	}

	@Test
	fun testCanNotPutShieldInArmorSlot() {
		val shield = createEquipment(armorType = mardek.characterClass.equipmentSlots[1].itemTypes.first())

		tab.interaction.hoveredSlot = putEquipment(1, shield)
		assertEquals(shield, mardekState.equipment[mardek.characterClass.equipmentSlots[1]])
		tab.processKeyPress(InputKey.Interact, context)
		assertSame(context.sounds.ui.clickConfirm, soundQueue.take())

		tab.interaction.hoveredSlot = EquipmentSlotReference(
			mardek, mardekState.equipment, mardek.characterClass.equipmentSlots[3]
		)
		tab.processKeyPress(InputKey.Interact, context)
		assertSame(context.sounds.ui.clickReject, soundQueue.take())
		assertEquals(ItemStack(shield, 1), state.cursorItemStack)
		assertNull(mardekState.equipment[mardek.characterClass.equipmentSlots[1]])
		assertNull(mardekState.equipment[mardek.characterClass.equipmentSlots[3]])
	}

	@Test
	fun testCanNotPutShieldInWeaponSlot() {
		val shield = createEquipment(armorType = mardek.characterClass.equipmentSlots[1].itemTypes.first())
		val weapon = createWeapon(mardek.characterClass.equipmentSlots[0].itemTypes.first())
		tab.interaction.hoveredSlot = putEquipment(0, weapon)
		state.cursorItemStack = ItemStack(shield, 1)

		tab.processKeyPress(InputKey.Interact, context)
		assertSame(context.sounds.ui.clickReject, soundQueue.take())
		assertEquals(ItemStack(shield, 1), state.cursorItemStack)
		assertEquals(ItemStack(weapon, 1), tab.interaction.hoveredSlot!!.get())
	}

	@Test
	fun testSwapWeapons() {
		val weaponType = mardek.characterClass.equipmentSlots[0].itemTypes.first()
		val oldWeapon = createWeapon(weaponType)
		val newWeapon = createWeapon(weaponType)
		state.cursorItemStack = ItemStack(newWeapon, 1)
		tab.interaction.hoveredSlot = putEquipment(0, oldWeapon)

		tab.processKeyPress(InputKey.Interact, context)
		assertSame(context.sounds.ui.clickConfirm, soundQueue.take())
		assertEquals(ItemStack(oldWeapon, 1), state.cursorItemStack)
		assertEquals(ItemStack(newWeapon, 1), tab.interaction.hoveredSlot!!.get())
		assertSame(newWeapon, mardekState.equipment[mardek.characterClass.equipmentSlots[0]])
	}

	@Test
	fun testCanNotTakeWeapon() {
		val weapon = createWeapon(mardek.characterClass.equipmentSlots[0].itemTypes.first())
		tab.interaction.hoveredSlot = putEquipment(0, weapon)

		tab.processKeyPress(InputKey.Interact, context)
		assertSame(context.sounds.ui.clickReject, soundQueue.take())
		assertSame(weapon, mardekState.equipment[mardek.characterClass.equipmentSlots[0]])
		assertNull(state.cursorItemStack)
	}

	@Test
	fun testCanNotEquipWrongWeapon() {
		val goodWeapon = createWeapon(mardek.characterClass.equipmentSlots[0].itemTypes.first())
		val wrongWeapon = createWeapon(ItemType())
		tab.interaction.hoveredSlot = putEquipment(0, goodWeapon)
		state.cursorItemStack = ItemStack(wrongWeapon, 1)

		tab.processKeyPress(InputKey.Interact, context)
		assertSame(context.sounds.ui.clickReject, soundQueue.take())
		assertSame(goodWeapon, mardekState.equipment[mardek.characterClass.equipmentSlots[0]])
	}

	@Test
	fun testRespectOnlyUser() {
		val shieldType = mardek.characterClass.equipmentSlots[1].itemTypes.first()
		val badShield = createEquipment(armorType = shieldType, onlyUser = PlayableCharacter())
		val goodShield = createEquipment(armorType = shieldType, onlyUser = mardek)
		state.cursorItemStack = ItemStack(badShield, 1)
		tab.interaction.hoveredSlot = EquipmentSlotReference(
			mardek, mardekState.equipment, mardek.characterClass.equipmentSlots[1]
		)

		tab.processKeyPress(InputKey.Interact, context)
		assertSame(context.sounds.ui.clickReject, soundQueue.take())
		assertNull(mardekState.equipment[mardek.characterClass.equipmentSlots[1]])

		state.cursorItemStack = ItemStack(goodShield, 1)
		tab.processKeyPress(InputKey.Interact, context)
		assertSame(context.sounds.ui.clickCancel, soundQueue.take())
		assertSame(goodShield, mardekState.equipment[mardek.characterClass.equipmentSlots[1]])
	}
}
