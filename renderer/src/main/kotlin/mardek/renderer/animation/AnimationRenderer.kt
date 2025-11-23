package mardek.renderer.animation

import com.github.knokko.boiler.utilities.ColorPacker.addColors
import com.github.knokko.boiler.utilities.ColorPacker.multiplyColors
import mardek.content.animation.AnimationFrames
import mardek.content.animation.AnimationNode
import mardek.content.animation.AnimationSprite
import mardek.content.animation.AnimationFrame
import mardek.content.animation.AnimationMask
import mardek.content.animation.SpecialAnimationNode
import mardek.content.animation.AnimationMatrix
import mardek.content.animation.ColorTransform
import mardek.content.animation.SkinnedAnimation
import mardek.content.sprite.BcSprite
import mardek.state.ingame.battle.CombatantRenderInfo
import mardek.state.ingame.battle.CombatantRenderPosition
import mardek.state.util.Rectangle
import org.joml.Matrix3x2f
import org.joml.Vector2f
import java.util.Locale


private fun noMaskSprite(context: AnimationContext) = AnimationSprite(
	-123, context.noMask, 0f, 0f
)

internal fun renderPortraitAnimation(animation: SkinnedAnimation, context: AnimationContext) {
	val frames = animation.skins[context.portrait!!.rootSkin]!!
	for (frame in frames) renderAnimationFrame(frame, context)
}

internal fun renderBattleBackgroundAnimation(nodes: Array<AnimationNode>, context: AnimationContext) {
	for (node in nodes) renderAnimationNode(node, context)
}

internal fun renderCutsceneAnimation(frames: AnimationFrames, context: AnimationContext) {
	renderAnimationNode(AnimationNode(
		depth = 1,
		animation = SkinnedAnimation(-12345, hashMapOf(Pair("", frames))),
		sprite = null,
		matrix = null,
		color = null,
		selectSkin = null,
		special = null,
		mask = AnimationMask(emptyArray()),
	), context)
}

internal fun renderCombatantAnimation(
	animation: AnimationFrames, flat: Array<AnimationNode>,
	relativeTime: Long, context: AnimationContext
) {
	context.combat!!.renderInfo.castingParticlePositions.clear()
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

internal fun toJOMLMatrix(raw: AnimationMatrix) = Matrix3x2f(
	raw.getScaleX(), raw.rotateSkew0,
	raw.rotateSkew1, raw.getScaleY(),
	raw.translateX, raw.translateY
)

private fun renderAnimationNode(node: AnimationNode, context: AnimationContext) {
	val top = context.stack.last()
	val special = node.special ?: top.special
	val localMatrix = toJOMLMatrix(node.matrix ?: AnimationMatrix.DEFAULT)
	val globalMatrix = top.matrix.mul(localMatrix, Matrix3x2f())
	val rawNodePosition = globalMatrix.transformPosition(Vector2f())
	val nodePosition = CombatantRenderPosition(rawNodePosition.x, rawNodePosition.y)

	val combat = context.combat

	fun trackUnmovablePoint(
		specialToTrack: SpecialAnimationNode,
		get: (CombatantRenderInfo) -> CombatantRenderPosition,
		set: (CombatantRenderInfo, CombatantRenderPosition) -> Unit,
	): Boolean {
		return if (special == specialToTrack) {
			if (get(combat!!.renderInfo) == CombatantRenderPosition.DUMMY || !combat.isMoving) {
				set(combat.renderInfo, nodePosition)
			}
			true
		} else false
	}

	if (trackUnmovablePoint(SpecialAnimationNode.HitPoint, { it.hitPoint }) {
		info, position -> info.hitPoint = position
	}) return
	if (trackUnmovablePoint(SpecialAnimationNode.StrikePoint, { it.strikePoint }) {
		info, position -> info.strikePoint = position
	}) return

	if (special == SpecialAnimationNode.StatusEffectPoint) {
		combat!!.renderInfo.statusEffectPoint = nodePosition
		return
	}

	trackUnmovablePoint(SpecialAnimationNode.BreathSource, { it.idleBreathSource }) {
			info, position -> info.idleBreathSource = position
	}
	if (special == SpecialAnimationNode.BreathSource) {
		combat!!.renderInfo.activeBreathSource = nodePosition
		return
	}

	if (trackUnmovablePoint(SpecialAnimationNode.Core, { it.core }) {
		info, position -> info.core = position
	}) return

	if (trackUnmovablePoint(SpecialAnimationNode.BreathDistance, { it.breathDistance }) {
			info, position -> info.breathDistance = position
	}) return

	// This one is an artifact from Deliverance (the predecessor of MARDEK), which we should ignore
	if (special == SpecialAnimationNode.Exclaim) return

	// This weird check is needed to hide a weird crystal pointer above monster heads
	if (special == null && node.sprite?.defineShapeFlashID == 1884) return

	if (special == SpecialAnimationNode.TargetingCursor && combat?.isSelectedTarget != true) return
	if (special == SpecialAnimationNode.OnTurnCursor && combat?.isSelectingMove != true) return

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

	if (special == SpecialAnimationNode.ElementalCastingSparkle) {
		combat!!.renderInfo.castingParticlePositions.add(nodePosition)
		return
	}

	val (mask, maskMatrix) = if (top.mask == null || top.mask.frames.size < node.mask.frames.size) {
		Pair(node.mask, top.matrix)
	} else Pair(top.mask, top.maskMatrix)

	if (special == SpecialAnimationNode.ElementalCastingBackground) {
		val backgroundSprite = combat?.magicElement?.spellCastBackground ?: return
		sprite = AnimationSprite(2223, backgroundSprite, 0f, 0f)
	}

	var colorTransform = mergeColorTransforms(node.color, top.colors)

	if (sprite != null) {
		val leafMatrix = globalMatrix.translate(sprite.offsetX, sprite.offsetY, Matrix3x2f())
		var maskSprite: AnimationSprite? = null
		var leafMaskMatrix: Matrix3x2f? = null

		if (mask.frames.isNotEmpty()) {
			var deltaTime = (context.renderTime - context.referenceTime) % mask.duration.inWholeNanoseconds
			for (frame in mask) {
				deltaTime -= frame.duration.inWholeNanoseconds
				if (deltaTime < 0L) {
					maskSprite = frame.sprite
					leafMaskMatrix = maskMatrix.mul(toJOMLMatrix(frame.matrix), Matrix3x2f())
					leafMaskMatrix.translate(
						maskSprite.offsetX,
						maskSprite.offsetY,
					)
					leafMaskMatrix.scale(
						maskSprite.image.width.toFloat() / context.magicScale,
						maskSprite.image.height.toFloat() / context.magicScale,
					)
					break
				}
			}
			if (maskSprite == null) throw Error()
		} else {
			maskSprite = noMaskSprite(context)
			leafMaskMatrix = null
		}

		leafMatrix.scale(
			sprite.image.width.toFloat() / context.magicScale,
			sprite.image.height.toFloat() / context.magicScale,
		)

		renderTransformedImage(
			leafMatrix, sprite.image, leafMaskMatrix, maskSprite.image,
			context.renderRegion, colorTransform, context.partBatch,
		)
	}

	val animation = chooseSkin(node, special, context)
	if (animation != null) {
		var deltaTime = (context.renderTime - context.referenceTime) % animation.duration.inWholeNanoseconds
		for (frame in animation.frames) {
			deltaTime -= frame.duration.inWholeNanoseconds
			if (deltaTime < 0L) {
				if (special == SpecialAnimationNode.OnTurnCursor || special == SpecialAnimationNode.TargetingCursor) {
					colorTransform = node.color
				}

				context.stack.add(TransformStackEntry(
					globalMatrix, colorTransform,
					special, node.selectSkin ?: context.stack.last().skin,
					mask, maskMatrix,
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
		return skinned.skins[weaponName.lowercase(Locale.ROOT)]
	}

	if (special == SpecialAnimationNode.Shield) {
		val shieldName = context.combat?.shieldName ?: return null
		return skinned.skins[shieldName.lowercase(Locale.ROOT)]
	}

	if (special == SpecialAnimationNode.PortraitExpressions) {
		return skinned.skins[context.portraitExpression!!]
	}

	if (special == SpecialAnimationNode.PortraitFace) {
		return skinned.skins[context.portrait!!.faceSkin]
	}

	if (special == SpecialAnimationNode.PortraitHair) {
		return skinned.skins[context.portrait!!.hairSkin]
	}

	if (special == SpecialAnimationNode.PortraitEye) {
		return skinned.skins[context.portrait!!.eyeSkin]
	}

	if (special == SpecialAnimationNode.PortraitEyeBrow) {
		return skinned.skins[context.portrait!!.eyeBrowSkin]
	}

	if (special == SpecialAnimationNode.PortraitMouth) {
		return skinned.skins[context.portrait!!.mouthSkin]
	}

	if (special == SpecialAnimationNode.PortraitEthnicity) {
		return skinned.skins[context.portrait!!.ethnicitySkin]
	}

	if (special == SpecialAnimationNode.PortraitArmor) {
		return skinned.skins[context.portrait!!.armorSkin]
	}

	if (special == SpecialAnimationNode.PortraitRobe) {
		return skinned.skins[context.portrait!!.robeSkin]
	}

	var animation = skinned.skins[""]
	val expectedSkin = node.selectSkin ?: context.stack.last().skin
	if (expectedSkin != null) animation = skinned.skins[expectedSkin]

	if (animation == null) animation = skinned.skins["d"]
	if (animation == null) animation = skinned.skins[""]
	if (animation == null) animation = skinned.skins.values.first()
	return animation
}

private fun renderTransformedImage(
	mainMatrix: Matrix3x2f, sprite: BcSprite,
	maskMatrix: Matrix3x2f?, maskSprite: BcSprite, region: Rectangle,
	colors: ColorTransform?, batch: AnimationPartBatch,
) {
	val ndcMatrix = Matrix3x2f().translate(-1f, -1f).scale(2f / batch.width, 2f / batch.height)
	if (maskMatrix != null) ndcMatrix.mul(maskMatrix, maskMatrix)
	ndcMatrix.mul(mainMatrix, mainMatrix)

	val rawCorners = arrayOf(
		Pair(0f, 1f), Pair(1f, 1f),
		Pair(1f, 0f), Pair(0f, 0f)
	)
	val corners = rawCorners.map { rawCorner ->
		mainMatrix.transformPosition(Vector2f(rawCorner.first, rawCorner.second))
	}
	val maskCorners = if (maskMatrix != null) rawCorners.map { rawCorner ->
		maskMatrix.transformPosition(Vector2f(rawCorner.first, rawCorner.second))
	} else listOf(
		Vector2f(-1f, 1f), Vector2f(1f, 1f),
		Vector2f(1f, -1f), Vector2f(-1f, -1f),
	)

	batch.transformed(
		corners[0].x, corners[0].y,
		corners[1].x, corners[1].y,
		corners[2].x, corners[2].y,
		corners[3].x, corners[3].y,
		maskCorners[0].x, maskCorners[0].y,
		maskCorners[1].x, maskCorners[1].y,
		maskCorners[2].x, maskCorners[2].y,
		maskCorners[3].x, maskCorners[3].y,
		sprite.index, maskSprite.index, region,
		colors?.addColor ?: 0,
		colors?.multiplyColor ?: -1,
		colors?.subtractColor ?: 0,
	)
}

private fun mergeColorTransforms(base: ColorTransform?, top: ColorTransform?): ColorTransform? {
	if (base == null) return top
	if (top == null) return base

	return ColorTransform(
		addColor = addColors(multiplyColors(base.addColor, top.multiplyColor), top.addColor),
		multiplyColor = multiplyColors(base.multiplyColor, top.multiplyColor),
		subtractColor = addColors(multiplyColors(base.subtractColor, top.multiplyColor), top.subtractColor),
	)
}
