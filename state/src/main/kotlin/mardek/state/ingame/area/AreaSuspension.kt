package mardek.state.ingame.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.area.AreaTransitionDestination
import mardek.content.area.Chest
import mardek.content.area.TransitionDestination
import mardek.content.area.objects.AreaDoor
import mardek.content.battle.Battle
import mardek.content.characters.PlayableCharacter
import mardek.state.ingame.actions.AreaActionsState
import mardek.state.ingame.area.loot.BattleLoot
import mardek.state.ingame.area.loot.ObtainedItemStack
import mardek.state.ingame.battle.BattleState
import mardek.content.util.Time
import java.io.Serializable
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Every possible area suspension (see [AreaState.suspension]) is a subclass of `AreaSuspension`.
 * When an area is 'suspended' by any `AreaSuspension`, the normal update flow is suspended until the suspension is
 * gone. This makes sure that e.g. the player cannot walk while engaged in a battle, or looking inside a chest.
 */
sealed class AreaSuspension : Serializable {

	/**
	 * Whether [AreaState.currentTime] should keep increasing during this suspension
	 */
	abstract fun shouldUpdateCurrentTime(): Boolean

	companion object {

		@Suppress("unused")
		private val BITSER_HIERARCHY = arrayOf(
			AreaSuspensionPlayerWalking::class.java,
			AreaSuspensionIncomingRandomBattle::class.java,
			AreaSuspensionIncomingBattle::class.java,
			AreaSuspensionBattle::class.java,
			AreaSuspensionActions::class.java,
			AreaSuspensionOpeningDoor::class.java,
			AreaSuspensionOpeningChest::class.java,
			AreaSuspensionTransition::class.java,
		)
	}
}

/**
 * The area state is suspended because the party is currently walking to another tile. The suspension will be ended
 * once they reach the destination tile.
 */
@BitStruct(backwardCompatible = true)
class AreaSuspensionPlayerWalking(

	/**
	 * The destination tile for the main/first party member.
	 */
	@BitField(id = 0)
	val destination: NextAreaPosition
) : AreaSuspension() {

	@Suppress("unused")
	private constructor() : this(NextAreaPosition())

	override fun shouldUpdateCurrentTime() = true
}

/**
 * The area state is suspended because a random battle might start soon. During this suspension, a blue or red
 * exclamation mark will be shown, depending on whether the player can avoid this random battle (`canAvoid`).
 *
 * When `canAvoid` is `true`, the player can avoid the battle by pressing the cancel button (Q or Z).
 */
@BitStruct(backwardCompatible = true)
class AreaSuspensionIncomingRandomBattle(

	/**
	 * The battle that will start, unless the player avoids it.
	 */
	@BitField(id = 0)
	val battle: Battle,

	/**
	 * The time at which the red or blue exclamation mark appeared.
	 * When `canAvoid` is `true`, the player can skip the battle until
	 * `areaState.currentTime >= encounteredAt + DURATION`.
	 */
	@BitField(id = 1)
	val encounteredAt: Time,

	/**
	 * Whether the player can avoid the random battle (by pressing Q or Z)
	 */
	@BitField(id = 3)
	val canAvoid: Boolean,
) : AreaSuspension() {

	@Suppress("unused")
	private constructor() : this(Battle(), Time.ZERO, false)

	override fun shouldUpdateCurrentTime() = true

	companion object {

		/**
		 * The amount of time between the moment the exclamation mark appears, and the flickering begins.
		 */
		val DURATION = 1.seconds
	}
}

/**
 * The area state is suspended because a battle will start soon. During this suspension, the screen will flicker black.
 */
@BitStruct(backwardCompatible = true)
class AreaSuspensionIncomingBattle(

	/**
	 * The battle that will start soon
	 */
	@BitField(id = 0)
	val battle: Battle,

	/**
	 * The time at which the 'flickering effect' before the battle started.
	 * The battle will start when `areaState.currentTime >= startedFlickerAt + DURATION`.
	 */
	@BitField(id = 1)
	val startedFlickerAt: Time,

	/**
	 * The players that will join the battle. This is almost always equal to the current party, but there are some
	 * exceptions (e.g. the Muriance fight in chapter 2).
	 */
	@BitField(id = 2)
	@NestedFieldSetting(path = "c", optional = true)
	@ReferenceField(stable = true, label = "playable characters")
	val players: Array<PlayableCharacter?>,

	/**
	 * The next actions, after the player wins this battle. This is usually `null`, but needed for special
	 * actions/dialogue after some boss battles.
	 */
	@BitField(id = 3, optional = true)
	val nextActions: AreaActionsState?,
) : AreaSuspension() {

	@Suppress("unused")
	private constructor() : this(Battle(), Time.ZERO, emptyArray(), null)

	override fun shouldUpdateCurrentTime() = true

	companion object {

		/**
		 * The duration of the 'flickering' effect before a battle starts
		 */
		val DURATION = 850.milliseconds
	}
}

/**
 * The area state is suspended because the player is currently in combat.
 */
@BitStruct(backwardCompatible = true)
class AreaSuspensionBattle(

	/**
	 * The state of the active battle
	 */
	@BitField(id = 0)
	val battle: BattleState,

	/**
	 * The next actions, after the player wins this battle. This is usually `null`, but needed for special
	 * actions/dialogue after some boss battles.
	 */
	@BitField(id = 1, optional = true)
	val nextActions: AreaActionsState? = null,
) : AreaSuspension() {

	/**
	 * The state of the battle loot, which should become non-`null` soon after the player wins the battle.
	 */
	@BitField(id = 2, optional = true)
	var loot: BattleLoot? = null

	@Suppress("unused")
	private constructor() : this(BattleState())

	override fun shouldUpdateCurrentTime() = false
}

/**
 * The area state is suspended because an `ActionSequence` is busy.
 */
@BitStruct(backwardCompatible = true)
class AreaSuspensionActions(

	/**
	 * The state of the ongoing action sequence
	 */
	@BitField(id = 0)
	val actions: AreaActionsState
) : AreaSuspension() {

	@Suppress("unused")
	private constructor() : this(AreaActionsState())

	override fun shouldUpdateCurrentTime() = true
}

/**
 * The area state is suspended because the player activated an area transition.
 * The player should go to the destination area once `area.currentTime >= startTime + FADE_DURATION`.
 */
@BitStruct(backwardCompatible = true)
class AreaSuspensionTransition(

	/**
	 * The destination position (or world map)
	 */
	@BitField(id = 0)
	@ClassField(root = TransitionDestination::class)
	val destination: TransitionDestination,

	/**
	 * The area time when this transition (its fade-out) was started
	 */
	@BitField(id = 1)
	val startTime: Time,
) : AreaSuspension() {

	@Suppress("unused")
	private constructor() : this(AreaTransitionDestination(), Time.ZERO)

	override fun shouldUpdateCurrentTime() = true

	companion object {

		/**
		 * The fade-out duration of area transitions, which starts when the player reaches the destination tile.
		 */
		val FADE_DURATION = 250.milliseconds
	}
}

/**
 * The area state is suspended because the player is currently opening a door.
 * The player should go to the destination area once `area.currentTime >= startTime + FADE_OUT_DURATION`.
 */
@BitStruct(backwardCompatible = true)
class AreaSuspensionOpeningDoor(

	/**
	 * The door that is being opened.
	 */
	@BitField(id = 0)
	@ReferenceField(stable = true, label = "doors")
	val door: AreaDoor,

	/**
	 * - When `areaState.currentTime >= startTime + DOOR_OPEN_DURATION`,
	 * the player is 'teleported' to `door.destination`.
	 * - When `areaState.currentTime < startTime + DOOR_OPEN_DURATION`, the door opening animation is played,
	 * while the player walks to the tile containing the door, and while the area is fading out.
	 */
	@BitField(id = 1)
	val startTime: Time,
) : AreaSuspension() {

	@Suppress("unused")
	private constructor() : this(AreaDoor(), Time.ZERO)

	override fun shouldUpdateCurrentTime() = true

	companion object {

		/**
		 * The time it takes to open a door in an area
		 */
		val DOOR_OPEN_DURATION = 200.milliseconds

		/**
		 * The duration of the area fade-out that starts when the door starts opening.
		 * It is slightly longer than [DOOR_OPEN_DURATION], which means that the door fill be fully open before the
		 * area is fully faded out.
		 */
		val FADE_OUT_DURATION = 250.milliseconds
	}
}

/**
 * The area state is suspended because the player is looking inside a chest.
 */
@BitStruct(backwardCompatible = true)
class AreaSuspensionOpeningChest(

	/**
	 * The chest that is currently opened
	 */
	@BitField(id = 0)
	@ReferenceField(stable = true, label = "chests")
	val chest: Chest,

	/**
	 * The campaign time when the chest was opened, and the fade-in started
	 */
	@BitField(id = 1)
	val openedAt: Time,
) : AreaSuspension() {

	/**
	 * The time at which the player took the item (or canceled), after which the fade-out starts
	 */
	@BitField(id = 2, optional = true)
	var closedAt: Time? = null

	/**
	 * This field is initially null, but the `CampaignState` should set it to something non-null as soon as it sees
	 * this.
	 */
	var obtainedItem: ObtainedItemStack? = null

	@Suppress("unused")
	private constructor() : this(Chest(), Time.ZERO)

	override fun shouldUpdateCurrentTime() = false

	companion object {

		/**
		 * The duration of the fade-in and the fade-out effect when opening/closing chests
		 */
		val FADE_DURATION = 250.milliseconds
	}
}
