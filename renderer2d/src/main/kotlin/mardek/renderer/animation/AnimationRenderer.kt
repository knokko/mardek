package mardek.renderer.animation

import com.github.knokko.boiler.utilities.ColorPacker.alpha
import com.github.knokko.boiler.utilities.ColorPacker.blue
import com.github.knokko.boiler.utilities.ColorPacker.green
import com.github.knokko.boiler.utilities.ColorPacker.multiplyColors
import com.github.knokko.boiler.utilities.ColorPacker.normalize
import com.github.knokko.boiler.utilities.ColorPacker.red
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import mardek.content.animation.AnimationFrames
import mardek.content.animation.AnimationNode
import mardek.content.animation.AnimationSprite
import mardek.content.animation.AnimationFrame
import mardek.content.animation.SpecialAnimationNode
import mardek.content.animation.AnimationMatrix
import mardek.content.animation.ColorTransform
import mardek.content.sprite.BcSprite
import org.joml.Matrix3x2f
import org.joml.Vector2f

private val referenceTime = System.nanoTime()

internal fun renderBattleBackgroundAnimation(nodes: Array<AnimationNode>, context: AnimationContext) {
	for (node in nodes) renderAnimationNode(node, context)
}

internal fun renderCombatantAnimation(
	animation: AnimationFrames, flat: Array<AnimationNode>,
	relativeTime: Long, context: AnimationContext
) {
	var remainingTime = relativeTime
	for (frame in animation) {
		remainingTime -= frame.duration.inWholeNanoseconds
		if (remainingTime <= 0L) {
			renderAnimationFrame(frame, context)
			break
		}
	}

	if (remainingTime > 0L) throw Error()

	for (node in flat) renderAnimationNode(node, context)
}

private fun renderAnimationFrame(frame: AnimationFrame, context: AnimationContext) {
	for (node in frame) {
		renderAnimationNode(node, context)
	}
}

private fun renderAnimationNode(node: AnimationNode, context: AnimationContext) {
	val special = node.special ?: context.stack.last().special

	val combat = context.combat
	if (special == SpecialAnimationNode.HitPoint) {
		// TODO mark hit point
		return
	}
	if (special == SpecialAnimationNode.StrikePoint) {
		// TODO mark strike point
		return
	}
	if (special == SpecialAnimationNode.StatusEffectPoint) {
		// TODO mark status effect point
		return
	}
	if (special == SpecialAnimationNode.Core) {
		// TODO mark core
		return
	}

	// I have no clue what this one is for
	if (special == SpecialAnimationNode.Exclaim) return

	// This weird check is needed to hide a weird crystal pointer above monster heads
	if (special == null && node.sprite?.defineShapeFlashID == 1884) return

	if (special == SpecialAnimationNode.TargetingCursor && combat?.isSelectedTarget != true) return
	if (special == SpecialAnimationNode.OnTurnCursor && combat?.isSelectingMove != true) return

	val matrix = node.matrix ?: AnimationMatrix.DEFAULT
	val (scaleX, scaleY) = if (matrix.hasScale) Pair(matrix.scaleX, matrix.scaleY) else Pair(
		1f,
		1f
	)

	var sprite = node.sprite

	if (special == SpecialAnimationNode.ElementalSwing) {
		val swingSprite = combat?.meleeElement?.swingEffect ?: return
		sprite = AnimationSprite(2295, swingSprite, -15.35f, -14.5f)
	}

	if (special == SpecialAnimationNode.ElementalCastingCircle) {
		if (combat?.magicElement == null) return
		sprite = AnimationSprite(
			199, combat.magicElement.thinSprite, -30f, -30f
		)
	}
	// TODO Casting sparkles

	if (special == SpecialAnimationNode.ElementalCastingBackground) {
		val backgroundSprite = combat?.magicElement?.spellCastBackground ?: return
		sprite = AnimationSprite(2223, backgroundSprite, 0f, 0f)
	}

	val localMatrix = Matrix3x2f(
		scaleX, matrix.rotateSkew0,
		matrix.rotateSkew1, scaleY,
		matrix.translateX, matrix.translateY
	)
	val top = context.stack.last()
	val colorTransform = mergeColorTransforms(node.color, top.colors)

	if (sprite != null) {
		val leafMatrix = localMatrix.translate(sprite.offsetX, sprite.offsetY, Matrix3x2f())
		top.matrix.mul(leafMatrix, leafMatrix)

		val maskSprite = if (node.mask != null) node.mask!!.sprite.image else context.noMask
		renderTransformedImage(
			leafMatrix, sprite.image.width.toFloat() / context.magicScale,
			sprite.image.height.toFloat() / context.magicScale,
			sprite.image, maskSprite, colorTransform, context.partBatch,
		)
	}

	val animation = chooseSkin(node, special, context)
	if (animation != null) {
		var deltaTime = (context.renderTime - referenceTime) % animation.duration.inWholeNanoseconds
		for (frame in animation.frames) {
			deltaTime -= frame.duration.inWholeNanoseconds
			if (deltaTime < 0L) {
				context.stack.add(TransformStackEntry(
					top.matrix.mul(localMatrix, localMatrix),
					colorTransform, special, node.selectSkin ?: context.stack.last().skin,
				))
				renderAnimationFrame(frame, context)
				context.stack.removeLast()
				break
			}
		}
	}
}

private fun chooseSkin(
	node: AnimationNode, special: SpecialAnimationNode?, context: AnimationContext
): AnimationFrames? {
	val skinned = node.animation ?: return null

	if (special == SpecialAnimationNode.Weapon) {
		val weaponName = context.combat?.weaponName ?: return null
		return skinned.skins[weaponName]
	}

	if (special == SpecialAnimationNode.Shield) {
		val shieldName = context.combat?.shieldName ?: return null
		return skinned.skins[shieldName]
	}

	var animation = skinned.skins[""]
	val expectedSkin = node.selectSkin ?: context.stack.last().skin
	if (expectedSkin != null) animation = skinned.skins[expectedSkin]

	if (animation == null) animation = skinned.skins["D"]
	if (animation == null) animation = skinned.skins[""]
	if (animation == null) animation = skinned.skins.values.first()
	return animation
}

private fun renderTransformedImage(
	jomlMatrix: Matrix3x2f, scaleX: Float, scaleY: Float,
	sprite: BcSprite, maskSprite: BcSprite, colors: ColorTransform?, batch: AnimationPartBatch,
) {
	val corners = arrayOf(
		Pair(0f, 1f), Pair(1f, 1f),
		Pair(1f, 0f), Pair(0f, 0f)
	).map { rawCorner ->
		jomlMatrix.transformPosition(Vector2f(
			rawCorner.first * scaleX,
			rawCorner.second * scaleY,
		))
	}

	batch.transformed(
		corners[0].x, corners[0].y,
		corners[1].x, corners[1].y,
		corners[2].x, corners[2].y,
		corners[3].x, corners[3].y,
		sprite.index,
		maskSprite.index,
		colors?.addColor ?: 0,
		colors?.multiplyColor ?: -1,
	)
}

private fun mergeColorTransforms(base: ColorTransform?, top: ColorTransform?): ColorTransform? {
	if (base == null) return top
	if (top == null) return base

	var addColor = multiplyColors(base.addColor, top.multiplyColor)
	addColor = rgba(
		normalize(red(addColor)) + normalize(red(top.addColor)),
		normalize(green(addColor)) + normalize(green(top.addColor)),
		normalize(blue(addColor)) + normalize(blue(top.addColor)),
		normalize(alpha(addColor)) + normalize(alpha(top.addColor)),
	)
	return ColorTransform(addColor = addColor, multiplyColor = multiplyColors(base.multiplyColor, top.multiplyColor))
}
