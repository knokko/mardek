package mardek.assets.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import mardek.assets.area.objects.AreaObjects

@BitStruct(backwardCompatible = false)
class Area(

	@BitField(ordering = 0)
	@IntegerField(expectUniform = false, minValue = 1)
	val width: Int,

	@BitField(ordering = 1)
	@IntegerField(expectUniform = false, minValue = 1)
	val height: Int,

	@BitField(ordering = 0)
	@ReferenceField(stable = false, label = "tilesheets")
	val tilesheet: Tilesheet,

	@BitField(ordering = 0)
	@ReferenceField(stable = false, label = "tiles")
	@NestedFieldSetting(path = "", optional = true)
	var tileGrid: Array<Tile>?,

	@BitField(ordering = 3)
	val objects: AreaObjects,

	// TODO Loot

	@BitField(ordering = 4, optional = true)
	val randomBattles: RandomAreaBattles?,

	@BitField(ordering = 5)
	val flags: AreaFlags,

	@BitField(ordering = 6)
	val properties: AreaProperties,
) {

	@BitField(ordering = 0)
	lateinit var canWalkGrid: BooleanArray

	// TODO Save conditionally
	@BitField(ordering = 7)
	@IntegerField(expectUniform = true)
	var renderLowTilesOffset = -1

	@BitField(ordering = 8)
	@IntegerField(expectUniform = true)
	var renderHighTilesOffset = -1

	constructor() : this(
		0, 0, Tilesheet(), emptyArray(), AreaObjects(), null,
		AreaFlags(), AreaProperties()
	)

	fun canWalkOnTile(x: Int, y: Int): Boolean {
		return if (x < 0 || x >= width || y < 0 || y >= height) false
		else canWalkGrid[x + y * width]
	}

	fun getTile(x: Int, y: Int) = tileGrid!![x + y * width]
}
