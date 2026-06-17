package mardek.renderer.animation

import com.github.knokko.bitser.ReferenceLazyBits
import com.github.knokko.boiler.utilities.ColorPacker.addColors
import com.github.knokko.boiler.utilities.ColorPacker.multiplyColors
import com.github.knokko.boiler.utilities.ColorPacker.rgba
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
import mardek.state.ingame.battle.AnimationEmitterState
import mardek.state.ingame.battle.CombatantRenderInfo
import mardek.state.ingame.battle.CombatantRenderPosition
import mardek.state.util.Rectangle
import org.joml.Matrix3x2f
import org.joml.Vector2f
import java.util.Locale
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random
import kotlin.random.nextInt


private fun noMaskSprite(context: AnimationContext) = AnimationSprite(
	-123, context.noMask, 0f, 0f
)

internal fun renderPortraitAnimation(animation: SkinnedAnimation, context: AnimationContext) {
	val frames = animation.skins[context.portrait!!.rootSkin]!!
	for (frame in frames.get()) renderAnimationFrame(frame, context)
}

internal fun renderBattleBackgroundAnimation(nodes: Array<AnimationNode>, context: AnimationContext) {
	for (node in nodes) renderAnimationNode(node, context)
}

internal fun renderCutsceneAnimation(frames: ReferenceLazyBits<AnimationFrames>, context: AnimationContext) {
	renderAnimationNode(AnimationNode(
		depth = 1,
		animation = SkinnedAnimation(-12345, hashMapOf(Pair("", frames))),
		sprite = null,
		matrix = null,
		color = null,
		selectSkin = null,
		special = null,
		mask = AnimationMask(emptyArray()),
		particleEmitters = emptyArray(),
	), context)
}

internal fun renderCombatantAnimation(
	animation: AnimationFrames, earlyFlat: Array<AnimationNode>, lateFlat: Array<AnimationNode>,
	relativeTime: Long, context: AnimationContext
) {
	context.combat!!.renderInfo.castingParticlePositions.clear()

	for (node in earlyFlat) renderAnimationNode(node, context)

	var remainingTime = relativeTime
	for (frame in animation) {
		remainingTime -= frame.duration.inWholeNanoseconds
		if (remainingTime <= 0L) {
			renderAnimationFrame(frame, context)
			break
		}
	}

	if (remainingTime > 0L) throw Error()

	for (node in lateFlat) renderAnimationNode(node, context)
}

private fun renderAnimationFrame(frame: AnimationFrame, context: AnimationContext) {
	for (node in frame) {
		renderAnimationNode(node, context)
	}
}

internal fun toJOMLMatrix(raw: AnimationMatrix) = Matrix3x2f(
	raw.scaleX, raw.rotateSkew0,
	raw.rotateSkew1, raw.scaleY,
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

	if (special == SpecialAnimationNode.ElementalBash) return

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
		val leafMatrix = Matrix3x2f(globalMatrix)
		leafMatrix.translate(0f, sprite.offsetY)
		if (special == SpecialAnimationNode.MorphingShadow) {
			val animationProgress = context.stack.last().animationProgress
			val shadowScale = 0.75f * (1f - 0.05f * sin(animationProgress * 2f * PI).toFloat())
			leafMatrix.translate(0f, 0.3f * sprite.offsetY)
			leafMatrix.scale(shadowScale)
		}
		leafMatrix.translate(sprite.offsetX, 0f)

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

	val (animation, skinAlpha) = chooseSkin(node, special, context)
	if (animation != null && skinAlpha > 0f) {
		var deltaTime = (context.renderTime - context.referenceTime) % animation.duration.inWholeNanoseconds
		val animationProgress = deltaTime.toFloat() / animation.duration.inWholeNanoseconds
		for (frame in animation.frames) {
			deltaTime -= frame.duration.inWholeNanoseconds
			if (deltaTime < 0L) {
				if (special == SpecialAnimationNode.OnTurnCursor || special == SpecialAnimationNode.TargetingCursor) {
					colorTransform = node.color
				}

				if (skinAlpha < 1f) {
					val alphaColor = rgba(1f, 1f, 1f, skinAlpha)
					if (colorTransform != null) {
						val newMultiplyColor = multiplyColors(colorTransform.multiplyColor, alphaColor)
						colorTransform = ColorTransform(
							colorTransform.addColor,
							newMultiplyColor,
							colorTransform.subtractColor,
						)
					} else {
						colorTransform = ColorTransform(addColor = 0, multiplyColor = alphaColor, subtractColor = 0)
					}
				}

				context.stack.add(TransformStackEntry(
					globalMatrix, colorTransform,
					special, node.selectSkin ?: context.stack.last().skin,
					mask, maskMatrix, animationProgress
				))
				renderAnimationFrame(frame, context)
				context.stack.removeLast()
				break
			}
		}
	}

	for (emitter in node.particleEmitters) {
		val renderInfo = context.combat?.renderInfo ?: continue
		val state = renderInfo.animationParticles.computeIfAbsent(
			emitter, ::AnimationEmitterState
		)

		val position = globalMatrix.transformPosition(Vector2f(0f, 0f))
		state.positions.add(CombatantRenderPosition(position.x, position.y))
	}
}

private fun chooseSkin(
	node: AnimationNode, special: SpecialAnimationNode?, context: AnimationContext
): Pair<AnimationFrames?, Float> {
	val skinned = node.animation ?: return Pair(null, 0f)

	fun choose(skin: String?) = Pair(skinned.skins[skin]?.get(), 1f)

	if (special == SpecialAnimationNode.Weapon) {
		val weaponName = context.combat?.weaponName ?: return Pair(null, 0f)
		return choose(weaponName.lowercase(Locale.ROOT))
	}

	if (special == SpecialAnimationNode.Shield) {
		val shieldName = context.combat?.shieldName ?: return Pair(null, 0f)
		return choose(shieldName.lowercase(Locale.ROOT))
	}

	if (special == SpecialAnimationNode.RandomLightningEffect) {
		val alpha = 1f - 4.5f * 0.001f * 0.001f * 0.001f * (context.renderTime - context.lightning.lastFrameChangeAt)
		if (alpha <= 0f || context.lightning.currentFrame == 1) {
			val nanoSecondsSinceLastFrame = context.renderTime - context.lightning.lastRenderedAt
			val secondsSinceLastFrame = 0.001 * 0.001 * 0.001 * nanoSecondsSinceLastFrame

			// secondsSinceLastFrame == 1.0 -> chance = 0.67 -> 1 - chance = 0.33
			// secondsSinceLastFrame == 0.5 -> 1 - chance = sqrt(0.33) = 0.57 -> chance = 0.43
			// secondsSinceLastFrame == 2.0 -> 1 - chance = 0.33 * 0.33 = 0.11 -> chance = 0.89
			val chance = 1.0 - 0.33.pow(secondsSinceLastFrame)
			if (chance > Random.nextDouble()) {
				context.lightning.currentFrame = Random.nextInt(2 .. skinned.skins.size)
				context.lightning.lastFrameChangeAt = context.renderTime
			}
			return Pair(null, 0f)
		} else return Pair(skinned.skins[context.lightning.currentFrame.toString()]?.get(), alpha)
	}

	if (special == SpecialAnimationNode.CurrentChapter) {
		return choose(context.currentChapter.toString())
	}

	if (special == SpecialAnimationNode.PortraitExpressions) {
		return choose(context.portraitExpression!!.lowercase(Locale.ROOT))
	}

	if (special == SpecialAnimationNode.PortraitMouthExpressions) {
		var currentCharacterIndex = context.shownDialogueCharacters.toInt()
		val defaultValue = choose(context.portraitExpression!!.lowercase(Locale.ROOT))
		if (currentCharacterIndex >= 0 && currentCharacterIndex < context.dialogueLine.length) {
			currentCharacterIndex = 4 * (currentCharacterIndex / 4)
			val characterToFrameMapping = intArrayOf(
				2, 3, 4, 6, 2, 5,
				4, 2, 2, 4, 2, 6,
				3, 2, 7, 3, 2, 4,
				4, 4, 7, 5, 3, 4,
				2, 4,
			)
			val currentCharacter = context.dialogueLine[currentCharacterIndex].lowercaseChar()
			if (currentCharacter in 'a'..'z') {
				return choose((characterToFrameMapping[currentCharacter - 'a']).toString())
			}
		}
		return defaultValue
	}

	if (special == SpecialAnimationNode.PortraitFace) {
		return choose(context.portrait!!.faceSkin)
	}

	if (special == SpecialAnimationNode.PortraitHair) {
		return choose(context.portrait!!.hairSkin)
	}

	if (special == SpecialAnimationNode.PortraitEye) {
		return choose(context.portrait!!.eyeSkin)
	}

	if (special == SpecialAnimationNode.PortraitEyeBrow) {
		return choose(context.portrait!!.eyeBrowSkin)
	}

	if (special == SpecialAnimationNode.PortraitMouth) {
		return choose(context.portrait!!.mouthSkin)
	}

	if (special == SpecialAnimationNode.PortraitEthnicity) {
		return choose(context.portrait!!.ethnicitySkin)
	}

	if (special == SpecialAnimationNode.PortraitArmor) {
		return choose(context.portrait!!.armorSkin)
	}

	if (special == SpecialAnimationNode.PortraitRobe) {
		return choose(context.portrait!!.robeSkin)
	}

	var animation = skinned.skins[""]?.get()
	val expectedSkin = node.selectSkin ?: context.stack.last().skin
	if (expectedSkin != null) animation = skinned.skins[expectedSkin]?.get()

	if (animation == null) animation = skinned.skins["d"]?.get()
	if (animation == null) animation = skinned.skins[""]?.get()
	if (animation == null) animation = skinned.skins.values.first().get()
	return Pair(animation, 1f)
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
