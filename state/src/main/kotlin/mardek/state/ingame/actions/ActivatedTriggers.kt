package mardek.state.ingame.actions

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.area.objects.AreaTrigger

/**
 * This class keeps track of the action triggers (e.g. events when the player enters an area for the first time) have
 * already been activated. It is needed to ensure that some triggers are activated only once, or only once per area
 * visit.
 */
@BitStruct(backwardCompatible = true)
class ActivatedTriggers {

	@BitField(id = 0)
	@ReferenceField(stable = true, label = "area triggers")
	private val oneTime = HashSet<AreaTrigger>()

	@BitField(id = 1)
	@ReferenceField(stable = true, label = "area triggers")
	private val oncePerAreaLoad = HashSet<AreaTrigger>()

	/**
	 * This method should be called whenever the current area is changed
	 */
	fun onAreaSwitch() {
		oncePerAreaLoad.clear()
	}

	/**
	 * Checks whether the given trigger can be activated.
	 * - If this method returns `false`, the trigger cannot be activated because it was already activated.
	 * - If this method returns `true`, the trigger should be activated. If the trigger is one-time-only, the next
	 *   call to `activateTrigger(sameTrigger)` will return `false`.
	 */
	fun activeTrigger(trigger: AreaTrigger): Boolean {
		if (trigger.oneTimeOnly && oneTime.contains(trigger)) return false
		if (trigger.oncePerAreaLoad && oncePerAreaLoad.contains(trigger)) return false

		if (trigger.oneTimeOnly) oneTime.add(trigger)
		if (trigger.oncePerAreaLoad) oncePerAreaLoad.add(trigger)
		return true
	}
}
