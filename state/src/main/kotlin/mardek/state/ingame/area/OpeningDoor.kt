package mardek.state.ingame.area

import mardek.assets.area.objects.AreaDoor
import kotlin.time.Duration

class OpeningDoor(
	val door: AreaDoor,
	val finishTime: Duration
)
