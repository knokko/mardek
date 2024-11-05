package mardek.assets.area.objects

import mardek.assets.area.TransitionDestination
import mardek.assets.area.sprites.ObjectSpritesheet

class AreaPortal(val x: Int, val y: Int, val destination: TransitionDestination) {

	var spritesheet: ObjectSpritesheet? = null

	override fun toString() = "Portal(x=$x, y=$y, destination=$destination)"

	override fun equals(other: Any?) = other is AreaPortal && x == other.x && y == other.y &&
			destination == other.destination

	override fun hashCode() = destination.hashCode()
}
