package mardek.game

import com.github.knokko.bitser.Bitser
import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.builders.BoilerBuilder
import com.github.knokko.boiler.builders.instance.ValidationFeatures
import com.github.knokko.boiler.commands.SingleTimeCommands
import com.github.knokko.boiler.descriptors.DescriptorCombiner
import com.github.knokko.boiler.memory.MemoryBlock
import com.github.knokko.boiler.memory.MemoryCombiner
import com.github.knokko.vk2d.Vk2dConfig
import com.github.knokko.vk2d.Vk2dInstance
import com.github.knokko.vk2d.pipeline.Vk2dPipelineContext
import com.github.knokko.vk2d.pipeline.Vk2dPipelines
import com.github.knokko.vk2d.resource.Vk2dResourceBundle
import com.github.knokko.vk2d.resource.Vk2dResourceLoader
import mardek.content.Content
import mardek.content.area.Area
import mardek.content.characters.PlayableCharacter
import mardek.content.inventory.Item
import mardek.content.inventory.ItemStack
import mardek.content.skill.ActiveSkill
import mardek.renderer.RenderManager
import mardek.state.SoundQueue
import mardek.state.VideoSettings
import mardek.state.ingame.CampaignState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.content.battle.Battle
import mardek.state.ingame.battle.BattleState
import mardek.state.ingame.battle.BattleUpdateContext
import mardek.content.battle.Enemy
import mardek.content.characters.CharacterState
import mardek.content.story.TimelineNode
import mardek.state.GameStateUpdateContext
import mardek.state.ingame.area.AreaSuspensionBattle
import mardek.state.saves.SaveFile
import mardek.state.saves.SavesFolderManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.lwjgl.vulkan.VK10.VK_FORMAT_R8G8B8A8_SRGB
import org.lwjgl.vulkan.VK10.vkDestroyDescriptorPool
import org.lwjgl.vulkan.VK10.vkDestroyRenderPass
import org.lwjgl.vulkan.VK13.VK_API_VERSION_1_3
import java.io.File
import java.nio.file.Files
import java.util.Collections

class TestingInstance {

	val bitser = Bitser(true)
	val content = Content.load("mardek/game/content.bits", bitser)

	val boiler: BoilerInstance
	val pipelineContext: Vk2dPipelineContext
	val vk2d: Vk2dInstance
	val renderManager: RenderManager
	val titleScreenResources: Vk2dResourceBundle
	val titleScreenMemory: MemoryBlock
	val titleScreenDescriptorPool: Long

	val dragonLairEntry: Area
	val dragonLair2: Area
	val heroMardek: PlayableCharacter
	val heroDeugan: PlayableCharacter
	val childMardek: PlayableCharacter
	val childDeugan: PlayableCharacter
	val shock: ActiveSkill
	val frostasia: ActiveSkill
	val elixir: Item

	val dummySavesDirectories: MutableList<File> = Collections.synchronizedList(ArrayList<File>())

	init {
		val builder = BoilerBuilder(
			VK_API_VERSION_1_3, "IntegrationTests", 1
		)
		builder.doNotUseVma()
		builder.requiredFeatures10("textureCompressionBC") {
			supportedFeatures -> supportedFeatures.textureCompressionBC()
		}
		builder.featurePicker10 { _, _, enabledFeatures ->
			enabledFeatures.textureCompressionBC(true)
		}
		builder.defaultTimeout(5_000_000_000L)
		boiler = builder.validation(ValidationFeatures(
			true, false, false, true
		)).forbidValidationErrors().build()

		val config = Vk2dConfig()
		RenderManager.initPipelinesConfig(config)

		vk2d = Vk2dInstance(boiler, config)
		pipelineContext = Vk2dPipelineContext.renderPass(boiler, VK_FORMAT_R8G8B8A8_SRGB)
		val basePipelines = Vk2dPipelines(vk2d, pipelineContext, config)

		val titleScreenAllocator = MemoryCombiner(boiler, "TitleScreenMemory")
		val titleScreenDescriptors = DescriptorCombiner(boiler)
		val titleScreenLoader = Vk2dResourceLoader(
			vk2d, MardekWindow::class.java.getResourceAsStream("title-screen.vk2d")!!
		)
		titleScreenLoader.claimMemory(titleScreenAllocator)
		titleScreenMemory = titleScreenAllocator.build(false)

		titleScreenLoader.prepareStaging()
		SingleTimeCommands.submit(boiler, "TitleScreenStaging") { recorder ->
			titleScreenLoader.performStaging(recorder, titleScreenDescriptors)
		}.destroy()
		titleScreenDescriptorPool = titleScreenDescriptors.build("TitleScreenDescriptors")
		titleScreenResources = titleScreenLoader.finish()

		renderManager = RenderManager(
			boiler, VideoSettings(0, capFps = false, showFps = false, framesInFlight = 1, delayRendering = true),
			titleScreenResources, pipelineContext, basePipelines,
		)
		renderManager.loadMainResources(
			TestingInstance::class.java.getResourceAsStream("content.vk2d")!!
		)
		renderManager.content = content

		dragonLairEntry = content.areas.areas.find { it.properties.rawName == "DL_entr" }!!
		dragonLair2 = content.areas.areas.find { it.properties.rawName == "DL_area2" }!!
		heroMardek = content.playableCharacters.find { it.characterClass.rawName == "mardek_hero" }!!
		heroDeugan = content.playableCharacters.find { it.characterClass.rawName == "deugan_hero" }!!
		childMardek = content.playableCharacters.find { it.characterClass.rawName == "mardek_child" }!!
		childDeugan = content.playableCharacters.find { it.characterClass.rawName == "deugan_child" }!!

		elixir = content.items.items.find { it.flashName == "Elixir" }!!
		shock = heroDeugan.characterClass.skillClass.actions.find { it.name == "Shock" }!!
		frostasia = heroDeugan.characterClass.skillClass.actions.find { it.name == "Frostasia" }!!

		if (!actualResultsDirectory.exists() && !actualResultsDirectory.mkdir()) {
			throw RuntimeException("Failed to create $actualResultsDirectory")
		}
	}

	fun simpleCharacterStates() = run {
		val mardekState = CharacterState()
		mardekState.equipment[0] = content.items.items.find { it.flashName == "M Blade" }!!
		mardekState.currentHealth = mardekState.determineMaxHealth(heroMardek.baseStats, emptySet())
		val deuganState = CharacterState()
		deuganState.equipment[0] = content.items.items.find { it.flashName == "Balmung" }!!
		deuganState.inventory[0] = ItemStack(elixir, 1)
		deuganState.skillMastery[shock] = shock.masteryPoints
		deuganState.skillMastery[frostasia] = frostasia.masteryPoints
		deuganState.currentHealth = deuganState.determineMaxHealth(heroDeugan.baseStats, emptySet())
		hashMapOf(Pair(heroMardek, mardekState), Pair(heroDeugan, deuganState))
	}

	fun battleUpdateContext(campaign: CampaignState) = BattleUpdateContext(
		campaign.characterStates, content.audio.fixedEffects,
		content.stats.elements.find { it.rawName == "NONE" }!!, SoundQueue()
	)

	fun startSimpleBattle(campaign: CampaignState, enemies: Array<Enemy?> = arrayOf(
		null, Enemy(monster = content.battle.monsters.find { it.name == "monster" }!!, level = 10), null, null
	), canFlee: Boolean = true) {
		campaign.currentArea!!.suspension = AreaSuspensionBattle(BattleState(
			battle = Battle(
				startingEnemies = enemies,
				enemyLayout = content.battle.enemyPartyLayouts.find { it.name == "TRIO" }!!,
				music = "battle",
				lootMusic = "VictoryFanfare",
				background = content.battle.backgrounds.find { it.name == "volcano" }!!,
				canFlee = canFlee,
				isRandom = true,
			),
			players = campaign.party,
			playerLayout = content.battle.enemyPartyLayouts.find { it.name == "DEFAULT" }!!,
			context = battleUpdateContext(campaign)
		))
	}

	fun simpleCampaignState(): CampaignState {
		val campaignState = CampaignState()
		campaignState.story.initialize(content)
		campaignState.story.validatePartyMembers(content, campaignState.party, campaignState.characterStates)
		campaignState.currentArea = AreaState(
			dragonLair2, AreaPosition(10, 10), skipFadeIn = true
		)
		campaignState.characterStates.putAll(simpleCharacterStates())
		campaignState.gold = 123
		return campaignState
	}

	fun dummySaveManager(): SavesFolderManager {
		val savesFolder = Files.createTempDirectory("").toFile()
		savesFolder.deleteOnExit()

		val manager = SavesFolderManager(savesFolder)
		dummySavesDirectories.add(savesFolder)
		return manager
	}

	fun createDummySave(saves: SavesFolderManager, campaignName: String): File {
		val campaignState = simpleCampaignState()
		campaignState.characterStates[heroMardek]!!.currentLevel = 1
		campaignState.characterStates[heroDeugan]!!.currentLevel = 1

		assertTrue(saves.createSave(content, campaignState, campaignName, SaveFile.Type.Crystal))

		val campaignFolder = File("${saves.root}/$campaignName")
		val saveFiles = campaignFolder.listFiles()!!
		assertEquals(1, saveFiles.size)
		campaignFolder.deleteOnExit()
		saveFiles[0].deleteOnExit()
		return saveFiles[0]
	}

	fun performTimelineTransition(
		updateContext: GameStateUpdateContext, campaign: CampaignState,
		timelineName: String, nodeName: String
	) {
		val timeline = content.story.timelines.find { it.name == timelineName }!!

		fun findNode(candidate: TimelineNode): TimelineNode? {
			if (candidate.name == nodeName) return candidate
			for (child in candidate.children) {
				val descendant = findNode(child)
				if (descendant != null) return descendant
			}
			return null
		}

		val campaignContext = CampaignState.UpdateContext(updateContext, "")
		campaign.performTimelineTransition(campaignContext, timeline, findNode(timeline.root)!!)
	}

	fun destroy() {
		for (directory in dummySavesDirectories) directory.deleteRecursively()
		renderManager.pipelines.base.destroy()
		renderManager.cleanUp()
		titleScreenMemory.destroy(boiler)
		vkDestroyDescriptorPool(boiler.vkDevice(), titleScreenDescriptorPool, null)
		vk2d.destroy()
		vkDestroyRenderPass(boiler.vkDevice(), pipelineContext.vkRenderPass, null)
		boiler.destroyInitialObjects()
	}
}
