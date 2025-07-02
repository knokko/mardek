package mardek.content.action

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.audio.SoundEffect

/**
 * The *action* of a `FixedActionNode` (e.g. walking or talking)
 */
@BitStruct(backwardCompatible = true)
sealed class FixedAction {
	companion object {

		@JvmStatic
		@Suppress("unused")
		val BITSER_HIERARCHY = arrayOf(
			ActionWalk::class.java,
			ActionTalk::class.java,
			ActionBattle::class.java,
			ActionFlashScreen::class.java,
			ActionPlaySound::class.java,
			ActionHealParty::class.java,
			ActionSaveCampaign::class.java,
		)
	}
}

/**
 * Forces the `target` to walk to the tile with the given coordinates at the given speed. The target will take a
 * shortest path from its current position to the destination position, and ignore any walls or blockades.
 */
@BitStruct(backwardCompatible = true)
class ActionWalk(

	/**
	 * The 'target' that will be forced to walk
	 */
	@BitField(id = 0)
	@ClassField(root = ActionTarget::class)
	val target: ActionTarget,

	/**
	 * The X-coordinate of the destination tile
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	val destinationX: Int,

	/**
	 * The Y-coordinate of the destination file
	 */
	@BitField(id = 2)
	@IntegerField(expectUniform = false, minValue = 0)
	val destinationY: Int,

	/**
	 * The walking speed during this action, usually `WalkSpeed.Normal`.
	 */
	@BitField(id = 3)
	val speed: WalkSpeed,
) : FixedAction() {
	internal constructor() : this(
		ActionTargetWholeParty(), 0, 0, WalkSpeed.Normal
	)
}

/**
 * Makes the `speaker` say something in a dialogue box.
 */
@BitStruct(backwardCompatible = true)
class ActionTalk(

	/**
	 * The character (or object) that should speak. This determines the portrait that will be shown in the dialogue
	 * box.
	 */
	@BitField(id = 0)
	@ClassField(root = ActionTarget::class)
	val speaker: ActionTarget,

	/**
	 * The facial expression of the portrait, for instance "norm" or "susp".
	 */
	@BitField(id = 1)
	val expression: String,

	/**
	 * The text that will appear in the dialogue box
	 */
	@BitField(id = 2)
	val text: String,
) : FixedAction() {

	@Suppress("unused")
	private constructor() : this(ActionTargetPartyMember(), "", "")
}

/**
 * Starts a battle, typically a boss battle
 */
@BitStruct(backwardCompatible = true)
class ActionBattle() : FixedAction()

/**
 * Creates a brief color 'flash' on top of the entire screen, e.g. the blue save crystal 'flash'.
 * The game will instantly move on to the next action node, *before* the flash has faded.
 */
@BitStruct(backwardCompatible = true)
class ActionFlashScreen(
	/**
	 * The color of the flash (packed by the `ColorPacker` class)
	 */
	@BitField(id = 0)
	@IntegerField(expectUniform = true)
	val color: Int
) : FixedAction() {

	@Suppress("unused")
	private constructor() : this(0)
}

/**
 * Plays a sound effect, and immediately moves to the next node.
 */
@BitStruct(backwardCompatible = true)
class ActionPlaySound(
	/**
	 * The sound effect to be played
	 */
	@BitField(id = 0)
	@ReferenceField(stable = true, label = "sound effects")
	val sound: SoundEffect
) : FixedAction() {

	@Suppress("unused")
	private constructor() : this(SoundEffect())
}

/**
 * Restores all health and mana of all party members, and removes all their status effects.
 */
@BitStruct(backwardCompatible = true)
class ActionHealParty() : FixedAction()

/**
 * When this action node is reached, a menu should be opened where players can save their progress.
 */
@BitStruct(backwardCompatible = true)
class ActionSaveCampaign() : FixedAction()
