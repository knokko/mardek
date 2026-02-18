package mardek.state.ingame.area.discovery

import com.github.knokko.bitser.Bitser
import mardek.content.BITSER
import mardek.content.Content
import mardek.content.area.Area
import mardek.content.area.AreaFlags
import mardek.content.area.AreaProperties
import mardek.content.area.Tilesheet
import mardek.content.area.objects.AreaObjects
import mardek.state.ingame.area.AreaDiscovery
import mardek.state.ingame.area.AreaDiscoveryMap
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class TestAreaDiscoveryMap {

	private fun createArea(width: Int, height: Int, minTileX: Int, minTileY: Int) = Area(
		width = width,
		height = height,
		minTileX = minTileX,
		minTileY = minTileY,
		tilesheet = Tilesheet(),
		tileGrid = emptyArray(),
		objects = AreaObjects(),
		chests = ArrayList(),
		randomBattles = null,
		flags = AreaFlags(),
		properties = AreaProperties(),
		id = UUID(1, 2),
	)

	private val smallArea = createArea(10, 15, -3, 8)
	private val largeArea = createArea(30, 50, -10, 5)

	@Test
	fun testSizeDoesNotChange() {
		fun check(mapping: AreaDiscoveryMap) {
			for (x in -AreaDiscovery.RADIUS ..AreaDiscovery.RADIUS) {
				for (y in 5 .. 4 + AreaDiscovery.RADIUS) {
					assertTrue(mapping.readOnly(largeArea).isDiscovered(x, y))
					assertTrue(mapping.readWrite(largeArea).isDiscovered(x, y))

					if (x >= smallArea.minTileX && x <= smallArea.maxTileX && y >= smallArea.minTileY && y <= smallArea.maxTileY) {
						assertFalse(mapping.readOnly(smallArea).isDiscovered(x, y))
					}
				}
				assertFalse(mapping.readOnly(largeArea).isDiscovered(x, 5 + AreaDiscovery.RADIUS))
			}
		}

		val mapping = AreaDiscoveryMap()
		assertFalse(mapping.readOnly(largeArea).isDiscovered(3, 7))
		mapping.readWrite(largeArea).discover(0, 4)
		check(mapping)

		val content = Content()
		content.areas.areas.add(largeArea)
		val copied = BITSER.stupidDeepCopy(mapping, Bitser.BACKWARD_COMPATIBLE, content)
		check(copied)
	}

	@Test
	fun testAreaShrinks() {
		val mapping = AreaDiscoveryMap()
		mapping.readWrite(largeArea).discover(0, 10)
		for (x in -AreaDiscovery.RADIUS ..AreaDiscovery.RADIUS) {
			for (y in 5 .. 10 + AreaDiscovery.RADIUS) {
				assertTrue(mapping.readOnly(largeArea).isDiscovered(x, y))
			}
			assertFalse(mapping.readOnly(largeArea).isDiscovered(x, 11 + AreaDiscovery.RADIUS))
		}

		val oldContent = Content()
		oldContent.areas.areas.add(largeArea)
		val bytes = BITSER.toBytes(mapping, Bitser.BACKWARD_COMPATIBLE, oldContent)

		val newContent = Content()
		newContent.areas.areas.add(smallArea)
		val copied = BITSER.fromBytes(
			AreaDiscoveryMap::class.java, bytes,
			Bitser.BACKWARD_COMPATIBLE, newContent,
		)

		for (x in -3 .. 6) {
			for (y in 8 .. 10 + AreaDiscovery.RADIUS) {
				assertTrue(copied.readOnly(smallArea).isDiscovered(x, y))
			}
			assertFalse(copied.readOnly(smallArea).isDiscovered(x, 11 + AreaDiscovery.RADIUS))
		}
	}

	@Test
	fun testAreaGrows() {
		val mapping = AreaDiscoveryMap()
		mapping.readWrite(smallArea).discover(-1, 10)

		val oldContent = Content()
		oldContent.areas.areas.add(smallArea)
		val bytes = BITSER.toBytes(mapping, Bitser.BACKWARD_COMPATIBLE, oldContent)

		val newContent = Content()
		newContent.areas.areas.add(largeArea)
		val copied = BITSER.fromBytes(
			AreaDiscoveryMap::class.java, bytes,
			Bitser.BACKWARD_COMPATIBLE, newContent,
		)

		for (x in -3 .. 6) {
			for (y in 8 .. 10 + AreaDiscovery.RADIUS) {
				assertTrue(mapping.readOnly(smallArea).isDiscovered(x, y))
				assertTrue(copied.readOnly(largeArea).isDiscovered(x, y))
			}
			assertFalse(mapping.readOnly(smallArea).isDiscovered(x, 11 + AreaDiscovery.RADIUS))
			assertFalse(copied.readOnly(largeArea).isDiscovered(x, 11 + AreaDiscovery.RADIUS))
			assertFalse(copied.readOnly(largeArea).isDiscovered(x, 7))
		}

		assertFalse(copied.readOnly(largeArea).isDiscovered(7, 10))
	}
}
