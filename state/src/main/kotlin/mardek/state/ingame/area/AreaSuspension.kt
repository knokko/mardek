package mardek.state.ingame.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.area.Chest
import mardek.content.area.TransitionDestination
import mardek.content.area.objects.AreaDoor
import mardek.content.battle.Battle
import mardek.state.ingame.actions.AreaActionsState
import mardek.state.ingame.area.loot.BattleLoot
import mardek.state.ingame.area.loot.ObtainedItemStack
import mardek.state.ingame.battle.BattleState
import kotlin.time.Duration

/**
 * Every possible area suspension (see [AreaState.suspension]) is a subclass of `AreaSuspension`.
 */
sealed class AreaSuspension {

	/**
	 * Whether [AreaState.currentTime] should keep increasing during this suspension
	 */
	abstract fun shouldUpdateCurrentTime(): Boolean

	companion object {

		@Suppress("unused")
		private val BITSER_HIERARCHY = arrayOf(
			AreaSuspensionPlayerWalking::class.java,
			AreaSuspensionIncomingRandomBattle::class.java,
			AreaSuspensionBattle::class.java,
			AreaSuspensionActions::class.java,
			// The state should never be AreaSuspensionTransition outside CampaignState.update()
			AreaSuspensionOpeningDoor::class.java,
			AreaSuspensionOpeningChest::class.java,
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
	 * The time at which the battle will start. When `canAvoid` is `true`, the player can skip the battle until
	 * `areaState.currentTime >= startAt`.
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = true)
	val startAt: Duration,

	/**
	 * Whether the player can avoid the random battle (by pressing Q or Z)
	 */
	@BitField(id = 2)
	val canAvoid: Boolean,
) : AreaSuspension() {

	@Suppress("unused")
	private constructor() : this(Battle(), Duration.ZERO, false)

	override fun shouldUpdateCurrentTime() = true
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

	override fun shouldUpdateCurrentTime() = false // TODO CHAP1 Change to `true` after refactor
}

/**
 * The area state is suspended because the player activated an area transition. When the `CampaignState` sees this,
 * it must transition the player to the destination area or world map **before** the end of its `update()` method.
 * This means that [AreaState.suspension] can only be `AreaSuspensionTransition` *during* `CampaignState.update(...)`.
 */
class AreaSuspensionTransition(val destination: TransitionDestination) : AreaSuspension() {

	override fun shouldUpdateCurrentTime() = false
}

/**
 * The area state is suspended because the player is currently opening a door. The suspension will be changed to
 * `AreaSuspensionTransition` after the door open animation is finished.
 */
@BitStruct(backwardCompatible = true)
class AreaSuspensionOpeningDoor(

	@BitField(id = 0)
	@ReferenceField(stable = true, label = "doors")
	val door: AreaDoor,

	@BitField(id = 1)
	@IntegerField(expectUniform = true)
	val finishTime: Duration
) : AreaSuspension() {

	@Suppress("unused")
	private constructor() : this(AreaDoor(), Duration.ZERO)

	override fun shouldUpdateCurrentTime() = true
}

/**
 * The area state is suspended because the player is looking inside a chest.
 */
@BitStruct(backwardCompatible = true)
class AreaSuspensionOpeningChest(
	@BitField(id = 0)
	@ReferenceField(stable = true, label = "chests")
	val chest: Chest
) : AreaSuspension() {

	/**
	 * This field is initially null, but the `CampaignState` should set it to something non-null as soon as it sees
	 * this.
	 */
	var obtainedItem: ObtainedItemStack? = null

	@Suppress("unused")
	private constructor() : this(Chest())

	override fun shouldUpdateCurrentTime() = false
}
