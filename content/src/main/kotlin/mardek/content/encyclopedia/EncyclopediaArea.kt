package mardek.content.encyclopedia

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.content.battle.BattleBackground
import mardek.content.expression.ConstantStateExpression
import mardek.content.expression.ExpressionBooleanValue
import mardek.content.expression.StateExpression
import java.util.UUID

/**
 * Represents an entry of the "Places" section of the "Encyclopedia" tab (of the in-game menu).
 */
@BitStruct(backwardCompatible = true)
class EncyclopediaArea(

	/**
	 * The unique ID of this area entry, which is used for (de)serialization
	 */
	@BitField(id = 0)
	@StableReferenceFieldId
	val id: UUID,

	/**
	 * The name of the area, which is shown in the list of places/areas.
	 */
	@BitField(id = 1)
	val name: String,

	/**
	 * The description of the area, which is shown when this entry is opened.
	 */
	@BitField(id = 2)
	val description: String,

	/**
	 * The background/image of the area, which is shown when this entry is opened.
	 */
	@BitField(id = 3)
	@ReferenceField(stable = false, label = "battle backgrounds")
	val background: BattleBackground,

	/**
	 * Whether this entry should show up in the encyclopedia:
	 * - When this evaluates to `false`, this entry is completely hidden, and does *not* even claim any space in the
	 * encyclopedia page at all.
	 * - When this evaluates to `true`, this entry will claim space in the encyclopedia.
	 * However, the name and information about the area are not shown until the player has *discovered* the area.
	 *
	 * This expression is used to hide areas that the player can only find in later chapters. For instance:
	 * - The `shouldShowUp` of Goznor should always evaluate to `true`, since the player can visit Goznor in chapter 1.
	 * - The `shouldShowUp` of Aeropolis should only evaluate to `true` in chapter 3, since the player cannot
	 * visit it in chapters 1 or 2.
	 */
	@BitField(id = 4)
	@ClassField(root = StateExpression::class)
	val shouldShowUp: StateExpression<Boolean>,
) {

	@Suppress("unused")
	private constructor() : this(
		UUID(0, 0), "", "", BattleBackground(),
		ConstantStateExpression(ExpressionBooleanValue(false)),
	)
}