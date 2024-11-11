package mardek.assets.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.CollectionField
import com.github.knokko.bitser.field.IntegerField
import mardek.assets.area.objects.AreaObjects

@BitStruct(backwardCompatible = false)
class OptimizedArea(

	@BitField(ordering = 0)
	@IntegerField(expectUniform = false, minValue = 1)
	val width: Int,

	@BitField(ordering = 1)
	@IntegerField(expectUniform = false, minValue = 1)
	val height: Int,

	@BitField(ordering = 2)
	@CollectionField(writeAsBytes = true)
	private val canWalkGrid: BooleanArray,

	@BitField(ordering = 3)
	val objects: AreaObjects,

	// TODO Loot

	@BitField(ordering = 4, optional = true)
	val randomBattles: RandomAreaBattles?,

	@BitField(ordering = 5)
	val flags: AreaFlags,

	@BitField(ordering = 6)
	val properties: AreaProperties,

	@BitField(ordering = 7)
	@IntegerField(expectUniform = true)
	val renderLowTilesOffset: Int,

	@BitField(ordering = 8)
	@IntegerField(expectUniform = true)
	val renderHighTilesOffset: Int,

	@BitField(ordering = 9)
	@IntegerField(expectUniform = false)
	@CollectionField(size = IntegerField(expectUniform = true, minValue = 5, maxValue = 5))
	val waterSpriteOffsets: IntArray,
) {

	@Suppress("unused")
	internal constructor() : this(
		0, 0, BooleanArray(0), AreaObjects(), null,
		AreaFlags(), AreaProperties(), 0, 0, IntArray(0)
	)

	fun canWalkAt(x: Int, y: Int): Boolean {
		return if (x < 0 || x >= width || y < 0 || y >= height) false
		else canWalkGrid[x + y * width]
	}
}
