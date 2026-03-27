package mardek.content.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.content.BITSER
import java.util.*

/**
 * The layout/positions of a team/party in combat. All combatants will stand in their position during the entire battle,
 * except while they are performing a melee attack.
 */
@BitStruct(backwardCompatible = true)
class PartyLayout(

	/**
	 * The name of the layout, as imported from Flash. It doesn't serve an in-game purpose, but is useful for debugging
	 * and editing.
	 */
	@BitField(id = 0)
	val name: String,

	/**
	 * The positions where the combatants are placed. It must be an array of length 4. The combatant with index `i`
	 * will be placed on the position with index `i`. Note that the X-coordinates of the player team positions are
	 * mirrored to make sure that the players are always on the right side of the battle, whereas the enemies are on
	 * the left side.
	 */
	@BitField(id = 1)
	@NestedFieldSetting(path = "c", sizeField = IntegerField(expectUniform = true, minValue = 4, maxValue = 4))
	val positions: Array<PartyLayoutPosition>,

	/**
	 * The unique ID of the layout, which is used for (de)serialization
	 */
	@BitField(id = 2)
	@StableReferenceFieldId
	val id: UUID,
) {

	constructor() : this("", emptyArray(), UUID.randomUUID())

	override fun toString() = "Layout($name)"
}

/**
 * Represents a single position in a `PartyLayout`.
 */
@BitStruct(backwardCompatible = true)
class PartyLayoutPosition(

	/**
	 * The distance on the X-axis between the party position and the left/right border of the window:
	 * - For enemies, the final X-coordinate should be `leftBorderX + this.x * windowHeight`
	 * - For players, the final X-coordinate should be `rightBorderX - this.x * windowHeight`
	 */
	@BitField(id = 0)
	@FloatField
	val distanceX: Float,

	/**
	 * The distance on the Y-axis between the party position and the top border of the window:
	 * the final Y-coordinate should be `topBorderY + this.y * windowHeight` (assuming that positive Y goes down)
	 */
	@BitField(id = 1)
	@FloatField
	val distanceY: Float,
) {
	@Suppress("unused")
	private constructor() : this(0f, 0f)

	override fun toString() = "($distanceX, $distanceY)"

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)
}
