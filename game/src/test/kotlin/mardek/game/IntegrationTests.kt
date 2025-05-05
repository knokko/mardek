package mardek.game

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.builders.BoilerBuilder
import com.github.knokko.boiler.commands.SingleTimeCommands
import com.github.knokko.boiler.images.ImageBuilder
import com.github.knokko.boiler.synchronization.ResourceUsage
import java.io.File
import mardek.content.Content
import mardek.content.area.Area
import mardek.content.characters.PlayableCharacter
import mardek.content.inventory.Item
import mardek.content.inventory.ItemStack
import mardek.content.skill.ActiveSkill
import mardek.input.InputKey
import mardek.input.InputKeyEvent
import mardek.input.InputManager
import mardek.renderer.GameRenderer
import mardek.renderer.SharedResources
import mardek.state.SoundQueue
import mardek.state.ingame.CampaignState
import mardek.state.ingame.InGameState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.battle.*
import mardek.state.ingame.characters.CharacterSelectionState
import mardek.state.ingame.characters.CharacterState
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.lwjgl.vulkan.VK10.*
import java.awt.Color
import java.lang.Thread.sleep
import java.util.concurrent.CompletableFuture
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntegrationTests {

	private val actualResultsDirectory = File("rendering-test-results/actual")

	private lateinit var boiler: BoilerInstance
	private lateinit var content: Content
	private lateinit var resources: SharedResources

	private val getResources = CompletableFuture<SharedResources>()

	private lateinit var dragonLairEntry: Area
	private lateinit var dragonLair2: Area
	private lateinit var heroMardek: PlayableCharacter
	private lateinit var heroDeugan: PlayableCharacter
	private lateinit var shock: ActiveSkill
	private lateinit var frostasia: ActiveSkill
	private lateinit var elixir: Item

	private fun nearlyEquals(expectedComponent: Int, actualComponent: Int) = abs(expectedComponent - actualComponent) <= 1

	private fun nearlyEquals(expected: Color, actual: Color) = nearlyEquals(expected.red, actual.red) &&
			nearlyEquals(expected.green, actual.green) && nearlyEquals(expected.blue, actual.blue) &&
			nearlyEquals(expected.alpha, actual.alpha)

	private fun simpleCharacterSelectionState() = CharacterSelectionState(
		available = hashSetOf(heroMardek, heroDeugan),
		unavailable = HashSet(),
		party = arrayOf(heroMardek, heroDeugan, null, null)
	)

	private fun simpleCharacterStates() = run {
		val mardekState = CharacterState()
		mardekState.equipment[0] = content.items.items.find { it.flashName == "M Blade" }!!
		val deuganState = CharacterState()
		deuganState.equipment[0] = content.items.items.find { it.flashName == "Balmung" }!!
		deuganState.inventory[0] = ItemStack(elixir, 1)
		deuganState.skillMastery[shock] = shock.masteryPoints
		deuganState.skillMastery[frostasia] = frostasia.masteryPoints
		hashMapOf(Pair(heroMardek, mardekState), Pair(heroDeugan, deuganState))
	}

	private fun pressKeyEvent(key: InputKey) = InputKeyEvent(key, didPress = true, didRelease = false, didRepeat = false)

	private fun repeatKeyEvent(key: InputKey) = InputKeyEvent(key, didPress = true, didRelease = false, didRepeat = true)

	private fun releaseKeyEvent(key: InputKey) = InputKeyEvent(key, didPress = false, didRelease = true, didRepeat = false)

	@BeforeAll
	fun importResources() {
		if (!actualResultsDirectory.exists() && !actualResultsDirectory.mkdir()) {
			throw RuntimeException("Failed to create $actualResultsDirectory")
		}
		content = Content.load("mardek/game/content.bin")

		val builder = BoilerBuilder(VK_API_VERSION_1_0, "IntegrationTests", 1)
		GameRenderer.addBoilerRequirements(builder)
		boiler = builder.validation().forbidValidationErrors().build()

		val getBoiler = CompletableFuture<BoilerInstance>()
		getBoiler.complete(boiler)
		resources = SharedResources(getBoiler, 1, skipWindow = true)
		getResources.complete(resources)

		dragonLairEntry = content.areas.areas.find { it.properties.rawName == "DL_entr" }!!
		dragonLair2 = content.areas.areas.find { it.properties.rawName == "DL_area2" }!!
		heroMardek = content.playableCharacters.find { it.characterClass.rawName == "mardek_hero" }!!
		heroDeugan = content.playableCharacters.find { it.characterClass.rawName == "deugan_hero" }!!

		elixir = content.items.items.find { it.flashName == "Elixir" }!!
		shock = heroDeugan.characterClass.skillClass.actions.find { it.name == "Shock" }!!
		frostasia = heroDeugan.characterClass.skillClass.actions.find { it.name == "Frostasia" }!!
	}

	@AfterAll
	fun cleanUp() {
		resources.destroy()
		boiler.destroyInitialObjects()
	}

	private fun testRendering(
		state: InGameState, width: Int, height: Int, name: String,
		expectedColors: Array<Color>, forbiddenColors: Array<Color>,
	) {
		val renderer = GameRenderer(boiler, getResources)

		val targetImage = ImageBuilder("TargetImage($name)", width, height)
			.format(VK_FORMAT_R8G8B8A8_SRGB)
			.setUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT or VK_IMAGE_USAGE_TRANSFER_SRC_BIT)
			.build(boiler)
		val framebuffer = boiler.images.createFramebuffer(
			resources.renderPass, width, height, "Framebuffer($name)", targetImage.vkImageView
		)
		val destinationBuffer = boiler.buffers.createMapped(
			4L * width * height, VK_BUFFER_USAGE_TRANSFER_DST_BIT, "DestinationBuffer($name)"
		)

		val commands = SingleTimeCommands(boiler)
		commands.submit(name) { recorder ->
			recorder.transitionLayout(targetImage, null, ResourceUsage.COLOR_ATTACHMENT_WRITE)
			renderer.render(state, recorder, targetImage, framebuffer, 0)
			recorder.transitionLayout(targetImage, ResourceUsage.COLOR_ATTACHMENT_WRITE, ResourceUsage.TRANSFER_SOURCE)
			recorder.copyImageToBuffer(targetImage, destinationBuffer.fullRange())
		}
		commands.destroy()

		val result = boiler.buffers.decodeBufferedImageRGBA(destinationBuffer, 0L, width, height)
		ImageIO.write(result, "PNG", File("$actualResultsDirectory/$name.png"))
		destinationBuffer.destroy(boiler)
		vkDestroyFramebuffer(boiler.vkDevice(), framebuffer, null)
		targetImage.destroy(boiler)

		for (color in expectedColors) {
			assertTrue((0 until result.width).any { x -> (0 until result.height).any { y ->
				nearlyEquals(color, Color(result.getRGB(x, y), true))
			} }, "Expected color $color, but did not find it")
		}

		for (color in forbiddenColors) {
			assertFalse((0 until result.width).any { x -> (0 until result.height).any { y ->
				nearlyEquals(color, Color(result.getRGB(x, y), true))
			} }, "Expected not to find color $color, but found it anyway")
		}
	}

	@Test
	fun testDragonLairDoor() {
		val state = InGameState(content, CampaignState(
			currentArea = AreaState(dragonLairEntry, AreaPosition(5, 8)),
			characterSelection = simpleCharacterSelectionState(),
			characterStates = simpleCharacterStates(),
			gold = 123
		))

		val doorColor = Color(59, 34, 22)
		val hairColorDeugan = Color(195, 156, 77)
		val expectedEntryColors = arrayOf(
			Color(59, 53, 66), // color between floor tiles
			Color(91, 79, 106), // light color of floor tiles
			Color(0, 0, 0), // black for the background
			Color(86, 50, 86), // color of braziers
			Color(101, 50, 0), // hair color of Mardek
			Color(69, 117, 28), // cape color of Deugan
			Color(96, 199, 242), // the save crystal
		)

		testRendering(
			state, 1000, 800, "dragon-lair-entry1",
			expectedEntryColors + doorColor, arrayOf(hairColorDeugan)
		)

		val dummySoundQueue = SoundQueue()
		val fakeInput = InputManager()
		fakeInput.postEvent(InputKeyEvent(InputKey.MoveUp, didPress = true, didRelease = false, didRepeat = false))

		for (counter in 0 until 5000) {
			state.update(fakeInput, 10.milliseconds, dummySoundQueue)
		}

		testRendering(
			state, 1000, 800, "dragon-lair-entry2",
			expectedEntryColors + hairColorDeugan, emptyArray()
		)

		fakeInput.postEvent(InputKeyEvent(InputKey.MoveUp, didPress = false, didRelease = true, didRepeat = false))
		fakeInput.postEvent(InputKeyEvent(InputKey.Interact, didPress = true, didRelease = false, didRepeat = false))

		for (counter in 0 until 2000) {
			state.update(fakeInput, 10.milliseconds, dummySoundQueue)
		}

		val expectedRoomColors = arrayOf(
			Color(59, 53, 66), // color between floor tiles
			Color(91, 79, 106), // light color of floor tiles
			Color(0, 0, 0), // black for the background
			Color(0, 0, 13), // dark blue for the rest of the background
			Color(101, 50, 0), // hair color of Mardek
		)
		testRendering(
			state, 1000, 800, "dragon-lair-room2",
			expectedRoomColors, arrayOf(hairColorDeugan)
		)
	}

	@Test
	fun testBattleMoveSelection() {
		val state = InGameState(content, CampaignState(
			currentArea = AreaState(dragonLair2, AreaPosition(10, 10)),
			characterSelection = simpleCharacterSelectionState(),
			characterStates = simpleCharacterStates(),
			gold = 123
		))
		val mardekState = state.campaign.characterStates[heroMardek]!!
		val deuganState = state.campaign.characterStates[heroDeugan]!!
		mardekState.currentHealth = 20
		deuganState.currentHealth = deuganState.determineMaxHealth(heroDeugan.baseStats)
		mardekState.currentMana = mardekState.determineMaxMana(heroMardek.baseStats)
		deuganState.currentMana = 20
		state.campaign.currentArea!!.activeBattle = BattleState(
			battle = Battle(
				enemies = arrayOf(null, Enemy(monster = content.battle.monsters.find {
					it.name == "monster"
				}!!, level = 10), null, null),
				enemyLayout = content.battle.enemyPartyLayouts.find { it.name == "TRIO" }!!,
				music = "peak",
				background = content.battle.backgrounds.find { it.name == "volcano" }!!
			),
			players = arrayOf(heroMardek, null, heroDeugan, null),
			playerLayout = content.battle.enemyPartyLayouts.find { it.name == "DEFAULT" }!!,
			campaignState = state.campaign
		)


		val battle = state.campaign.currentArea!!.activeBattle!!

		val backgroundColors = arrayOf(
			Color(198, 4, 0), // one of the lava colors
			Color(0, 0, 16), // dark lava color
		)

		val barColors = arrayOf(
			Color(14, 243, 8), // earth element color
			Color(58, 108, 25), // full health bar color
			Color(127, 231, 56), // full health bar text color
			Color(131, 94, 32), // half health bar color
			Color(207, 230, 56), // half health bar text color
			Color(38, 109, 129), // mana bar color
			Color(34, 247, 255), // mana bar text color
			Color(181, 146, 70), // xp bar color
			Color(251, 225, 99), // xp bar text color
			Color(59, 42, 28), // bar background color
		)

		val monsterColors = arrayOf(
			Color(85, 56, 133), // skin color of monster
			Color(74, 49, 117), // 'back' skin color of monster
			Color(255, 255, 204), // teeth color of monster
		)

		val mardekColors = arrayOf(
			Color(129, 129, 79), // pants color of battle model of Mardek
		)

		val deuganColors = arrayOf(
			Color(195, 157, 79), // hair color of battle model of Deugan
		)

		val turnOrderColors = arrayOf(
			Color(88, 64, 28), // one of the turn order monster icon colors
		)

		val pointerColors = arrayOf(
			Color(50, 153, 203),
			Color(0, 50, 153),
			Color(50, 50, 203),
		)

		val targetingColors = arrayOf(
			Color(180, 154, 109),
			Color(178, 129, 81),
			Color(175, 59, 0),
		)

		val elixirColors = arrayOf(
			Color(155, 90, 0),
			Color(182, 141, 0),
			Color(255, 255, 192)
		)

		val powersColors = arrayOf(
			Color(254, 254, 194),
			Color(11, 195, 243),
		)

		val shallowColors = backgroundColors + barColors + monsterColors + mardekColors +
				deuganColors + turnOrderColors + pointerColors
		val fakeInput = InputManager()
		val soundQueue = SoundQueue()
		state.update(fakeInput, 100.milliseconds, soundQueue)
		assertEquals(BattleMoveSelectionAttack(target = null), battle.selectedMove)
		assertEquals("menu-party-scroll", soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(state, 800, 600, "battle-select-attack0", shallowColors, targetingColors + powersColors)

		// 'Scroll' to skill selection
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveLeft))
		state.update(fakeInput, 100.milliseconds, soundQueue)
		assertEquals(BattleMoveSelectionSkill(skill = null, target = null), battle.selectedMove)
		assertEquals("menu-scroll", soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(state, 800, 600, "battle-select-skill0", shallowColors, targetingColors)

		// 'Scroll' to item selection
		fakeInput.postEvent(repeatKeyEvent(InputKey.MoveLeft))
		state.update(fakeInput, 100.milliseconds, soundQueue)
		assertEquals(BattleMoveSelectionItem(item = null, target = null), battle.selectedMove)
		assertEquals("menu-scroll", soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(state, 800, 600, "battle-select-item0", shallowColors, targetingColors)

		// 'Scroll' to wait
		fakeInput.postEvent(repeatKeyEvent(InputKey.MoveLeft))
		state.update(fakeInput, 100.milliseconds, soundQueue)
		assertTrue(battle.selectedMove is BattleMoveSelectionWait)
		assertEquals("menu-scroll", soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(state, 800, 600, "battle-select-wait", shallowColors, targetingColors)

		// 'Scroll' to flee
		fakeInput.postEvent(repeatKeyEvent(InputKey.MoveLeft))
		state.update(fakeInput, 100.milliseconds, soundQueue)
		assertTrue(battle.selectedMove is BattleMoveSelectionFlee)
		assertEquals("menu-scroll", soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(state, 800, 600, "battle-select-flee", shallowColors, targetingColors)

		// 'Scroll' to attack
		fakeInput.postEvent(repeatKeyEvent(InputKey.MoveLeft))
		state.update(fakeInput, 100.milliseconds, soundQueue)
		assertTrue(battle.selectedMove is BattleMoveSelectionAttack)
		assertEquals("menu-scroll", soundQueue.take())
		assertNull(soundQueue.take())

		// 'Dive' into attack target selection
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveLeft))
		fakeInput.postEvent(pressKeyEvent(InputKey.Interact))
		fakeInput.postEvent(releaseKeyEvent(InputKey.Interact))
		state.update(fakeInput, 10.milliseconds, soundQueue)
		assertEquals(BattleMoveSelectionAttack(
			CombatantReference(isPlayer = false, index = 1, battle)
		), battle.selectedMove)
		assertEquals("click-confirm", soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(
			state, 800, 600, "battle-select-attack1",
			backgroundColors + pointerColors + mardekColors + deuganColors + targetingColors,
			turnOrderColors + monsterColors
		)

		// 'Scrolling' left has no effect since basic attacks are single-target
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveLeft))
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveLeft))
		state.update(fakeInput, 10.milliseconds, soundQueue)
		assertEquals(BattleMoveSelectionAttack(
			CombatantReference(isPlayer = false, index = 1, battle)
		), battle.selectedMove)
		assertNull(soundQueue.take())

		// 'Scrolling' right should cause Deugan to become the target
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveRight))
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveRight))
		state.update(fakeInput, 10.milliseconds, soundQueue)
		assertEquals(BattleMoveSelectionAttack(
			CombatantReference(isPlayer = true, index = 2, battle)
		), battle.selectedMove)
		assertEquals("menu-scroll", soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(
			state, 800, 600, "battle-select-attack2",
			backgroundColors + pointerColors + mardekColors + targetingColors,
			turnOrderColors + monsterColors + deuganColors
		)

		// 'Scrolling' right again has no effect since basic attacks are single-target
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveRight))
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveRight))
		state.update(fakeInput, 10.milliseconds, soundQueue)
		assertEquals(BattleMoveSelectionAttack(
			CombatantReference(isPlayer = true, index = 2, battle)
		), battle.selectedMove)
		assertNull(soundQueue.take())

		// 'Cancel' and open item selection
		fakeInput.postEvent(pressKeyEvent(InputKey.Cancel))
		fakeInput.postEvent(releaseKeyEvent(InputKey.Cancel))
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveRight))
		fakeInput.postEvent(repeatKeyEvent(InputKey.MoveRight))
		fakeInput.postEvent(repeatKeyEvent(InputKey.MoveRight))
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveRight))
		fakeInput.postEvent(pressKeyEvent(InputKey.Interact))
		fakeInput.postEvent(releaseKeyEvent(InputKey.Interact))
		state.update(fakeInput, 200.milliseconds, soundQueue)
		assertEquals(BattleMoveSelectionItem(
			item = elixir, target = null
		), battle.selectedMove)
		assertEquals("click-cancel", soundQueue.take())
		assertEquals("menu-scroll", soundQueue.take())
		assertEquals("menu-scroll", soundQueue.take())
		assertEquals("menu-scroll", soundQueue.take())
		assertEquals("click-confirm", soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(
			state, 800, 600, "battle-select-item1",
			backgroundColors + pointerColors + mardekColors + elixirColors, turnOrderColors
		)

		// Choose elixir and 'dive into' target selection
		fakeInput.postEvent(pressKeyEvent(InputKey.Interact))
		fakeInput.postEvent(releaseKeyEvent(InputKey.Interact))
		state.update(fakeInput, 100.milliseconds, soundQueue)
		assertEquals(BattleMoveSelectionItem(
			item = elixir, target = CombatantReference(isPlayer = true, index = 2, battle)
		), battle.selectedMove)
		assertEquals("click-confirm", soundQueue.take())
		assertNull(soundQueue.take())

		// Scrolling right should have no effect because elixirs are single-target
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveRight))
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveRight))
		state.update(fakeInput, 100.milliseconds, soundQueue)
		assertEquals(BattleMoveSelectionItem(
			item = elixir, target = CombatantReference(isPlayer = true, index = 2, battle)
		), battle.selectedMove)
		assertNull(soundQueue.take())

		testRendering(
			state, 800, 600, "battle-select-item2",
			backgroundColors + pointerColors + mardekColors, turnOrderColors + elixirColors
		)

		// Scrolling up should cause Mardek to become the target
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveUp))
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveUp))
		state.update(fakeInput, 1.milliseconds, soundQueue)
		assertEquals(BattleMoveSelectionItem(
			item = elixir, target = CombatantReference(isPlayer = true, index = 0, battle)
		), battle.selectedMove)
		assertEquals("menu-scroll", soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(
			state, 800, 600, "battle-select-item3",
			backgroundColors + pointerColors, turnOrderColors + mardekColors + elixirColors
		)

		// Scrolling left twice should only work once since elixirs are single-target
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveLeft))
		fakeInput.postEvent(repeatKeyEvent(InputKey.MoveLeft))
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveLeft))
		state.update(fakeInput, 1.milliseconds, soundQueue)
		assertEquals(BattleMoveSelectionItem(
			item = elixir, target = CombatantReference(isPlayer = false, index = 1, battle)
		), battle.selectedMove)
		assertEquals("menu-scroll", soundQueue.take())
		assertNull(soundQueue.take())

		// Cancel item targeting, and go to skill selection
		fakeInput.postEvent(pressKeyEvent(InputKey.Cancel))
		fakeInput.postEvent(repeatKeyEvent(InputKey.Cancel))
		fakeInput.postEvent(releaseKeyEvent(InputKey.Cancel))
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveRight))
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveRight))
		fakeInput.postEvent(pressKeyEvent(InputKey.Interact))
		fakeInput.postEvent(releaseKeyEvent(InputKey.Interact))
		state.update(fakeInput, 20.milliseconds, soundQueue)
		assertEquals(BattleMoveSelectionSkill(
			skill = shock, target = null
		), battle.selectedMove)
		assertEquals("click-cancel", soundQueue.take())
		assertEquals("click-cancel", soundQueue.take())
		assertEquals("menu-scroll", soundQueue.take())
		assertEquals("click-confirm", soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(
			state, 800, 600, "battle-select-skill1",
			backgroundColors + pointerColors + powersColors,
			turnOrderColors + mardekColors + elixirColors
		)

		// Scroll to frostasia
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveDown))
		fakeInput.postEvent(repeatKeyEvent(InputKey.MoveDown))
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveDown))
		state.update(fakeInput, 100.milliseconds, soundQueue)
		assertEquals(BattleMoveSelectionSkill(
			skill = frostasia, target = null
		), battle.selectedMove)
		assertEquals("menu-scroll", soundQueue.take())
		assertEquals("menu-scroll", soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(
			state, 800, 600, "battle-select-skill2",
			backgroundColors + pointerColors + powersColors,
			turnOrderColors + mardekColors + elixirColors
		)

		// Let 'blue targeting blink' wear off
		sleep(1000)

		// Choose frostasia and dive into target selection
		fakeInput.postEvent(pressKeyEvent(InputKey.Interact))
		fakeInput.postEvent(releaseKeyEvent(InputKey.Interact))
		state.update(fakeInput, 10.milliseconds, soundQueue)
		assertEquals(BattleMoveSelectionSkill(
			skill = frostasia, target = BattleSkillTargetSingle(CombatantReference(isPlayer = false, index = 1, battle))
		), battle.selectedMove)
		assertEquals("click-confirm", soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(
			state, 800, 600, "battle-select-skill3",
			backgroundColors + pointerColors + mardekColors + deuganColors + arrayOf(powersColors[1]),
			turnOrderColors + elixirColors + monsterColors + arrayOf(powersColors[0])
		)

		// Scrolling left has no effect since there is only 1 enemy
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveLeft))
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveLeft))
		state.update(fakeInput, 100.milliseconds, soundQueue)
		assertEquals(BattleMoveSelectionSkill(
			skill = frostasia, target = BattleSkillTargetSingle(CombatantReference(isPlayer = false, index = 1, battle))
		), battle.selectedMove)
		assertNull(soundQueue.take())

		// Scroll right once to target Deugan
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveRight))
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveRight))
		state.update(fakeInput, 100.milliseconds, soundQueue)
		assertEquals(BattleMoveSelectionSkill(
			skill = frostasia, target = BattleSkillTargetSingle(CombatantReference(isPlayer = true, index = 2, battle))
		), battle.selectedMove)
		assertEquals("menu-scroll", soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(
			state, 800, 600, "battle-select-skill4",
			backgroundColors + pointerColors + mardekColors + arrayOf(powersColors[1]),
			turnOrderColors + elixirColors + deuganColors + arrayOf(powersColors[0])
		)

		// Scroll right again to target both Mardek and Deugan
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveRight))
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveRight))
		state.update(fakeInput, 10.milliseconds, soundQueue)
		assertEquals(BattleMoveSelectionSkill(
			skill = frostasia, target = BattleSkillTargetAllAllies
		), battle.selectedMove)
		assertEquals("menu-scroll", soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(
			state, 800, 600, "battle-select-skill5",
			backgroundColors + pointerColors + arrayOf(powersColors[1]),
			turnOrderColors + elixirColors + mardekColors + deuganColors + arrayOf(powersColors[0])
		)

		// Targeting multiple allies costs too much mana
		fakeInput.postEvent(pressKeyEvent(InputKey.Interact))
		fakeInput.postEvent(releaseKeyEvent(InputKey.Interact))
		state.update(fakeInput, 10.milliseconds, soundQueue)
		assertEquals(BattleMoveSelectionSkill(
			skill = frostasia, target = BattleSkillTargetAllAllies
		), battle.selectedMove)
		assertEquals(BattleMoveThinking, battle.currentMove)
		assertEquals("click-reject", soundQueue.take())
		assertNull(soundQueue.take())

		// But casting on just Deugan should work...
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveLeft))
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveLeft))
		fakeInput.postEvent(pressKeyEvent(InputKey.Interact))
		fakeInput.postEvent(releaseKeyEvent(InputKey.Interact))
		state.update(fakeInput, 10.milliseconds, soundQueue)
		assertEquals(BattleMoveSelectionAttack(null), battle.selectedMove)
		assertEquals(BattleMoveSkill(frostasia, BattleSkillTargetSingle(CombatantReference(
			isPlayer = true, index = 2, battle
		))), battle.currentMove)
		assertEquals("menu-scroll", soundQueue.take())
		assertEquals("click-confirm", soundQueue.take())
		assertNull(soundQueue.take())
	}
}
