package mardek.renderer.animation

import mardek.content.animation.AnimationMask
import mardek.content.animation.ColorTransform
import mardek.content.animation.SpecialAnimationNode
import mardek.content.portrait.PortraitInfo
import mardek.content.sprite.BcSprite
import mardek.content.stats.Element
import mardek.state.ingame.battle.CombatantRenderInfo
import mardek.state.util.Rectangle
import org.joml.Matrix3x2f
import kotlin.time.Duration

private val defaultReferenceTime = System.nanoTime()

class AnimationContext(
	val renderRegion: Rectangle,
	val renderTime: Long,
	val magicScale: Int,
	parentMatrix: Matrix3x2f,
	parentColorTransform: ColorTransform?,
	val partBatch: AnimationPartBatch,
	val noMask: BcSprite,
	val combat: CombatantAnimationContext?,
	val portrait: PortraitInfo?,
	val currentChapter: Int,
	val portraitExpression: String? = null,
	val dialogueLine: String = "",
	val shownDialogueCharacters: Float = 0f,
	val referenceTime: Long = defaultReferenceTime,
	val lightning: LightningInfo = LightningInfo(),
	animationDuration: Duration,
) {
	val stack = mutableListOf(TransformStackEntry(
		parentMatrix, parentColorTransform,
		null, combat?.rootSkin,
		null, Matrix3x2f(),
		if (animationDuration == Duration.ZERO) 0f else (
				(renderTime - referenceTime) % animationDuration.inWholeNanoseconds
		).toFloat() / animationDuration.inWholeNanoseconds,
	))
}

class CombatantAnimationContext(
	val isSelectedTarget: Boolean,
	val isSelectingMove: Boolean,
	val meleeElement: Element?,
	val magicElement: Element?,
	val isMoving: Boolean,
	val rootSkin: String?,
	val weaponName: String?,
	val shieldName: String?,
	val renderInfo: CombatantRenderInfo,
)

class TransformStackEntry(
	val matrix: Matrix3x2f,
	val colors: ColorTransform?,
	val special: SpecialAnimationNode?,
	val skin: String?,
	val mask: AnimationMask?,
	val maskMatrix: Matrix3x2f,

	/**
	 * Should be 0f at the start of the animation, and nearly 1f at the end of the animation
	 */
	val animationProgress: Float,
)

class LightningInfo {
	var currentFrame = 1
	var lastFrameChangeAt = System.nanoTime()
	var lastRenderedAt = System.nanoTime()
}
