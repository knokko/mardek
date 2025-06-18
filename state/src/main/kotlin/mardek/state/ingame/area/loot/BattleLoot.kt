package mardek.state.ingame.area.loot

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.characters.PlayableCharacter
import mardek.content.inventory.Dreamstone
import mardek.content.inventory.ItemStack
import mardek.content.inventory.PlotItem
import mardek.input.InputKey
import mardek.state.GameStateUpdateContext
import mardek.state.ingame.characters.CharacterState

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

	party: Array<PlayableCharacter?>,
) {
	@Suppress("unused")
	private constructor() : this(
		0, ArrayList(0),
		ArrayList(0), ArrayList(0),
		"", emptyArray()
	)

	@BitField(id = 5)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 3)
	var selectedPartyIndex = party.indexOfFirst { it != null }
		private set

	var selectedElement: SelectedElement = SelectedGetAll

	fun processKeyPress(key: InputKey, context: UpdateContext) {
		val soundEffects = context.content.audio.fixedEffects.ui
		val oldPartyIndex = selectedPartyIndex
		if (key == InputKey.MoveLeft) {
			selectedPartyIndex -= 1
			while (selectedPartyIndex >= 0 && context.party[selectedPartyIndex] == null) selectedPartyIndex -= 1
			if (selectedPartyIndex == -1) selectedPartyIndex = context.party.indexOfLast { it != null }
		}
		if (key == InputKey.MoveRight) {
			selectedPartyIndex += 1
			while (selectedPartyIndex < context.party.size && context.party[selectedPartyIndex] == null) {
				selectedPartyIndex += 1
			}
			if (selectedPartyIndex == context.party.size) selectedPartyIndex = context.party.indexOfFirst { it != null }
		}
		if (oldPartyIndex != selectedPartyIndex) {
			// TODO test this
			context.soundQueue.insert(soundEffects.scroll)
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
			context.soundQueue.insert(soundEffects.scroll)
		}

		if (key == InputKey.Interact) {
			val (_, memberState) = context.party[selectedPartyIndex]!!
			if (oldElement is SelectedGetAll && items.isNotEmpty()) {
				while (items.isNotEmpty()) {
					if (memberState.giveItemStack(items.first())) {
						items.removeFirst()
					} else break
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
		}
	}

	sealed class SelectedElement

	data object SelectedGetAll : SelectedElement()

	data object SelectedFinish : SelectedElement()

	data class SelectedItem(val index: Int) : SelectedElement()

	class UpdateContext(
		parent: GameStateUpdateContext,
		val party: Array<Pair<PlayableCharacter, CharacterState>?>
	) : GameStateUpdateContext(parent)
}
