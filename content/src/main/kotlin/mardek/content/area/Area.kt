package mardek.content.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.ReferenceFieldTarget
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.content.action.ActionSequence
import mardek.content.area.objects.AreaObjects
import java.util.*

@BitStruct(backwardCompatible = true)
class Area(

	@BitField(id = 0)
	@IntegerField(expectUniform = false, minValue = 1)
	val width: Int,

	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 1)
	val height: Int,

	@BitField(id = 2)
	@ReferenceField(stable = false, label = "tilesheets")
	val tilesheet: Tilesheet,

	@BitField(id = 3)
	@ReferenceField(stable = false, label = "tiles")
	val tileGrid: Array<Tile>,

	@BitField(id = 4)
	val objects: AreaObjects,

	@BitField(id = 5)
	@ReferenceFieldTarget(label = "chests")
	val chests: ArrayList<Chest>,

	@BitField(id = 6, optional = true)
	val randomBattles: RandomAreaBattles?,

	@BitField(id = 7)
	val flags: AreaFlags,

	@BitField(id = 8)
	val properties: AreaProperties,
) {

	@BitField(id = 9)
	@ReferenceFieldTarget(label = "action sequences")
	val actions = ArrayList<ActionSequence>()

	@BitField(id = 10)
	@StableReferenceFieldId
	val id = UUID.randomUUID()!!

	constructor() : this(
		0, 0, Tilesheet(), emptyArray(), AreaObjects(), ArrayList(),
		null, AreaFlags(), AreaProperties()
	)

	override fun toString() = properties.displayName

	fun canWalkOnTile(x: Int, y: Int): Boolean {
		if (x !in 0 until width || y !in 0 until height) return false
		if (!getTile(x, y).canWalkOn) return false

		for (chest in chests) {
			if (x == chest.x && y == chest.y) return false
		}

		for (door in objects.doors) {
			if (x == door.x && y == door.y) return false
		}

		for (orb in objects.switchOrbs) {
			if (x == orb.x && y == orb.y) return false
		}

		return true
	}

	fun getTile(x: Int, y: Int) = tileGrid[x + y * width]
}
