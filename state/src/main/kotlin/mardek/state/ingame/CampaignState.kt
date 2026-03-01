package mardek.state.ingame

import com.github.knokko.bitser.BitPostInit
import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.Bitser
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.io.BitInputStream
import com.github.knokko.bitser.options.WithParameter
import mardek.content.BITSER
import mardek.content.Content
import mardek.content.action.ActionPlayCutscene
import mardek.content.action.FixedActionNode
import mardek.content.area.Chest
import mardek.content.characters.PlayableCharacter
import mardek.input.InputKey
import mardek.input.InputKeyEvent
import mardek.state.GameStateUpdateContext
import mardek.state.ingame.actions.ActivatedTriggers
import mardek.state.ingame.actions.CampaignActionsState
import mardek.state.ingame.area.AreaDiscoveryMap
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.battle.BattleStateMachine
import mardek.content.characters.CharacterState
import mardek.content.inventory.ItemStack
import mardek.content.story.Timeline
import mardek.content.story.TimelineNode
import mardek.state.UsedPartyMember
import mardek.state.ingame.area.AreaSuspensionActions
import mardek.state.ingame.area.AreaSuspensionBattle
import mardek.state.ingame.area.ShopsStates
import mardek.state.ingame.story.StoryState
import mardek.state.ingame.worldmap.WorldMapState
import mardek.state.saves.SaveFile
import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlin.time.Duration.Companion.seconds

@BitStruct(backwardCompatible = true)
class CampaignState : BitPostInit {

	/**
	 * The (primary) state (machine) of the campaign state. The campaign is always in one of the following states:
	 * - Inside an area
	 * - Outside an area, in a global action sequence
	 * - Outside an area, on the world map
	 */
	@BitField(id = 0)
	@ClassField(root = CampaignStateMachine::class)
	var state: CampaignStateMachine = CampaignActionsState(FixedActionNode())

	/**
	 * The characters currently in the party
	 */
	@BitField(id = 1)
	@NestedFieldSetting(path = "c", optional = true)
	@ReferenceField(stable = true, label = "playable characters")
	val party: Array<PlayableCharacter?> = arrayOf(null, null, null, null)

	@BitField(id = 2)
	@NestedFieldSetting(path = "k", fieldName = "CHARACTER_STATES_KEY")
	val characterStates = HashMap<PlayableCharacter, CharacterState>()

	@BitField(id = 3)
	@IntegerField(expectUniform = false, minValue = 0)
	var gold: Int = 0

	@BitField(id = 4)
	@ReferenceField(stable = true, label = "chests")
	val openedChests = HashSet<Chest>()

	/**
	 * The state of the timelines, from which many other states are derived. It determines among others which
	 * playable characters are available, and influences a lot of dialogue.
	 */
	@BitField(id = 5)
	val story = StoryState()

	@BitField(id = 6)
	val areaDiscovery = AreaDiscoveryMap()

	@BitField(id = 7)
	val triggers = ActivatedTriggers()

	/**
	 * - This variable is 0 at the start of the campaign, and is reset to 0 whenever a random battle is encountered.
	 * - This variable is increased by 1 whenever the player moves in an area with random battles
	 * - When this variable gets larger, the probability of encountering a random battle increases.
	 * - When this variable is too low, no random battle can be encountered.
	 */
	@BitField(id = 8)
	@IntegerField(expectUniform = false, minValue = 0)
	var stepsSinceLastBattle = 0

	@BitField(id = 9)
	@IntegerField(expectUniform = false, minValue = 0)
	var totalSteps = 0L

	@BitField(id = 10)
	@IntegerField(expectUniform = true, minValue = 0)
	var totalTime = 0.seconds

	/**
	 * The items that are currently in the item storage, which the player can access via a save crystal. This list
	 * starts empty, but will grow larger if needed.
	 */
	@BitField(id = 11)
	@NestedFieldSetting(path = "c", optional = true)
	val itemStorage = ArrayList<ItemStack?>()

	/**
	 * The item stack that is currently grabbed by the cursor (e.g. in the inventory tab).
	 */
	@BitField(id = 12, optional = true)
	var cursorItemStack: ItemStack? = null

	/**
	 * The states (inventories) of all the shops and traders.
	 */
	@BitField(id = 13)
	val shops = ShopsStates()

	var shouldOpenMenu = false
	var gameOver = false

	override fun postInit(context: BitPostInit.Context) {
		val content = context.withParameters["content"] as Content
		story.validatePartyMembers(content, party, characterStates)
		for ((character, state) in characterStates) {
			state.initialize(character, itemStorage)
		}
	}

	fun update(context: UpdateContext) {
		while (true) {
			val event = context.input.consumeEvent() ?: break
			if (event is InputKeyEvent && event.didPress && event.key == InputKey.CheatSave) {
				context.saves.createSave(context.content, this, context.campaignName, SaveFile.Type.Cheat)
			}

			state.processEvent(event, context, this)
		}

		while (true) {
			val oldState = this.state
			oldState.update(context, this)
			if (this.state === oldState) break
		}
	}

	/**
	 * Gets a list of party members that are currently used. Unlike `allPartyMembers`, this list does *not* include
	 * null members/empty party slots.
	 */
	fun usedPartyMembers() = party.withIndex().filter {
		it.value != null
	}.map { UsedPartyMember(it.index, it.value!!, characterStates[it.value!!]!!) }

	/**
	 * Gets an array of length 4, where the element at index `i` is `(playableCharacter, characterState)`, or `null`
	 * if the party member slot at index `i` is empty.
	 */
	fun allPartyMembers() = party.map {
		if (it == null) null else Pair(it, characterStates[it]!!)
	}.toTypedArray()

	fun clampHealthAndMana() {
		for ((playableCharacter, state) in characterStates) {
			val maxHealth = state.determineMaxHealth(playableCharacter.baseStats, state.activeStatusEffects)
			state.currentHealth = state.currentHealth.coerceIn(1, maxHealth)
			val maxMana = state.determineMaxMana(playableCharacter.baseStats, state.activeStatusEffects)
			state.currentMana = state.currentMana.coerceIn(0, maxMana)
		}
	}

	/**
	 * Restores all HP and MP of all party members, and removes all their status effects.
	 */
	fun healParty() {
		for ((_, player, playerState) in usedPartyMembers()) {
			playerState.activeStatusEffects.clear()
			playerState.currentHealth = playerState.determineMaxHealth(player.baseStats, emptySet())
			playerState.currentMana = playerState.determineMaxMana(player.baseStats, emptySet())
		}
	}

	/**
	 * Transitions the current state/node of `timeline` to `newNode`, if `newNode` occurs later than the current
	 * state/node of `timeline`. If not, nothing happens.
	 */
	fun performTimelineTransition(context: UpdateContext, timeline: Timeline, newNode: TimelineNode) {
		story.transition(context.content, party, characterStates, timeline, newNode)
	}

	/**
	 * This method should be called when the player enters the campaign state/session, so either:
	 * - when the player starts a new game, or
	 * - when the player loads a saved game
	 *
	 * This method will reset some animation states.
	 */
	fun markSessionStart() {
		when (val currentState = this.state) {
			is CampaignActionsState -> currentState.markSessionStart()
			is AreaState -> {
				when (val suspension = currentState.suspension) {
					is AreaSuspensionBattle -> suspension.battle.markSessionStart()
					else -> {}
				}
			}
		}
	}

	/**
	 * Determines the name of the music track that the `AudioUpdater` should play in the current state. This is
	 * often the background music of the current area, but not always.
	 */
	fun determineMusicTrack(content: Content): String? {
		return when (val state = this.state) {
			is AreaState -> {
				val suspension = state.suspension
				if (suspension is AreaSuspensionBattle) {
					val battleState = suspension.battle
					if (
						!battleState.battle.isRandom ||
						story.evaluate(content.story.fixedVariables.blockRandomBattleMusic) == null
					) {
						return if (battleState.state is BattleStateMachine.Victory) battleState.battle.lootMusic
						else battleState.battle.music
					}
				}

				if (suspension is AreaSuspensionActions) {
					val overrideMusic = suspension.actions.overrideMusic
					if (overrideMusic != null) return overrideMusic
				}

				story.evaluate(state.area.properties.musicTrack, expressionContext())
			}

			is CampaignActionsState -> {
				when (val node = state.node) {
					is FixedActionNode -> {
						val action = node.action
						if (action is ActionPlayCutscene) action.cutscene.payload.get().musicTrack else null
					}
					else -> null
				}
			}

			is WorldMapState -> {
				story.evaluate(state.map.music, expressionContext())
			}

			else -> {
				throw UnsupportedOperationException("Unexpected campaign state machine $state")
			}
		}
	}

	/**
	 * Generates a [StoryState.ExpressionContext] that is needed for [StoryState.evaluate]
	 */
	fun expressionContext() = StoryState.ExpressionContext(
		countItemInInventory = { itemToCount -> usedPartyMembers().sumOf { it.state.countItemOccurrences(itemToCount) }}
	)

	open class UpdateContext(
		parent: GameStateUpdateContext,
		val campaignName: String
	) : GameStateUpdateContext(parent)

	companion object {

		@Suppress("unused")
		@ReferenceField(stable = true, label = "playable characters")
		private val CHARACTER_STATES_KEY = false

		/**
		 * Loads the campaign state from a save file that is being read through `input`.
		 */
		fun loadSave(content: Content, input: InputStream): CampaignState {
			val bitInput = BitInputStream(input)
			val campaignState = BITSER.deserialize(
				CampaignState::class.java, bitInput,
				content,
				Bitser.BACKWARD_COMPATIBLE,
				WithParameter("content", content),
			)
			bitInput.close()

			return campaignState
		}

		/**
		 * Loads the [Content.checkpoints] with the given `name`
		 */
		fun loadCheckpoint(content: Content, name: String): CampaignState {
			val rawCheckpoint = content.checkpoints[name] ?: throw IllegalArgumentException(
				"Missing checkpoint $name: options are ${content.checkpoints}"
			)
			return loadSave(content, ByteArrayInputStream(rawCheckpoint))
		}

		/**
		 * Loads the initial state of the given `chapter`, e.g. `loadChapter(content, 2)` would return the
		 * campaign state that represents the start of chapter 2.
		 */
		fun loadChapter(content: Content, chapter: Int) = loadCheckpoint(content, "chapter$chapter")
	}
}
