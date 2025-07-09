package mardek.content.animation

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceField

/**
 * Represents a node in an animation graph, which is typically a PlaceObject2 tag in Flash. An animation node will
 * always have a transformation matrix (possibly the identity matrix) and may or may not have a color transformation.
 *
 * The *content* of the node can be a sprite, a light source, or a child animation. This representation allows nodes
 * to have e.g. both a light source and a child animation, but nodes imported from Flash never will.
 */
@BitStruct(backwardCompatible = true)
class AnimationNode(
	/**
	 * Only used during importing
	 */
	val depth: Int,

	/**
	 * The child animation of this node, if the child is a DefineSpriteTag.
	 */
	@BitField(id = 0, optional = true)
	@ReferenceField(stable = false, label = "skinned animations")
	val animation: SkinnedAnimation?,

	/**
	 * The sprite of this node, if the child is a DefineShapeTag.
	 */
	@BitField(id = 1, optional = true)
	@ReferenceField(stable = false, label = "animation sprites")
	val sprite: AnimationSprite?,

	/**
	 * The transformation matrix of the node. The *content* position of this node should be transformed by the
	 * transformation matrix. When this is `null`, the identity matrix is assumed.
	 */
	@BitField(id = 2, optional = true)
	val matrix: AnimationMatrix?,

	/**
	 * Sometimes, the sprites in an animation have a color transform, which means that its sprite and the sprites of all
	 * its children should be rendered with that color transform. This is typically used by the back arms/legs to give
	 * them a darker shade, which gives the illusion of depth.
	 */
	@BitField(id = 3, optional = true)
	val color: ColorTransform?,

	@BitField(id = 4, optional = true)
	val selectSkin: String?,

	/**
	 * When not null, this animation node has special rules
	 */
	@BitField(id = 5, optional = true)
	val special: SpecialAnimationNode?,

	/**
	 * For most nodes, this will be null, which means that the node renders normally.
	 *
	 * When non-null, the node (and its children) use a grayscale image as *mask*. The alpha of any child sprite is
	 * multiplied by the corresponding pixel of the mask.
	 */
	@BitField(id = 6, optional = true)
	val mask: AnimationMask?,
) {
	@Suppress("unused")
	private constructor() : this(
		0, null, null, null,
		null, null, null, null
	)

	override fun toString(): String {
		val result = StringBuilder("AnimationNode(depth=$depth")
		if (animation != null) result.append(", animation=$animation")
		if (sprite != null) result.append(", sprite=$sprite")
		if (selectSkin != null) result.append(", skin=$selectSkin")
		result.append(")")
		return result.toString()
	}
}
