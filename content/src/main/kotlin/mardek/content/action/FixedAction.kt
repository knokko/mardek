package mardek.content.action

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.area.Area
import mardek.content.area.Direction
import mardek.content.audio.SoundEffect
import mardek.content.battle.Battle
import mardek.content.characters.PlayableCharacter
import mardek.content.sprite.NamedSprite
import mardek.content.story.Timeline
import mardek.content.story.TimelineNode
import kotlin.collections.addAll
import kotlin.time.Duration

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
			ActionShowChapterName::class.java,
			ActionToArea::class.java,
			ActionPlayCutscene::class.java,
			ActionFadeCharacter::class.java,
			ActionRotate::class.java,
			ActionParallel::class.java,
			ActionTimelineTransition::class.java,
			ActionTeleport::class.java,
			ActionSetMoney::class.java,
			ActionItemStorage::class.java,
			ActionSetOverlayColor::class.java,
			ActionSetMusic::class.java,
			ActionSetBackgroundImage::class.java,
			ActionToGlobalActions::class.java,
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

	override fun toString() = "ActionWalk($target, $destinationX, $destinationY, $speed)"
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

	override fun toString() = "ActionTalk($speaker, $expression, $text)"
}

/**
 * Starts a battle, typically a boss battle
 */
@BitStruct(backwardCompatible = true)
class ActionBattle(
	/**
	 * The information about the battle to be started
	 */
	@BitField(id = 0)
	val battle: Battle,

	/**
	 * Most of the time, `overridePlayers` will be null, which means that the current party of the player will fight the
	 * battle.
	 *
	 * When `overridePlayers` is non-null, the player will have to use the `overridePlayers` party to fight the battle.
	 * This is useful for rare cases like the Muriance battle and the Cambria Arena.
	 */
	@BitField(id = 1)
	@NestedFieldSetting(path = "", optional = true, sizeField = IntegerField(
		expectUniform = true, minValue = 4, maxValue = 4
	))
	@NestedFieldSetting(path = "c", optional = true)
	@ReferenceField(stable = false, label = "playable characters")
	val overridePlayers: Array<PlayableCharacter?>?,
) : FixedAction() {

	@Suppress("unused")
	private constructor() : this(Battle(), null)

	override fun toString() = "ActionBattle($battle)"
}

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

	override fun toString() = "ActionFlashScreen($color)"
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
	@ReferenceField(stable = false, label = "sound effects")
	val sound: SoundEffect
) : FixedAction() {

	@Suppress("unused")
	private constructor() : this(SoundEffect())

	override fun toString() = "ActionPlaySound($sound)"
}

/**
 * Restores all health and mana of all party members, and removes all their status effects.
 */
@BitStruct(backwardCompatible = true)
class ActionHealParty() : FixedAction() {

	override fun toString() = "ActionHealParty"
}

/**
 * When this action node is reached, a menu should be opened where players can save their progress.
 */
@BitStruct(backwardCompatible = true)
class ActionSaveCampaign() : FixedAction() {

	override fun toString() = "ActionSaveCampaign"
}

/**
 * Shows the chapter name and the chapter number (using Roman numbers) in the middle of the screen. They will fade in
 * and fade out. This action typically appears at the start of each chapter.
 */
@BitStruct(backwardCompatible = true)
class ActionShowChapterName(

	/**
	 * The chapter number (in the original game, it would be either 1, 2, or 3)
	 */
	@BitField(id = 0)
	@IntegerField(expectUniform = false, minValue = 1)
	val chapter: Int,

	/**
	 * The chapter name (e.g. "A Fallen Star")
	 */
	@BitField(id = 1)
	val name: String,
) : FixedAction() {

	@Suppress("unused")
	private constructor() : this(0, "")

	override fun toString() = "ActionShowChapterName($chapter, $name)"

	companion object {

		/**
		 * The time (in nanoseconds) needed to transition from an alpha/opacity of 0% to 100%, or from 100% to 0%.
		 */
		const val FADE_DURATION = 1500_000_000L

		/**
		 * The time (in nanoseconds) during which the chapter name will be shown with its full opacity
		 */
		const val MAIN_DURATION = 2000_000_000L

		/**
		 * The total duration (in nanoseconds):
		 * 1. The screen is black
		 * 2. The chapter title appears with an alpha/opacity of ~0
		 * 3. The alpha/opacity gradually increases to full opacity, which takes `FADE_DURATION` ns
		 * 4. The chapter title is shown with full opacity for `MAIN_DURATION` ns
		 * 5. The chapter title slowly fades back to alpha/opacity 0, which takes `FADE_DURATION` ns
		 */
		const val TOTAL_DURATION = 2 * FADE_DURATION + MAIN_DURATION
	}
}

/**
 * Instantly 'teleports' the player to an(other) area
 */
@BitStruct(backwardCompatible = true)
class ActionToArea(

	/**
	 * The raw name of the destination area. The destination area will be resolved upon calling `resolve(...)`, which
	 * should happen during the importing process.
	 */
	private val areaName: String,

	/**
	 * The X-coordinate of the destination tile
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	val x: Int,

	/**
	 * The Y-coordinate of the destination tile
	 */
	@BitField(id = 2)
	@IntegerField(expectUniform = false, minValue = 0)
	val y: Int,

	/**
	 * The initial direction in which the player will look
	 */
	@BitField(id = 3)
	val direction: Direction,
) : FixedAction() {

	/**
	 * The destination area
	 *
	 * Note that this will be a 'dummy' area until `resolve(...)` has been called.
	 */
	@BitField(id = 0)
	@ReferenceField(stable = false, label = "areas")
	var area: Area = Area()
		private set

	@Suppress("unused")
	private constructor() : this("", 0, 0, Direction.Up)

	override fun toString() = "ActionToArea($area, $x, $y)"

	/**
	 * This method initializes `area` to the `Area` whose raw name is `areaName`.
	 * This method should only be needed by the importer.
	 */
	fun resolve(areas: Collection<Area>) {
		this.area = areas.find { it.properties.rawName == areaName } ?: throw IllegalArgumentException(
			"Can't find area with raw name $areaName: options are ${areas.map { it.properties.rawName }}"
		)
	}
}

/**
 * Plays a cutscene. This action is automatically finished when the cutscene is over.
 */
@BitStruct(backwardCompatible = true)
class ActionPlayCutscene(

	/**
	 * The cutscene to be played
	 */
	@BitField(id = 0)
	@ReferenceField(stable = false, label = "cutscenes")
	val cutscene: Cutscene,
) : FixedAction() {

	@Suppress("unused")
	private constructor() : this(Cutscene())

	override fun toString() = "ActionPlayCutscene($cutscene)"
}

/**
 * Gradually turns the color of an `AreaCharacter` into transparent red, and removes it once the character is completely
 * transparent.
 *
 * This is typically used on the area characters of bosses after that boss is slain in combat.
 */
@BitStruct(backwardCompatible = true)
class ActionFadeCharacter(

	/**
	 * The character (typically a boss) that should fade away
	 */
	@BitField(id = 0)
	@ClassField(root = ActionTarget::class)
	val target: ActionTargetAreaCharacter
) : FixedAction() {

	@Suppress("unused")
	private constructor() : this(ActionTargetAreaCharacter())

	override fun toString() = "ActionFadeCharacter($target)"
}

/**
 * Rotates the target: the current direction of the target is changed to `newDirection`. The target should be a player
 * character or area character.
 */
@BitStruct(backwardCompatible = true)
class ActionRotate(

	/**
	 * The target that should be rotated. This should be a player or area character.
	 */
	@BitField(id = 0)
	@ClassField(root = ActionTarget::class)
	val target: ActionTarget,

	/**
	 * The new direction/rotation of the target
	 */
	@BitField(id = 1)
	val newDirection: Direction,
) : FixedAction() {

	@Suppress("unused")
	private constructor() : this(ActionTargetPartyMember(0), Direction.Down)

	override fun toString() = "ActionRotate($target, $newDirection)"
}

/**
 * Represents a list of actions that should be executed at the same time (e.g. let multiple area characters walk in
 * parallel).
 */
@BitStruct(backwardCompatible = true)
class ActionParallel(

	/**
	 * The actions to be executed in parallel
	 */
	@BitField(id = 0)
	@ClassField(root = FixedAction::class)
	val actions: Array<FixedAction>
) : FixedAction() {

	@Suppress("unused")
	private constructor() : this(emptyArray())

	override fun toString() = "ActionParallel($actions)"
}

/**
 * An action that transitions the story state to a new timeline node. These actions are crucial for making progress in
 * the story.
 */
@BitStruct(backwardCompatible = true)
class ActionTimelineTransition(
	/**
	 * The name of the timeline whose state/node should be transitioned. The actual `timeline` will be resolved when
	 * `resolve(...)` is called, which should be done by the importer.
	 */
	private val timelineName: String,

	/**
	 * The name of the timeline node to which the timeline should be transitioned. The actual `node` will be resolved
	 * when `resolve(...)` is called, which should be done by the importer.
	 */
	private val nodeName: String,
) : FixedAction() {

	/**
	 * The timeline whose state/node should be transitioned to `newNode`
	 */
	@BitField(id = 0)
	@ReferenceField(stable = false, label = "timelines")
	var timeline: Timeline = Timeline()
		private set

	/**
	 * The timeline node to which the state for `timeline` should be transitioned
	 */
	@BitField(id = 1)
	@ReferenceField(stable = false, label = "timeline nodes")
	var newNode: TimelineNode = TimelineNode()
		private set

	@Suppress("unused")
	private constructor() : this("", "")

	override fun toString() = "ActionTimelineTransition($timeline -> $newNode)"

	/**
	 * This method initializes `timeline` to the `Timeline` whose name is `timelineName`, and initializes `newNode` to
	 * the `TimelineNode` named `nodeName`. This method should only be needed by the importer.
	 */
	fun resolve(timelines: Collection<Timeline>) {
		this.timeline = timelines.find { it.name == timelineName } ?: throw IllegalArgumentException(
			"Can't find timeline with name $timelineName: options are ${timelines.map { it.name }}"
		)

		val allNodes = mutableListOf<TimelineNode>()
		val remainingNodes = mutableListOf(this.timeline.root)
		while (remainingNodes.isNotEmpty()) {
			val nextNode = remainingNodes.removeLast()
			allNodes.add(nextNode)
			remainingNodes.addAll(nextNode.children)
		}

		this.newNode = allNodes.find { it.name == nodeName } ?: throw IllegalArgumentException(
			"Can't find node with name $nodeName: options are ${allNodes.map { it.name }}"
		)
	}
}

/**
 * Instantly teleports `target` to `(x, y)`, and lets it look to `direction`
 */
@BitStruct(backwardCompatible = true)
class ActionTeleport(

	/**
	 * The character to be teleported
	 */
	@BitField(id = 0)
	@ClassField(root = ActionTarget::class)
	val target: ActionTarget,

	/**
	 * The X-coordinate of the destination
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = false)
	val x: Int,

	/**
	 * The Y-coordinate of the destination
	 */
	@BitField(id = 2)
	@IntegerField(expectUniform = false)
	val y: Int,

	/**
	 * The direction that `target` should face after being teleported
	 */
	@BitField(id = 3)
	val direction: Direction,
) : FixedAction() {

	@Suppress("unused")
	private constructor() : this(ActionTargetPartyMember(), 0, 0, Direction.Down)

	override fun toString() = "ActionTeleport($target, $x, $y, $direction)"
}

/**
 * Sets the money of the player to `amount`
 */
@BitStruct(backwardCompatible = true)
class ActionSetMoney(

	/**
	 * The amount of money that the player has right after executing this action.
	 */
	@BitField(id = 0)
	@IntegerField(expectUniform = false, minValue = 0)
	val amount: Int
) : FixedAction() {

	@Suppress("unused")
	private constructor() : this(0)
}

/**
 * Opens the item storage: a large inventory that the player can only access at save crystals. It is typically used
 * for storing items when the inventories of the playable characters are (almost) full.
 */
@BitStruct(backwardCompatible = true)
class ActionItemStorage : FixedAction()

/**
 * (Gradually) changes the overlay color to [color] for the remainder of the `AreaActionsState`, or until the next
 * [ActionSetOverlayColor] is encountered.
 */
@BitStruct(backwardCompatible = true)
class ActionSetOverlayColor(
	/**
	 * The new overlay color
	 */
	@BitField(id = 0)
	@IntegerField(expectUniform = true, commonValues = [0])
	val color: Int,

	/**
	 * - When this is `Duration.ZERO`, the overlay color is instantly changed to [color]
	 * - When this is positive, the overlay color is gradually transitioned from the old color to [color],
	 * which takes `transitionTime` time
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	val transitionTime: Duration,
) : FixedAction() {

	@Suppress("unused")
	private constructor() : this(0, Duration.ZERO)
}

/**
 * Overrides the music truck that should be played for the remainder of the `AreaActionsState`, or until the next
 * [ActionSetMusic] is encountered. In the meantime, the background music of the current area will **not** be played.
 */
@BitStruct(backwardCompatible = true)
class ActionSetMusic(

	/**
	 * The new music track that should be played, or `null` to start playing the background music of the current area
	 * again.
	 */
	@BitField(id = 0, optional = true)
	val newMusicTrack: String?
) : FixedAction() {

	@Suppress("unused")
	private constructor() : this(null)
}

/**
 * Sets the background image of the current `AreaActionsState`. When non-null, this background image will be rendered
 * in front of the current area and its characters, but behind the dialogue. The background image of each
 * `AreaActionsState` is initially `null`.
 */
@BitStruct(backwardCompatible = true)
class ActionSetBackgroundImage(

	/**
	 * The new background image, or `null` to stop rendering the current background image.
	 */
	@BitField(id = 0, optional = true)
	@ReferenceField(stable = false, label = "action background images")
	val newBackgroundImage: NamedSprite?,
) : FixedAction() {

	@Suppress("unused")
	private constructor() : this(null)
}

/**
 * When this action is encountered during an `AreaActionsState`, the campaign state will leave the current area, and it
 * will become a `CampaignActionsState`, which will handle the remaining action nodes.
 */
@BitStruct(backwardCompatible = true)
class ActionToGlobalActions : FixedAction()
