package mardek.content.encyclopedia

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.battle.Monster
import mardek.content.expression.ConstantStateExpression
import mardek.content.expression.StateExpression

/**
 * Represents an entry of the "Bestiary" section of the "Encyclopedia" tab (of the in-game menu).
 */
@BitStruct(backwardCompatible = true)
class EncyclopediaMonster(

	/**
	 * The monsters to which this entry is linked. This also determines the name and element that are shown in the
	 * encyclopedia.
	 *
	 * Almost all entries are linked to exactly 1 monster, but there are a few exceptions. For instance, there are 2
	 * 'monsters' that represent Bartholio (one for chapter 2, one for chapter 3), but the Bestiary only has 1 entry
	 * for Bartholio.
	 */
	@BitField(id = 0)
	@ReferenceField(stable = false, label = "monsters")
	val monsters: Array<Monster>,

	/**
	 * The description of the monster, which is shown in the encyclopedia.
	 */
	@BitField(id = 1)
	val description: String,

	/**
	 * Whether this entry should show up in the encyclopedia:
	 * - When this evaluates to `false`, this entry is completely hidden, and does *not* even claim any space in the
	 * encyclopedia page at all.
	 * - When this evaluates to `true`, this entry will claim space in the encyclopedia.
	 * However, the name and information about the monster are not shown until the player has defeated the monster at
	 * least once.
	 *
	 * This expression is used to hide monsters that the player can only find in later chapters. For instance:
	 * - The `shouldShowUp` of FunGoblins should always evaluate to `true`, since the player can already encounter them
	 * in chapter 1.
	 * - The `shouldShowUp` of Charred Bones should only evaluate to `true` in chapter 3, since the player cannot
	 * encounter it in chapters 1 or 2.
	 */
	@BitField(id = 2)
	@ClassField(root = StateExpression::class)
	val shouldShowUp: StateExpression<Boolean>,
) {

	@Suppress("unused")
	private constructor() : this(emptyArray(), "", ConstantStateExpression())
}
