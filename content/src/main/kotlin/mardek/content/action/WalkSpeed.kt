package mardek.content.action

import com.github.knokko.bitser.BitEnum
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@BitEnum(mode = BitEnum.Mode.Ordinal)
enum class WalkSpeed(
	/**
	 * How long it takes to move from one tile to the next tile
	 */
	val duration: Duration
) {
	Slow(500.milliseconds),
	Normal(200.milliseconds)
}
