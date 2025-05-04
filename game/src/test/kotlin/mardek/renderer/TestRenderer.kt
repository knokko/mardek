package mardek.renderer

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.builders.BoilerBuilder
import com.github.knokko.boiler.commands.SingleTimeCommands
import com.github.knokko.boiler.images.ImageBuilder
import com.github.knokko.boiler.synchronization.ResourceUsage
import mardek.content.Content
import mardek.state.ingame.CampaignState
import mardek.state.ingame.InGameState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.battle.Battle
import mardek.state.ingame.battle.BattleState
import mardek.state.ingame.battle.Enemy
import mardek.state.ingame.characters.CharacterSelectionState
import mardek.state.ingame.characters.CharacterState
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.lwjgl.vulkan.VK10.*
import java.awt.Color
import java.util.concurrent.CompletableFuture
import kotlin.math.abs

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestRenderer {

	private lateinit var boiler: BoilerInstance
	private lateinit var content: Content
	private lateinit var resources: SharedResources

	private val getResources = CompletableFuture<SharedResources>()

	@BeforeAll
	fun importResources() {
		content = Content.load("mardek/game/content.bin")

		val builder = BoilerBuilder(VK_API_VERSION_1_0, "TestBattleRenderer", 1)
		GameRenderer.addBoilerRequirements(builder)
		boiler = builder.validation().forbidValidationErrors().build()

		val getBoiler = CompletableFuture<BoilerInstance>()
		getBoiler.complete(boiler)
		resources = SharedResources(getBoiler, 1, skipWindow = true)
		getResources.complete(resources)
	}

	@AfterAll
	fun cleanUp() {
		resources.destroy()
		boiler.destroyInitialObjects()
	}

	private fun nearlyEquals(expectedComponent: Int, actualComponent: Int) = abs(expectedComponent - actualComponent) <= 1

	private fun nearlyEquals(expected: Color, actual: Color) = nearlyEquals(expected.red, actual.red) &&
			nearlyEquals(expected.green, actual.green) && nearlyEquals(expected.blue, actual.blue) &&
			nearlyEquals(expected.alpha, actual.alpha)

	@Test
	fun testRenderDragonLair() {
		val area = content.areas.areas.find { it.properties.rawName == "DL_area2" }!!
		val mardek = content.playableCharacters.find { it.characterClass.rawName == "mardek_hero" }!!
		val deugan = content.playableCharacters.find { it.characterClass.rawName == "deugan_hero" }!!
		val state = InGameState(content, CampaignState(
			currentArea = AreaState(area, AreaPosition(10, 10)),
			characterSelection = CharacterSelectionState(
				available = hashSetOf(mardek, deugan),
				unavailable = HashSet(),
				party = arrayOf(mardek, deugan, null, null)
			),
			characterStates = hashMapOf(
				Pair(mardek, CharacterState()),
				Pair(deugan, CharacterState())
			),
			gold = 123
		))

		val renderer = GameRenderer(boiler, getResources)

		val targetImage = ImageBuilder("TargetImage", 1000, 800)
			.format(VK_FORMAT_R8G8B8A8_SRGB)
			.setUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT or VK_IMAGE_USAGE_TRANSFER_SRC_BIT)
			.build(boiler)
		val framebuffer = boiler.images.createFramebuffer(
			resources.renderPass, targetImage.width, targetImage.height, "Framebuffer", targetImage.vkImageView
		)
		val destinationBuffer = boiler.buffers.createMapped(
			4L * targetImage.width * targetImage.height, VK_BUFFER_USAGE_TRANSFER_DST_BIT, "DestinationBuffer"
		)

		val commands = SingleTimeCommands(boiler)
		commands.submit("TestDragonLair") { recorder ->
			recorder.transitionLayout(targetImage, null, ResourceUsage.COLOR_ATTACHMENT_WRITE)
			renderer.render(state, recorder, targetImage, framebuffer, 0)
			recorder.transitionLayout(targetImage, ResourceUsage.COLOR_ATTACHMENT_WRITE, ResourceUsage.TRANSFER_SOURCE)
			recorder.copyImageToBuffer(targetImage, destinationBuffer.fullRange())
		}
		commands.destroy()

		val result = boiler.buffers.decodeBufferedImageRGBA(destinationBuffer, 0L, targetImage.width, targetImage.height)
		destinationBuffer.destroy(boiler)
		vkDestroyFramebuffer(boiler.vkDevice(), framebuffer, null)
		targetImage.destroy(boiler)

		val expectedColors = listOf(
			Color(59, 53, 66), // color between floor tiles
			Color(91, 79, 106), // light color of floor tiles
			Color(0, 0, 0), // black for the background
			Color(86, 50, 86), // color of braziers
			Color(225, 0, 38), // color used in paintings
			Color(101, 50, 0), // hair color of Mardek
			Color(69, 117, 28), // cape color of Deugan
		)

		for (color in expectedColors) {
			assertTrue((0 until result.width).any { x -> (0 until result.height).any { y ->
				nearlyEquals(color, Color(result.getRGB(x, y), true))
			} })
		}
	}

	@Test
	fun testRenderBattle() {
		val area = content.areas.areas.find { it.properties.rawName == "DL_area2" }!!
		val mardek = content.playableCharacters.find { it.characterClass.rawName == "mardek_hero" }!!
		val deugan = content.playableCharacters.find { it.characterClass.rawName == "deugan_hero" }!!
		val state = InGameState(content, CampaignState(
			currentArea = AreaState(area, AreaPosition(10, 10)),
			characterSelection = CharacterSelectionState(
				available = hashSetOf(mardek, deugan),
				unavailable = HashSet(),
				party = arrayOf(mardek, deugan, null, null)
			),
			characterStates = hashMapOf(
				Pair(mardek, CharacterState()),
				Pair(deugan, CharacterState())
			),
			gold = 123
		))
		state.campaign.currentArea!!.activeBattle = BattleState(
			battle = Battle(
				enemies = arrayOf(null, Enemy(monster = content.battle.monsters.find {
					it.name == "Killer Cod"
				}!!, level = 10), null, null),
				enemyPositions = content.battle.enemyPartyLayouts.find { it.name == "TRIO" }!!,
				music = "peak",
				background = content.battle.backgrounds.find { it.name == "volcano" }!!
			),
			players = arrayOf(mardek, null, deugan, null),
			campaignState = state.campaign
		)

		val renderer = GameRenderer(boiler, getResources)

		val targetImage = ImageBuilder("TargetImage", 1000, 800)
			.format(VK_FORMAT_R8G8B8A8_SRGB)
			.setUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT or VK_IMAGE_USAGE_TRANSFER_SRC_BIT)
			.build(boiler)
		val framebuffer = boiler.images.createFramebuffer(
			resources.renderPass, targetImage.width, targetImage.height, "Framebuffer", targetImage.vkImageView
		)
		val destinationBuffer = boiler.buffers.createMapped(
			4L * targetImage.width * targetImage.height, VK_BUFFER_USAGE_TRANSFER_DST_BIT, "DestinationBuffer"
		)

		val commands = SingleTimeCommands(boiler)
		commands.submit("TestRenderBattle") { recorder ->
			recorder.transitionLayout(targetImage, null, ResourceUsage.COLOR_ATTACHMENT_WRITE)
			renderer.render(state, recorder, targetImage, framebuffer, 0)
			recorder.transitionLayout(targetImage, ResourceUsage.COLOR_ATTACHMENT_WRITE, ResourceUsage.TRANSFER_SOURCE)
			recorder.copyImageToBuffer(targetImage, destinationBuffer.fullRange())
		}
		commands.destroy()

		val result = boiler.buffers.decodeBufferedImageRGBA(destinationBuffer, 0L, targetImage.width, targetImage.height)
		destinationBuffer.destroy(boiler)
		vkDestroyFramebuffer(boiler.vkDevice(), framebuffer, null)
		targetImage.destroy(boiler)

		val expectedColors = listOf(
			Color(0, 102, 204), // color of Killer Cod fin
			Color(255, 65, 41), // one of the lava colors
			Color(0, 0, 16), // dark lava color
			Color(208, 193, 142), // turn order text/line color
			Color(88, 64, 28), // one of the turn order monster icon colors
			Color(101, 50, 0), // hair color of turn order icon of Mardek
			Color(69, 117, 28), // cape color of turn order icon of Deugan
			Color(195, 157, 79), // hair color of battle model of Deugan
		)

		for (color in expectedColors) {
			assertTrue((0 until result.width).any { x -> (0 until result.height).any { y ->
				nearlyEquals(color, Color(result.getRGB(x, y), true))
			} })
		}
	}
}
