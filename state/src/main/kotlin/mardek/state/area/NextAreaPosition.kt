package mardek.state.area

import kotlin.time.Duration

class NextAreaPosition(val position: AreaPosition, val startTime: Duration, val arrivalTime: Duration) {

	override fun toString() = "($position at $arrivalTime)"
}
