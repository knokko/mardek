package mardek.assets.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.ReferenceFieldTarget
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.assets.area.objects.AreaObjects
import java.util.*

@BitStruct(backwardCompatible = false)
class Area(

	@BitField(ordering = 0)
	@IntegerField(expectUniform = false, minValue = 1)
	val width: Int,

	@BitField(ordering = 1)
	@IntegerField(expectUniform = false, minValue = 1)
	val height: Int,

	@BitField(ordering = 2)
	@ReferenceField(stable = false, label = "tilesheets")
	val tilesheet: Tilesheet,

	@BitField(ordering = 3)
	@ReferenceField(stable = false, label = "tiles")
	@NestedFieldSetting(path = "", optional = true)
	var tileGrid: Array<Tile>?,

	@BitField(ordering = 4)
	val objects: AreaObjects,

	@BitField(ordering = 5)
	@ReferenceFieldTarget(label = "chests")
	val chests: ArrayList<Chest>,

	@BitField(ordering = 6, optional = true)
	val randomBattles: RandomAreaBattles?,

	@BitField(ordering = 7)
	val flags: AreaFlags,

	@BitField(ordering = 8)
	val properties: AreaProperties,
) {

	@BitField(ordering = 9)
	lateinit var canWalkGrid: BooleanArray
	// TODO Save conditionally

	@BitField(ordering = 10)
	@StableReferenceFieldId
	val id = UUID.randomUUID()!!

	constructor() : this(
		0, 0, Tilesheet(), emptyArray(), AreaObjects(), ArrayList(),
		null, AreaFlags(), AreaProperties()
	)

	override fun toString() = properties.displayName

	fun canWalkOnTile(x: Int, y: Int): Boolean {
		return if (x < 0 || x >= width || y < 0 || y >= height) false
		else canWalkGrid[x + y * width]
	}

	fun getTile(x: Int, y: Int) = tileGrid!![x + y * width]
}
