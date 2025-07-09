package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.*
import mardek.content.animation.AnimationFrames
import mardek.content.animation.ColorTransform
import mardek.content.battle.PartyLayoutPosition
import mardek.content.stats.Element
import mardek.renderer.animation.AnimationContext
import mardek.renderer.animation.AnimationPartBatch
import mardek.renderer.animation.CombatantAnimationContext
import mardek.renderer.animation.renderCombatantAnimation
import mardek.state.ingame.battle.BattleStateMachine
import mardek.state.ingame.battle.CombatantRenderPosition
import mardek.state.ingame.battle.CombatantState
import mardek.state.ingame.battle.DamageIndicatorHealth
import mardek.state.ingame.battle.DamageIndicatorMana
import mardek.state.ingame.battle.MonsterCombatantState
import org.joml.Matrix3x2f
import kotlin.math.min

private const val FRAME_LENGTH = 33_000_000L

class CombatantRenderer(
	private val context: BattleRenderContext,
	private val batch: AnimationPartBatch,
	private val combatant: CombatantState,
	private val showcase: Boolean = false,
) {
	private val state = context.battle.state
	private val flipX = if (combatant.isOnPlayerSide && !showcase) 1f else -1f
	private val effectColorTransform = mergeColorTransforms(
		mergeColorTransforms(selectedColorTransform(), damageColorTransform()),
		skipTurnTransform()
	)

	private val animations = combatant.getAnimations()
	private var relativeTime = context.renderTime - context.battle.startTime
	private var animation: AnimationFrames? = animations["idle"]

	private var coordinates = transformBattleCoordinates(
		combatant.getPosition(context.battle), flipX, context
	)

	private fun colorCombineTransform(max: Float, intensity: Float, color: Int) = ColorTransform(
		addColor = rgba(
			normalize(red(color)) * max * intensity,
			normalize(green(color)) * max * intensity,
			normalize(blue(color)) * max * intensity, 0f
		),
		multiplyColor = rgb(1f - max * intensity, 1f - max * intensity, 1f - max * intensity)
	)

	private fun selectedColorTransform(intensity: Float) = colorCombineTransform(
		0.5f, intensity, rgb(0f, 0f, 1f)
	)

	private fun selectedColorTransform(): ColorTransform? {
		if (combatant.renderInfo.lastPointedTo == 0L) return null

		val blinkTime = 500_000_000L
		val passedTime = context.renderTime - combatant.renderInfo.lastPointedTo
		if (passedTime >= blinkTime) return null

		return selectedColorTransform(1f - passedTime.toFloat() / blinkTime)
	}

	private fun skipTurnTransform(): ColorTransform? {
		val forcedTurn = combatant.renderInfo.lastForcedTurn ?: return null
		val duration = 1000_000_000L
		val passedTime = context.renderTime - forcedTurn.time
		if (passedTime >= duration) return null

		return colorCombineTransform(1f, 1f - passedTime.toFloat() / duration, forcedTurn.color)
	}

	private fun damageColorTransform(blinkColor: Int, intensity: Float) = colorCombineTransform(
		1f, intensity, blinkColor
	)

	private fun damageColorTransform(): ColorTransform? {
		val damageIndicator = combatant.renderInfo.lastDamageIndicator
		val (element, overrideColor) = when (damageIndicator) {
			is DamageIndicatorHealth -> if (damageIndicator.gainedHealth == 0) return null else Pair(
				damageIndicator.element, damageIndicator.overrideColor
			)
			is DamageIndicatorMana -> Pair(damageIndicator.element, damageIndicator.overrideColor)
			else -> return null
		}

		val blinkTime = 1000_000_000L
		val passedTime = context.renderTime - damageIndicator.time
		if (passedTime >= blinkTime) return null

		val color = if (overrideColor != 0) overrideColor
		else if (element === context.updateContext.physicalElement) rgb(250, 20, 20)
		else element.color

		return damageColorTransform(srgbToLinear(color), 1f - passedTime.toFloat() / blinkTime)
	}

	private fun mergeColorTransforms(base: ColorTransform?, top: ColorTransform?): ColorTransform? {
		if (base == null) return top
		if (top == null) return base

		val addColor = rgba(
			normalize(red(base.addColor)) * normalize(red(top.multiplyColor)) + normalize(red(top.addColor)),
			normalize(green(base.addColor)) * normalize(green(top.multiplyColor)) + normalize(green(top.addColor)),
			normalize(blue(base.addColor)) * normalize(blue(top.multiplyColor)) + normalize(blue(top.addColor)),
			normalize(alpha(base.addColor)) * normalize(alpha(top.multiplyColor)) + normalize(alpha(top.addColor)),
		)
		// TODO Use ColorPacker here
		val multipleColor = rgba(
			normalize(red(base.multiplyColor)) * normalize(red(top.multiplyColor)),
			normalize(green(base.multiplyColor)) * normalize(green(top.multiplyColor)),
			normalize(blue(base.multiplyColor)) * normalize(blue(top.multiplyColor)),
			normalize(alpha(base.multiplyColor)) * normalize(alpha(top.multiplyColor)),
		)
		return ColorTransform(addColor = addColor, multiplyColor = multipleColor)
	}

	fun render() {
		if (showcase) {
			choosePassiveAnimation()
			coordinates = transformBattleCoordinates(
				PartyLayoutPosition(40, 60), flipX, context
			)
		} else {
			if (state is BattleStateMachine.MeleeAttack && state.attacker === combatant) {
				chooseMeleeAnimation()
			} else if (state is BattleStateMachine.CastSkill && state.caster === combatant) {
				chooseCastingAnimation()
			} else if (state is BattleStateMachine.UseItem && state.thrower === combatant) {
				chooseItemAnimation()
			} else choosePassiveAnimation()
		}

		renderAnimation()
	}

	private fun chooseMeleeAnimation() {
		if (state !is BattleStateMachine.MeleeAttack) throw Error()
//		val rawTargetCoordinates = state.target.getPosition(context.battle)
//		val targetFlipX = if (state.target.isOnPlayerSide) 1f else -1f
//		val targetModel = state.target.getModel()
		val targetStrikePoint = state.target.renderInfo.strikePoint
//		val rawStrikePosition = PartyLayoutPosition(
//			rawTargetCoordinates.x + (flipX * targetFlipX).roundToInt() * targetStrikePoint.x.roundToInt(),
//			rawTargetCoordinates.y + (targetModel.skeleton.groundDistance - skeleton.groundDistance).roundToInt()
//		)
//		val strikePosition = transformBattleCoordinates(rawStrikePosition, targetFlipX, context)
		var strikePosition = targetStrikePoint
		if (state.attacker.isOnPlayerSide == state.target.isOnPlayerSide) {
			val targetX = state.target.renderInfo.core.x
			val diffX = targetX - targetStrikePoint.x
			strikePosition = CombatantRenderPosition(targetX + diffX, strikePosition.y)
		}

		if (state is BattleStateMachine.MeleeAttack.MoveTo) {
			val moveAnimation = animations["moveto"]
			val moveTime = moveAnimation.frames.size * FRAME_LENGTH
			animation = moveAnimation
			relativeTime = context.renderTime - state.startTime
			if (relativeTime >= moveTime) {
				state.finished = true
				relativeTime = moveTime - 1L
			}

			val movementProgress = relativeTime.toFloat() / moveTime.toFloat()
			coordinates.x = movementProgress * strikePosition.x + (1f - movementProgress) * coordinates.x
			coordinates.y = movementProgress * strikePosition.y + (1f - movementProgress) * coordinates.y
		}

		if (state is BattleStateMachine.MeleeAttack.Strike) {
			val strikeAnimation = animations["strike"]
			val strikeTime = strikeAnimation.frames.size * FRAME_LENGTH
			animation = strikeAnimation
			relativeTime = context.renderTime - state.startTime

			if (relativeTime >= strikeTime / 2) state.canDealDamage = true
			if (relativeTime >= strikeTime) {
				state.finished = true
				relativeTime = strikeTime - 1L
			}

			coordinates.x = strikePosition.x
			coordinates.y = strikePosition.y
		}

		if (state is BattleStateMachine.MeleeAttack.JumpBack) {
			val jumpAnimation = animations["jumpback"]
			val jumpTime = jumpAnimation.frames.size * FRAME_LENGTH

			val relativeJumpTime = context.renderTime - state.startTime
			if (relativeJumpTime >= jumpTime) {
				relativeTime = jumpTime - 1L
				state.finished = true
			} else {
				animation = jumpAnimation
				relativeTime = relativeJumpTime

				var movementProgress = relativeTime.toFloat() / jumpTime.toFloat()
				movementProgress = if (movementProgress < 0.2f) 0f
				else (movementProgress - 0.2f) / 0.5f
				if (movementProgress > 1f) movementProgress = 1f
				coordinates.x = (1f - movementProgress) * strikePosition.x + movementProgress * coordinates.x
				coordinates.y = (1f - movementProgress) * strikePosition.y + movementProgress * coordinates.y
			}
		}
	}

	private fun chooseCastingAnimation() {
		if (state !is BattleStateMachine.CastSkill) throw Error()

		val castAnimation = animations[state.skill.animation ?: "spellcast"]
		val relativeCastTime = context.renderTime - state.startTime
		val castTime = castAnimation.frames.size * FRAME_LENGTH
		if (relativeCastTime < castTime) {
			animation = castAnimation
			relativeTime = relativeCastTime
		} else state.canDealDamage = true
	}

	private fun chooseItemAnimation() {
		if (state !is BattleStateMachine.UseItem) throw Error()

		val itemAnimation = animations["useitem"]
		val relativeThrowTime = context.renderTime - state.startTime
		val throwTime = itemAnimation.frames.size * FRAME_LENGTH
		if (relativeThrowTime < throwTime) {
			animation = itemAnimation
			relativeTime = relativeThrowTime
		} else state.canDrinkItem = true
	}

	private fun choosePassiveAnimation() {
		val lastDamage = combatant.renderInfo.lastDamageIndicator
		if (combatant.isAlive() && lastDamage != null && lastDamage is DamageIndicatorHealth && lastDamage.gainedHealth < 0) {
			val hurtAnimation = animations["hit"]
			val sinceDamage = context.renderTime - lastDamage.time
			val hurtFrame = sinceDamage / FRAME_LENGTH
			if (hurtFrame < hurtAnimation.frames.size) {
				animation = hurtAnimation
				relativeTime = sinceDamage
				return
			}
		}

		if (!combatant.isAlive()) {
			if (lastDamage != null) {
				val dieAnimation = animations["die"]
				val sinceDeath = context.renderTime - lastDamage.time
				val dieFrame = sinceDeath / FRAME_LENGTH
				if (dieFrame < dieAnimation.frames.size) {
					animation = dieAnimation
					relativeTime = sinceDeath
					return
				}
			}

			animation = if (combatant is MonsterCombatantState) null
			else animations["dead"]
		}

		if (combatant.isAlive() && state is BattleStateMachine.Victory) {
			val victoryAnimation = animations["victory"]
			animation = victoryAnimation
			relativeTime = min(context.renderTime - state.startTime, victoryAnimation.frames.size * FRAME_LENGTH - 1)
		}
	}

	private fun renderAnimation() {
		val animation = this.animation ?: return

		val stateMachine = context.battle.state
		var isSelectedTarget = false
		var isSelectingMove = false
		if (stateMachine is BattleStateMachine.SelectMove) {
			isSelectingMove = stateMachine.onTurn === combatant
			isSelectedTarget = stateMachine.selectedMove.targets(context.battle).contains(combatant)
		}

		var meleeElement: Element? = null
		var magicElement: Element? = null
		var isMoving = false
		if (stateMachine is BattleStateMachine.CastSkill && stateMachine.caster === combatant) {
			if (stateMachine.skill.isMelee) {
				meleeElement = stateMachine.skill.element
				isMoving = true
			} else magicElement = stateMachine.skill.element
		}

		if (stateMachine is BattleStateMachine.MeleeAttack && stateMachine.attacker === combatant) isMoving = true

		val scaleX = if (combatant.isOnPlayerSide) coordinates.scale else -coordinates.scale
		val parentMatrix = Matrix3x2f()
			.translate(coordinates.x, coordinates.y)
			.scale(scaleX, coordinates.scale)

		val equipment = combatant.getEquipment(context.updateContext)
		val animationContext = AnimationContext(
			renderTime = context.renderTime,
			magicScale = 4,
			parentMatrix = parentMatrix,
			parentColorTransform = effectColorTransform,
			partBatch = batch,
			noMask = context.context.content.battle.noMask,
			combat = CombatantAnimationContext(
				isSelectedTarget = isSelectedTarget,
				isSelectingMove = isSelectingMove,
				meleeElement = meleeElement,
				magicElement = magicElement,
				isMoving = isMoving,
				rootSkin = animations.skin,
				weaponName = equipment[0]?.flashName,
				shieldName = equipment[1]?.flashName,
				renderInfo = combatant.renderInfo,
			),
		)

		val modTime = relativeTime % animation.duration.inWholeNanoseconds
		renderCombatantAnimation(animation, animations.skeleton.flatNodes, modTime, animationContext)
		return
	}
}
