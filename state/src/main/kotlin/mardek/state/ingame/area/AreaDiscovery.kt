package mardek.state.ingame.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import mardek.assets.area.Area
import kotlin.math.max
import kotlin.math.min

@BitStruct(backwardCompatible = false)
class AreaDiscovery(area: Area) {

	@BitField(ordering = 0)
	@IntegerField(expectUniform = false, minValue = 1)
	private val width = area.width

	@NestedFieldSetting(path = "", writeAsBytes = true)
	private val raw = BooleanArray(area.width * area.height)

	@Suppress("unused")
	private constructor() : this(Area())

	fun isDiscovered(x: Int, y: Int) = raw[x + y * width]

	fun discover(playerX: Int, playerY: Int) {
		val range = 9
		val height = raw.size / width
		for (x in max(0, playerX - range) .. min(width - 1, playerX + range)) {
			for (y in max(0, playerY - range) .. min(height - 1, playerY + range)) {
				raw[x + y * width] = true
			}
		}
	}
}
