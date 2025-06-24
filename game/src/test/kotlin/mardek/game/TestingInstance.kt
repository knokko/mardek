package mardek.game

import com.github.knokko.bitser.serialize.Bitser
import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.builders.BoilerBuilder
import mardek.content.Content
import mardek.content.area.Area
import mardek.content.characters.PlayableCharacter
import mardek.content.inventory.Item
import mardek.content.inventory.ItemStack
import mardek.content.skill.ActiveSkill
import mardek.renderer.GameRenderer
import mardek.state.SoundQueue
import mardek.state.ingame.CampaignState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.battle.Battle
import mardek.state.ingame.battle.BattleState
import mardek.state.ingame.battle.BattleUpdateContext
import mardek.state.ingame.battle.Enemy
import mardek.state.ingame.characters.CharacterSelectionState
import mardek.state.ingame.characters.CharacterState
import org.lwjgl.vulkan.VK10.VK_API_VERSION_1_0
import java.util.concurrent.CompletableFuture

class TestingInstance {

	val content = Content.load("mardek/game/content.bin", Bitser(true))
	val boiler: BoilerInstance
	val getBoiler: CompletableFuture<BoilerInstance>

	val dragonLairEntry: Area
	val dragonLair2: Area
	val heroMardek: PlayableCharacter
	val heroDeugan: PlayableCharacter
	val shock: ActiveSkill
	val frostasia: ActiveSkill
	val elixir: Item

	init {
		val builder = BoilerBuilder(VK_API_VERSION_1_0, "IntegrationTests", 1)
		GameRenderer.addBoilerRequirements(builder)
		builder.defaultTimeout(5_000_000_000L)
		boiler = builder.validation().forbidValidationErrors().build()

		getBoiler = CompletableFuture<BoilerInstance>()
		getBoiler.complete(boiler)

		dragonLairEntry = content.areas.areas.find { it.properties.rawName == "DL_entr" }!!
		dragonLair2 = content.areas.areas.find { it.properties.rawName == "DL_area2" }!!
		heroMardek = content.playableCharacters.find { it.characterClass.rawName == "mardek_hero" }!!
		heroDeugan = content.playableCharacters.find { it.characterClass.rawName == "deugan_hero" }!!

		elixir = content.items.items.find { it.flashName == "Elixir" }!!
		shock = heroDeugan.characterClass.skillClass.actions.find { it.name == "Shock" }!!
		frostasia = heroDeugan.characterClass.skillClass.actions.find { it.name == "Frostasia" }!!
	}

	fun simpleCharacterSelectionState() = CharacterSelectionState(
		available = hashSetOf(heroMardek, heroDeugan),
		unavailable = HashSet(),
		party = arrayOf(heroMardek, heroDeugan, null, null)
	)

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
		null, Enemy(monster = content.battle.monsters.find { it.name == "monster" }!!, level = 10), null, null)
	) {
		campaign.currentArea!!.activeBattle = BattleState(
			battle = Battle(
				startingEnemies = enemies,
				enemyLayout = content.battle.enemyPartyLayouts.find { it.name == "TRIO" }!!,
				music = "peak",
				background = content.battle.backgrounds.find { it.name == "volcano" }!!
			),
			players = arrayOf(heroMardek, null, heroDeugan, null),
			playerLayout = content.battle.enemyPartyLayouts.find { it.name == "DEFAULT" }!!,
			context = battleUpdateContext(campaign)
		)
	}

	fun simpleCampaignState() = CampaignState(
		currentArea = AreaState(dragonLair2, AreaPosition(10, 10)),
		characterSelection = simpleCharacterSelectionState(),
		characterStates = simpleCharacterStates(),
		gold = 123
	)

	fun destroy() {
		boiler.destroyInitialObjects()
	}
}
