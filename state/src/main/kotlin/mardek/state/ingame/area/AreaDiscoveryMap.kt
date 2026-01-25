package mardek.state.ingame.area

import com.github.knokko.bitser.BitPostInit
import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.area.Area

/**
 * Tracks which tiles in each area have already been *discovered* on their area map.
 *
 * When tiles are *discovered*, players can see them when they open the area map in their in-game menu. Players
 * automatically discover tiles that are close to their main character (normally Mardek). By walking through the area,
 * more and more tiles will be discovered automatically.
 */
@BitStruct(backwardCompatible = true)
class AreaDiscoveryMap : BitPostInit {

	@BitField(id = 0)
	@NestedFieldSetting(path = "k", fieldName = "MAP_KEY")
	@NestedFieldSetting(path = "v", writeAsBytes = true)
	private val map = HashMap<Area, AreaDiscovery>()

	/**
	 * Gets a read-only [AreaDiscovery] instance for `area`. When no discovery data for `area` exists, this will return
	 * an empty instance whose [AreaDiscovery.isDiscovered] always returns false.
	 *
	 * Modifying the returned discovery data may or may not have any effect.
	 */
	fun readOnly(area: Area) = map[area] ?: AreaDiscovery(area)

	/**
	 * Gets the [AreaDiscovery] instance for `area`, and initializes it if it doesn't exist yet.
	 */
	fun readWrite(area: Area) = map.computeIfAbsent(area, ::AreaDiscovery)

	override fun postInit(context: BitPostInit.Context?) {
		map.keys.removeIf { it.flags.hasClearMap }
		for ((area, discovery) in map) discovery.validateOffsetsAndSize(area)
	}

	companion object {

		@Suppress("unused")
		@ReferenceField(stable = true, label = "areas")
		private const val MAP_KEY = false
	}
}
