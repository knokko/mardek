package mardek.renderer.animation

import mardek.content.animation.AnimationMask
import mardek.content.animation.ColorTransform
import mardek.content.animation.SpecialAnimationNode
import mardek.content.portrait.PortraitInfo
import mardek.content.sprite.BcSprite
import mardek.content.stats.Element
import mardek.state.ingame.battle.CombatantRenderInfo
import org.joml.Matrix3x2f

class AnimationContext(
	val renderTime: Long,
	val magicScale: Int,
	parentMatrix: Matrix3x2f,
	parentColorTransform: ColorTransform?,
	val partBatch: AnimationPartBatch,
	val noMask: BcSprite,
	val combat: CombatantAnimationContext?,
	val portrait: PortraitInfo?,
	val portraitExpression: String? = null,
) {
	val stack = mutableListOf(TransformStackEntry(
		parentMatrix, parentColorTransform, null, combat?.rootSkin, null
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
)
