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
import mardek.state.UsedPartyMember
import mardek.state.WholeParty

@BitStruct(backwardCompatible = true)
class BattleLoot(
	@BitField(id = 0)
	@IntegerField(expectUniform = false, minValue = 0)
	val gold: Int,

	@BitField(id = 1)
	val items: ArrayList<ItemStack>,

	@BitField(id = 2)
	@ReferenceField(stable = true, label = "plot items")
	val plotItems: ArrayList<PlotItem>,

	@BitField(id = 3)
	@ReferenceField(stable = true, label = "dreamstones")
	val dreamStones: ArrayList<Dreamstone>,

	@BitField(id = 4)
	val itemText: String,

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

	@BitField(id = 6)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 3)
	var selectedPartyIndex = party[0].index
		private set

	var selectedElement = if (items.isEmpty()) SelectedFinish else SelectedGetAll

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

	sealed class SelectedElement

	data object SelectedGetAll : SelectedElement()

	data object SelectedFinish : SelectedElement()

	data class SelectedItem(val index: Int) : SelectedElement()

	class UpdateContext(
		parent: GameStateUpdateContext,
		val usedParty: List<UsedPartyMember>,
		val fullParty: WholeParty,
	) : GameStateUpdateContext(parent)

	companion object {
		const val FADE_OUT_DURATION = 500_000_000L

		@Suppress("unused")
		@ReferenceField(stable = true, label = "playable characters")
		private const val MASTERED_SKILLS_KEYS = false

		@Suppress("unused")
		@ReferenceField(stable = true, label = "skills")
		private const val MASTERED_SKILLS_VALUES = false
	}
}
