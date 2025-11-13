package mardek.content.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.BITSER
import mardek.content.skill.ActiveSkill

/**
 * Some (mostly boss) enemies will perform a counter-attack whenever they are attacked. This class models such a
 * counter-attack. Every monster has a (usually empty) list of melee counter attacks and ranged counter attacks.
 */
@BitStruct(backwardCompatible = true)
class CounterAttack(

	/**
	 * The attack that the enemy will perform when that enemy is attacked.
	 */
	@BitField(id = 0)
	@ReferenceField(stable = false, label = "skills")
	val action: ActiveSkill,

	/**
	 * The chance (in percentages) that the monster will perform a counter-attack upon being attacked
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 100)
	val chance: Int,

	/**
	 * This field decides how the monster will choose the target for its counter-attack:
	 * - `AnyEnemy`: the monster will attack whoever attacked it first
	 * - `AllEnemies`: the monster will react with an area-of-effect skill that targets all players
	 * - `Self`: the monster will react by casing a skill on itself (typically a positive skill)
	 * - `AllAllies`: the monster will react by casting a (typically positive) area-of-effect skill on all monsters
	 */
	@BitField(id = 2)
	val target: StrategyTarget,
) {

	@Suppress("unused")
	private constructor() : this(ActiveSkill(), 0, StrategyTarget.Self)

	override fun toString() = "Counter($chance% $action at $target)"

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)
}
