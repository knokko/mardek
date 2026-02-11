package mardek.content.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.ReferenceFieldTarget
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.content.BITSER
import mardek.content.action.ActionNode
import mardek.content.action.ActionSequence
import mardek.content.area.Direction
import mardek.content.portrait.PortraitInfo
import mardek.content.sprite.DirectionalSprites
import mardek.content.sprite.ObjectSprites
import mardek.content.stats.Element
import java.util.UUID

/**
 * Represents a non-player character (or object) inside an area. Some area characters can walk (randomly),
 * whereas others never move.
 *
 * Player can *not* move through `AreaCharacter`s, and can interact with *some* of them, but not all.
 */
@BitStruct(backwardCompatible = true)
class AreaCharacter(

	/**
	 * The name of the character (as imported from Flash). It is currently only used in dialogues.
	 */
	@BitField(id = 0)
	val name: String,

	/**
	 * When non-null, this character has a pair of sprites for each `Direction` in which the character may look/walk.
	 *
	 * Most characters have this.
	 *
	 * Exactly 1 of `directionalSprites` and `fixedSprites` must be non-null
	 */
	@BitField(id = 1, optional = true)
	@ReferenceField(stable = false, label = "character sprites")
	val directionalSprites: DirectionalSprites?,

	/**
	 * When non-null, this character always uses the same sprite or animation, regardless of the `Direction` in which
	 * the character is looking or walking.
	 *
	 * This is typically used for boss characters that only have directions for 1 sprite.
	 *
	 * Exactly 1 of `directionalSprites` and `fixedSprites` must be non-null
	 */
	@BitField(id = 2, optional = true)
	@ReferenceField(stable = false, label = "object sprites")
	val fixedSprites: ObjectSprites?,

	/**
	 * The X-coordinate of the tile where this character will start/spawn when the player enters the area.
	 */
	@BitField(id = 3)
	@IntegerField(expectUniform = false)
	val startX: Int,

	/**
	 * The Y-coordinate of the tile where this character will start/spawn when the player enters the area.
	 */
	@BitField(id = 4)
	@IntegerField(expectUniform = false)
	val startY: Int,

	/**
	 * The direction that this character will face when the player enters the area. `Down` is used as a 'default' for
	 * objects that don't really have a direction.
	 */
	@BitField(id = 5)
	val startDirection: Direction,

	/**
	 * When walkSpeed is positive, the character randomly moves around. When it's -1, the character stays at the same
	 * position, but still shows the walking animation. When it's -2, the character doesn't move, nor does it show any
	 * walking animation.
	 *
	 * When `fixedSprites != null`, then `walkSpeed` must be -2.
	 */
	@BitField(id = 6)
	@IntegerField(expectUniform = false, minValue = -2)
	val walkSpeed: Int,

	/**
	 * The element of this character, which is only used in dialogues (for the element background icon). This is
	 * only needed for characters that have dialogue.
	 */
	@BitField(id = 7, optional = true)
	@ReferenceField(stable = false, label = "elements")
	val element: Element?,

	/**
	 * The portrait of this character, which is only used in dialogues. Note that portraits are truly optional: some
	 * characters simply don't have one, even if they do have dialogue.
	 */
	@BitField(id = 8, optional = true)
	@ReferenceField(stable = false, label = "portrait info")
	val portrait: PortraitInfo?,

	/**
	 * The action that should be activated when the player interacts with this character, or `null` when
	 * `sharedActionSequence` should be used instead (or `null` when it's not yet properly imported).
	 */
	@BitField(id = 9, optional = true)
	@ClassField(root = ActionNode::class)
	@ReferenceFieldTarget(label = "action nodes")
	val ownActions: ActionNode?,

	/**
	 * The action sequence that should be activated when the player interacts with this character, or `null` when
	 * `ownActions` should be used instead (or `null` when it's not yet properly imported).
	 *
	 * This must be a reference to either an area action sequence, or to a global action sequence.
	 */
	@BitField(id = 10, optional = true)
	@ReferenceField(stable = false, label = "action sequences")
	val sharedActionSequence: ActionSequence?,

	/**
	 * When `encyclopediaPerson` is non-null and the player interacts with this character, the character named
	 * `encyclopediaPerson` should be added to the encyclopedia (unless already present).
	 */
	@BitField(id = 11, optional = true)
	val encyclopediaPerson: String?,

	/**
	 * The unique ID of this character, which is used for (de)serialization
	 */
	@BitField(id = 12)
	@StableReferenceFieldId
	val id: UUID,
) {

	constructor() : this(
		"", DirectionalSprites(), null, 0, 0, Direction.Down,
		0, null, null, null, null,
		null, UUID.randomUUID(),
	)

	init {
		if (ownActions != null && sharedActionSequence != null) {
			throw IllegalArgumentException("At most 1 of ownActions and sharedActionSequence can be non-null")
		}
		if ((directionalSprites == null) == (fixedSprites == null)) {
			throw IllegalArgumentException("Exactly 1 of directionSprites and fixedSprites must be non-null")
		}
		if (fixedSprites != null && walkSpeed != -2) {
			throw IllegalArgumentException("walkSpeed must be -2 when fixedSprites != null")
		}
	}

	override fun toString() = "Character($name, ${directionalSprites?.name ?: fixedSprites?.flashName}, " +
			"x=$startX, y=$startY, direction=$startDirection, " +
			"walkSpeed=$walkSpeed, element=$element, person=$encyclopediaPerson)"

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = id.hashCode()
}
