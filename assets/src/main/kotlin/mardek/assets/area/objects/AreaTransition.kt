package mardek.assets.area.objects

import mardek.assets.area.TransitionDestination

class AreaTransition(val x: Int, val y: Int, val destination: TransitionDestination, val arrow: String?) {

	override fun toString() = "Transition(x=$x, y=$y, arrow=$arrow, destination=$destination)"

	override fun equals(other: Any?) = other is AreaTransition && x == other.x && y == other.y &&
			destination == other.destination && arrow == other.arrow

	override fun hashCode() = x - 13 * y + 17 * destination.hashCode()
}