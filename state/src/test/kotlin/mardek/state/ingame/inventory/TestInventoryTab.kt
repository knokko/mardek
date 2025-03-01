package mardek.state.ingame.inventory

import mardek.assets.animations.BattleModel
import mardek.assets.characters.PlayableCharacter
import mardek.assets.combat.CharacterClass
import mardek.assets.combat.Element
import mardek.assets.combat.Resistances
import mardek.assets.inventory.*
import mardek.assets.skill.SkillClass
import mardek.assets.sprite.DirectionalSprites
import mardek.input.InputKey
import mardek.state.SoundQueue
import mardek.state.ingame.CampaignState
import mardek.state.ingame.characters.CharacterSelectionState
import mardek.state.ingame.characters.CharacterState
import mardek.state.ingame.menu.InventoryTab
import mardek.state.ingame.menu.ItemReference
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

private fun createState(): CampaignState {
	val mardekClass = CharacterClass(
		rawName = "knight",
		displayName = "Royal Knight",
		skillClass = SkillClass(),
		weaponType = WeaponType(),
		armorTypes = arrayListOf(ArmorType("Sh", "Shield", EquipmentSlotType.OffHand))
	)
	val mardek = PlayableCharacter("Mardek", mardekClass, Element(), ArrayList(), DirectionalSprites(), BattleModel())
	val mardekState = CharacterState()

	return CampaignState(
		currentArea = null,
		characterSelection = CharacterSelectionState(
			available = hashSetOf(mardek),
			unavailable = HashSet(0),
			party = arrayOf(mardek, null, null, null)
		),
		characterStates = hashMapOf(Pair(mardek, mardekState)),
		gold = 0
	)
}

class TestInventoryTab {

	private val state = createState()
	private val mardek = state.characterStates.keys.first()
	private val mardekState = state.characterStates.values.first()
	private val soundQueue = SoundQueue()
	private val tab = InventoryTab(state)

	init {
		tab.inside = true
	}

	private fun putStack(index: Int, stack: ItemStack): ItemReference {
		mardekState.inventory[index] = stack
		return ItemReference(mardek, mardekState, index)
	}

	private fun putEquipment(index: Int, item: Item): ItemReference {
		mardekState.equipment[index] = item
		return ItemReference(mardek, mardekState, -index - 1)
	}

	private fun createEquipmentItem(equipment: EquipmentProperties) = Item(
		flashName = "some-equipment",
		description = "the description",
		type = ItemType("lala", false),
		element = Element(),
		cost = 123,
		equipment = equipment,
		consumable = null
	)

	private fun createEquipment(
		weapon: WeaponProperties? = null,
		armorType: ArmorType? = null,
		onlyUser: String? = null
	) = createEquipmentItem(EquipmentProperties(
		skills = ArrayList(0),
		stats = ArrayList(0),
		elementalBonuses = ArrayList(0),
		resistances = Resistances(),
		autoEffects = ArrayList(0),
		weapon = weapon,
		armorType = armorType,
		gem = null,
		onlyUser = onlyUser,
		charismaticPerformanceChance = 0
	))

	private fun createWeapon(type: WeaponType) = createEquipment(weapon = WeaponProperties(
		type = type,
		critChance = 0,
		hitChance = 100,
		hpDrain = 0f,
		effectiveAgainstCreatureTypes = ArrayList(0),
		effectiveAgainstElements = ArrayList(0),
		addEffects = ArrayList(0),
		hitSound = null
	))

	@Test
	fun testInsertItemIntoEmptySlot() {
		val item = Item()

		tab.pickedUpItem = putStack(10, ItemStack(item, 3))
		tab.hoveringItem = ItemReference(mardek, mardekState, 11)
		tab.processKeyPress(InputKey.Interact, soundQueue)
		assertEquals("click-cancel", soundQueue.take())

		assertNull(tab.pickedUpItem)
		assertEquals(tab.hoveringItem, ItemReference(mardek, mardekState, 11))
		assertNull(mardekState.inventory[10])
		assertEquals(ItemStack(item, 3), mardekState.inventory[11])
	}

	@Test
	fun testPickItemWithNothingOnCursor() {
		val item = Item()

		tab.hoveringItem = putStack(5, ItemStack(item, 1))
		tab.processKeyPress(InputKey.Interact, soundQueue)
		assertEquals("click-confirm", soundQueue.take())
		assertEquals(tab.hoveringItem, tab.pickedUpItem)
	}

	@Test
	fun testCanUnEquipShield() {
		val shield = createEquipment(armorType = mardek.characterClass.armorTypes.first())

		tab.hoveringItem = putEquipment(1, shield)
		assertEquals(shield, mardekState.equipment[1])
		tab.processKeyPress(InputKey.Interact, soundQueue)
		assertEquals("click-confirm", soundQueue.take())
		assertEquals(tab.hoveringItem, tab.pickedUpItem)

		tab.processKeyPress(InputKey.Interact, soundQueue)
		assertEquals("click-cancel", soundQueue.take())
		assertNull(tab.pickedUpItem)
		assertEquals(ItemStack(shield, 1), tab.hoveringItem!!.get())

		tab.processKeyPress(InputKey.Interact, soundQueue)
		assertEquals("click-confirm", soundQueue.take())
		assertEquals(tab.hoveringItem, tab.pickedUpItem)

		tab.hoveringItem = ItemReference(mardek, mardekState, 5)
		tab.processKeyPress(InputKey.Interact, soundQueue)
		assertEquals("click-cancel", soundQueue.take())
		assertNull(tab.pickedUpItem)
		assertEquals(ItemStack(shield, 1), mardekState.inventory[5])
		assertNull(mardekState.equipment[1])
	}

	@Test
	fun testCanNotPutShieldInArmorSlot() {
		val shield = createEquipment(armorType = mardek.characterClass.armorTypes.first())

		tab.hoveringItem = putEquipment(1, shield)
		assertEquals(shield, mardekState.equipment[1])
		tab.processKeyPress(InputKey.Interact, soundQueue)
		assertEquals("click-confirm", soundQueue.take())
		assertEquals(tab.hoveringItem, tab.pickedUpItem)

		tab.hoveringItem = ItemReference(mardek, mardekState, -4)
		tab.processKeyPress(InputKey.Interact, soundQueue)
		assertEquals("click-reject", soundQueue.take())
		assertEquals(ItemReference(mardek, mardekState, -2), tab.pickedUpItem)
		assertEquals(shield, mardekState.equipment[1])
		assertNull(mardekState.equipment[3])
	}

	@Test
	fun testCanNotPutShieldInWeaponSlot() {
		val shield = createEquipment(armorType = mardek.characterClass.armorTypes.first())
		val weapon = createWeapon(mardek.characterClass.weaponType!!)
		tab.hoveringItem = putEquipment(0, weapon)
		tab.pickedUpItem = putEquipment(1, shield)

		tab.processKeyPress(InputKey.Interact, soundQueue)
		assertEquals("click-reject", soundQueue.take())
		assertEquals(ItemStack(shield, 1), tab.pickedUpItem!!.get())
		assertEquals(ItemStack(weapon, 1), tab.hoveringItem!!.get())
	}

	@Test
	fun testSwapWeapons() {
		val oldWeapon = createWeapon(mardek.characterClass.weaponType!!)
		val newWeapon = createWeapon(mardek.characterClass.weaponType!!)
		tab.pickedUpItem = putStack(0, ItemStack(newWeapon, 1))
		tab.hoveringItem = putEquipment(0, oldWeapon)

		tab.processKeyPress(InputKey.Interact, soundQueue)
		assertEquals("click-confirm", soundQueue.take())
		assertEquals(ItemStack(oldWeapon, 1), tab.pickedUpItem!!.get())
		assertEquals(ItemStack(newWeapon, 1), tab.hoveringItem!!.get())
		assertSame(newWeapon, mardekState.equipment[0])
		assertSame(oldWeapon, mardekState.inventory[0]!!.item)
	}

	@Test
	fun testCanNotTakeWeapon() {
		val weapon = createWeapon(mardek.characterClass.weaponType!!)
		tab.hoveringItem = putEquipment(0, weapon)

		tab.processKeyPress(InputKey.Interact, soundQueue)
		assertEquals("click-reject", soundQueue.take())
		assertSame(weapon, mardekState.equipment[0])
		assertNull(tab.pickedUpItem)
	}

	@Test
	fun testCanNotEquipWrongWeapon() {
		val goodWeapon = createWeapon(mardek.characterClass.weaponType!!)
		val wrongWeapon = createWeapon(WeaponType())
		tab.hoveringItem = putEquipment(0, goodWeapon)
		tab.pickedUpItem = putStack(12, ItemStack(wrongWeapon, 1))

		tab.processKeyPress(InputKey.Interact, soundQueue)
		assertEquals("click-reject", soundQueue.take())
		assertSame(goodWeapon, mardekState.equipment[0])
	}

	@Test
	fun testRespectOnlyUser() {
		val badShield = createEquipment(armorType = mardek.characterClass.armorTypes.first(), onlyUser = "Emela")
		val goodShield = createEquipment(armorType = mardek.characterClass.armorTypes.first(), onlyUser = "Mardek")
		tab.pickedUpItem = putStack(3, ItemStack(badShield, 1))
		tab.hoveringItem = ItemReference(mardek, mardekState, -2)

		tab.processKeyPress(InputKey.Interact, soundQueue)
		assertEquals("click-reject", soundQueue.take())
		assertNull(mardekState.equipment[1])

		tab.pickedUpItem = putStack(2, ItemStack(goodShield, 1))
		tab.processKeyPress(InputKey.Interact, soundQueue)
		assertEquals("click-cancel", soundQueue.take())
		assertSame(goodShield, mardekState.equipment[1])
	}
}
