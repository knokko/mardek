package mardek.state.ingame.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import mardek.assets.area.Area

@BitStruct(backwardCompatible = false)
class AreaDiscoveryMap {

	@NestedFieldSetting(path = "k", fieldName = "MAP_KEY")
	@NestedFieldSetting(path = "v", writeAsBytes = true)
	private val map = HashMap<Area, AreaDiscovery>()

	fun readOnly(area: Area) = map[area] ?: AreaDiscovery(area)

	fun readWrite(area: Area) = map.computeIfAbsent(area, ::AreaDiscovery)

	companion object {

		@Suppress("unused")
		@ReferenceField(stable = true, label = "areas")
		private val MAP_KEY = false
	}
}