package mardek.state.ingame.area.loot

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.BitPostInit
import com.github.knokko.bitser.field.NestedFieldSetting
import mardek.content.characters.PlayableCharacter
import mardek.content.inventory.Dreamstone
import mardek.content.inventory.ItemStack
import mardek.content.inventory.PlotItem
import mardek.input.InputKey
import mardek.state.GameStateUpdateContext
import mardek.content.characters.CharacterState
import mardek.content.skill.Skill
import mardek.state.ingame.UsedPartyMember
import mardek.state.ingame.WholeParty

/**
 * The loot that a player got after winning a battle (e.g. the amount of gold and looted items).
 * This class also tracks the 'interaction state' of the battle loot UI (e.g. which items were already taken and
 * which button is highlighted).
 */
@BitStruct(backwardCompatible = true)
class BattleLoot(

	/**
	 * The amount of gold that the player got by winning the battle
	 */
	@BitField(id = 0)
	@IntegerField(expectUniform = false, minValue = 0)
	val gold: Int,

	/**
	 * The items that the player can claim in the loot screen
	 */
	@BitField(id = 1)
	val items: ArrayList<ItemStack>,

	/**
	 * The plot items that the player acquired by winning the battle
	 */
	@BitField(id = 2)
	@ReferenceField(stable = true, label = "plot items")
	val plotItems: ArrayList<PlotItem>,

	/**
	 * The dreamstones that the player acquired by winning the battle
	 */
	@BitField(id = 3)
	@ReferenceField(stable = true, label = "dreamstones")
	val dreamStones: ArrayList<Dreamstone>,

	/**
	 * The text that will be shown at the top-left of the screen, e.g. "You have acquired:" or "No spoils here!"
	 */
	@BitField(id = 4)
	val itemText: String,

	/**
	 * The skills that the player combatants mastered during the battle. These will be shown after taking the loot.
	 */
	@BitField(id = 5)
	@NestedFieldSetting(path = "k", fieldName = "MASTERED_SKILLS_KEYS")
	@NestedFieldSetting(path = "vc", fieldName = "MASTERED_SKILLS_VALUES")
	val masteredSkills: HashMap<PlayableCharacter, HashSet<Skill>>,

	party: List<UsedPartyMember>,
) : BitPostInit {

	@Suppress("unused")
	private constructor() : this(
		0, ArrayList(0),
		ArrayList(0), ArrayList(0),
		"", HashMap(),
		listOf(UsedPartyMember(0, PlayableCharacter(), CharacterState())),
	)

	/**
	 * The index of the currently-selected party member (into [mardek.state.ingame.CampaignState.party]). When the
	 * player claims an item, it will be given to the party member with this index.
	 */
	@BitField(id = 6)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 3)
	var selectedPartyIndex = party[0].index
		private set

	/**
	 * The button or item that is currently selected/highlighted. This can be [SelectedGetAll], [SelectedFinish], or
	 * a [SelectedItem]
	 */
	var selectedElement = if (items.isEmpty()) SelectedFinish else SelectedGetAll

	/**
	 * Whether the mastery screen (mastered skills) are currently being shown.
	 * - When this is `false` (initially), the looted gold/items are shown
	 * - When this is `true`, the skills that were mastered during the past battle are shown. During this state,
	 *   interacting with the loot is no longer possible.
	 *
	 * This variable will be set to `true` after the player 'clicks' on 'Finish', and at least 1 skill was mastered
	 * during the past battle.
	 */
	var showMasteryScreen = false
		private set

	/**
	 * The loot menu should close when `finishAt != 0L && System.nanoTime() >= finishAt`.
	 *
	 * When `finishAt != 0L && System.nanoTime() < finishAt`, the loot menu should be frozen, while a fade-out is being
	 * shown.
	 */
	var finishAt = 0L

	override fun toString() = "BattleLoot(gold=$gold, items=$items)"

	override fun postInit(context: BitPostInit.Context) {
		if (items.isNotEmpty()) selectedElement = SelectedGetAll
	}

	/**
	 * This method should be called whenever an `InputKeyEvent` with `didPress = true` is received while the player is
	 * in the battle loot screen. This should be called by [mardek.state.ingame.area.AreaState.processBattleKeyEvent].
	 */
	fun processKeyPress(key: InputKey, context: UpdateContext) {
		if (finishAt != 0L) return
		if (showMasteryScreen) {
			if (key == InputKey.Interact || key == InputKey.Cancel ||
				key == InputKey.ToggleMenu || key == InputKey.Escape
			) {
				finishAt = System.nanoTime() + FADE_OUT_DURATION
			}
			return
		}

		val soundEffects = context.content.audio.fixedEffects.ui
		val oldPartyIndex = selectedPartyIndex
		if (key == InputKey.MoveLeft) {
			selectedPartyIndex -= 1
			while (selectedPartyIndex >= 0 && context.fullParty[selectedPartyIndex] == null) selectedPartyIndex -= 1
			if (selectedPartyIndex == -1) selectedPartyIndex = context.usedParty.last().index
		}
		if (key == InputKey.MoveRight) {
			selectedPartyIndex += 1
			while (selectedPartyIndex < context.fullParty.size && context.fullParty[selectedPartyIndex] == null) {
				selectedPartyIndex += 1
			}
			if (selectedPartyIndex == context.fullParty.size) selectedPartyIndex = context.usedParty[0].index
		}
		if (oldPartyIndex != selectedPartyIndex) {
			context.soundQueue.insert(soundEffects.scroll1)
		}

		val oldElement = selectedElement
		if (key == InputKey.MoveUp) {
			if (oldElement is SelectedFinish && items.isNotEmpty()) {
				selectedElement = SelectedItem(items.size - 1)
			}
			if (oldElement is SelectedItem) {
				selectedElement = if (oldElement.index == 0) SelectedGetAll
				else SelectedItem(oldElement.index - 1)
			}
			if (oldElement is SelectedGetAll) selectedElement = SelectedFinish
		}
		if (key == InputKey.MoveDown) {
			if (oldElement is SelectedGetAll) {
				selectedElement = if (items.isNotEmpty()) SelectedItem(0) else SelectedFinish
			}
			if (oldElement is SelectedItem) {
				val newIndex = oldElement.index + 1
				selectedElement = if (newIndex < items.size) SelectedItem(newIndex) else SelectedFinish
			}
			if (oldElement is SelectedFinish && items.isNotEmpty()) selectedElement = SelectedGetAll
		}
		if (selectedElement != oldElement) {
			context.soundQueue.insert(soundEffects.scroll1)
		}

		if (key == InputKey.Interact) {
			val memberState = context.fullParty[selectedPartyIndex]!!.second
			if (oldElement is SelectedGetAll && items.isNotEmpty()) {
				var itemIndex = 0
				while (itemIndex < items.size) {
					if (memberState.giveItemStack(items[itemIndex])) {
						items.removeAt(itemIndex)
					} else {
						itemIndex += 1
					}
				}
				if (items.isNotEmpty()) {
					context.soundQueue.insert(soundEffects.clickReject)
				} else {
					context.soundQueue.insert(soundEffects.clickConfirm)
					selectedElement = SelectedFinish
				}
			}
			if (oldElement is SelectedItem) {
				if (memberState.giveItemStack(items[oldElement.index])) {
					context.soundQueue.insert(soundEffects.clickConfirm)
					items.removeAt(oldElement.index)
					if (oldElement.index >= items.size) {
						selectedElement = if (items.isEmpty()) SelectedFinish else SelectedItem(items.size - 1)
					}
				} else context.soundQueue.insert(soundEffects.clickReject)
			}
			if (oldElement is SelectedFinish) {
				context.soundQueue.insert(soundEffects.clickConfirm)
				if (masteredSkills.any { it.value.isNotEmpty() }) {
					showMasteryScreen = true
					context.soundQueue.insert(context.content.audio.fixedEffects.battle.masteredSkill)
				} else {
					finishAt = System.nanoTime() + FADE_OUT_DURATION
				}
			}
		}
	}

	/**
	 * One of the selectable buttons/elements in the battle loot screen: [SelectedGetAll], [SelectedFinish], or a
	 * [SelectedItem]
	 */
	sealed class SelectedElement

	/**
	 * The 'Get All' button
	 */
	data object SelectedGetAll : SelectedElement()

	/**
	 * The 'Finish' button
	 */
	data object SelectedFinish : SelectedElement()

	/**
	 * When [selectedElement] is `SelectedItem(i)`, the player has currently
	 * selected/highlighted the item stack with index `i` in [items]. If the player presses the interact key, that
	 * item will be put in the inventory of the currently-selected playable character.
	 */
	data class SelectedItem(val index: Int) : SelectedElement()

	/**
	 * This class is used as parameter in [processKeyPress], and contains most of the inputs/data that is needed by
	 * this method. Using update context classes like this reduces the number of parameters that would otherwise be
	 * needed for [processKeyPress].
	 */
	class UpdateContext(
		parent: GameStateUpdateContext,

		/**
		 * The result of [mardek.state.ingame.CampaignState.usedPartyMembers]
		 */
		val usedParty: List<UsedPartyMember>,

		/**
		 * The result of [mardek.state.ingame.CampaignState.allPartyMembers]
		 */
		val fullParty: WholeParty,
	) : GameStateUpdateContext(parent)

	companion object {

		/**
		 * The duration of the fade-out after the player exits the battle loot menu (in nanoseconds).
		 */
		const val FADE_OUT_DURATION = 500_000_000L

		@Suppress("unused")
		@ReferenceField(stable = true, label = "playable characters")
		private const val MASTERED_SKILLS_KEYS = false

		@Suppress("unused")
		@ReferenceField(stable = true, label = "skills")
		private const val MASTERED_SKILLS_VALUES = false
	}
}
