package mardek.content.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceField
import java.io.Serializable

/**
 * This class stores all the [Tile]s of an [Area]
 */
@BitStruct(backwardCompatible = true)
class TileGrid(

	@BitField(id = 0)
	@ReferenceField(stable = false, label = "tiles")
	internal val array: Array<Tile>
) : Serializable {
	internal constructor() : this(emptyArray())
}
