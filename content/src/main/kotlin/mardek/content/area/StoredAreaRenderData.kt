package mardek.content.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import mardek.content.sprite.KimSprite
import java.util.*

@BitStruct(backwardCompatible = true)
class StoredAreaRenderData(
	@BitField(id = 0)
	val areaID: UUID,

	@BitField(id = 1)
	@NestedFieldSetting(path = "", sizeField = IntegerField(expectUniform = true, minValue = 1, maxValue = 1024))
	val tileSprites: Array<KimSprite>,

	@BitField(id = 2)
	@NestedFieldSetting(path = "", sizeField = IntegerField(expectUniform = true, minValue = 5, maxValue = 5))
	val waterSprites: Array<KimSprite>,

	@BitField(id = 3)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 1023)
	val indirectLowTiles: IntArray,

	@BitField(id = 4)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 1023)
	@NestedFieldSetting(path = "c", optional = true)
	val indirectHigherTiles: Array<Int?>,

	@BitField(id = 5)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 3)
	val indirectWater: IntArray,

	@BitField(id = 6)
	@IntegerField(expectUniform = false, minValue = 1)
	val width: Int,
) {
	init {
		if (indirectLowTiles.size != indirectWater.size) {
			throw IllegalArgumentException("indirectLowTiles and indirectWater must have same size")
		}
		if (indirectLowTiles.size * 2 != indirectHigherTiles.size) {
			throw IllegalArgumentException("indirectHigherTiles must be twice as large as indirectLowTiles")
		}
		if (indirectLowTiles.size % width != 0) {
			throw IllegalArgumentException("indirectLowTiles.size must be divisible by width")
		}
	}

	@Suppress("unused")
	private constructor() : this(
		UUID.randomUUID(), Array(1) { KimSprite() }, Array(1) { KimSprite() },
		IntArray(1), Array(2) { null }, IntArray(1), 1
	)
}
