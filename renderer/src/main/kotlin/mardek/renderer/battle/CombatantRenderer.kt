package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.*
import mardek.content.animation.AnimationFrames
import mardek.content.animation.ColorTransform
import mardek.content.battle.PartyLayoutPosition
import mardek.content.skill.SkillTargetType
import mardek.content.stats.Element
import mardek.renderer.animation.AnimationContext
import mardek.renderer.animation.AnimationPartBatch
import mardek.renderer.animation.CombatantAnimationContext
import mardek.renderer.animation.renderCombatantAnimation
import mardek.renderer.animation.toJOMLMatrix
import mardek.state.ingame.battle.BattleState
import mardek.state.ingame.battle.BattleStateMachine
import mardek.state.ingame.battle.CombatantRenderPosition
import mardek.state.ingame.battle.CombatantState
import mardek.state.ingame.battle.DamageIndicatorHealth
import mardek.state.ingame.battle.DamageIndicatorMana
import mardek.state.ingame.battle.MonsterCombatantState
import mardek.state.util.Rectangle
import org.joml.Matrix3x2f
import kotlin.math.min

private const val FRAME_LENGTH = 33_000_000L

class CombatantRenderer(
	private val context: BattleRenderContext,
	private val batch: AnimationPartBatch,
	private val combatant: CombatantState,
	private val region: Rectangle,
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
		combatant.getPosition(context.battle), flipX, region
	)

	private fun colorCombineTransform(max: Float, intensity: Float, color: Int) = ColorTransform(
		addColor = rgba(
			normalize(red(color)) * max * intensity,
			normalize(green(color)) * max * intensity,
			normalize(blue(color)) * max * intensity, 0f
		),
		multiplyColor = rgb(1f - max * intensity, 1f - max * intensity, 1f - max * intensity),
		subtractColor = 0,
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

		val addColor = addColors(multiplyColors(base.addColor, top.multiplyColor), top.addColor)
		val multiplyColor = multiplyColors(base.multiplyColor, top.multiplyColor)
		val subtractColor = addColors(multiplyColors(base.subtractColor, top.multiplyColor), top.subtractColor)
		return ColorTransform(addColor = addColor, multiplyColor = multiplyColor, subtractColor = subtractColor)
	}

	fun render() {
		if (showcase) {
			choosePassiveAnimation()
			coordinates = transformBattleCoordinates(
				PartyLayoutPosition(40, 60), flipX, region
			)
		} else {
			when (state) {
				is BattleStateMachine.MeleeAttack if state.attacker === combatant -> {
					chooseMeleeAnimation()
				}

				is BattleStateMachine.BreathAttack if state.attacker === combatant -> {
					chooseBreathAnimation()
				}

				is BattleStateMachine.CastSkill if state.caster === combatant -> {
					chooseCastingAnimation()
				}

				is BattleStateMachine.UseItem if state.thrower === combatant -> {
					chooseItemAnimation()
				}

				else -> choosePassiveAnimation()
			}
		}

		renderAnimation()
	}

	private fun chooseMeleeAnimation() {
		if (state !is BattleStateMachine.MeleeAttack) throw Error()

		val attacker = state.attacker.renderInfo
		val target = state.target.renderInfo

		val originTargetCoordinates = transformBattleCoordinates(
			state.target.getPosition(context.battle), 0f, region
		)

		// We want the X-coordinate of the StrikePoint of the attacker to coincide with the HitPoint of the target
		val strikeX = target.hitPoint.x + (attacker.core.x - attacker.strikePoint.x)

		// The Y-coordinate of the attacker should simply equal the Y-coordinate of the target
		val strikePosition = CombatantRenderPosition(strikeX, originTargetCoordinates.y)

		if (state is BattleStateMachine.MeleeAttack.MoveTo) {
			chooseMoveToAnimation(
				strikePosition,
				state.startTime,
				{ state.halfWay = true },
				{ state.finished = true },
			)
		}

		if (state is BattleStateMachine.MeleeAttack.Strike) {
			chooseAttackAnimation(
				strikePosition,
				state.startTime,
				"strike",
				{ state.canDealDamage = true },
				{ state.finished = true },
			)
		}

		if (state is BattleStateMachine.MeleeAttack.JumpBack) {
			chooseJumpBackAnimation(
				strikePosition,
				state.startTime,
				{ state.halfWay = true },
				{ state.finished = true },
			)
		}
	}

	private fun chooseBreathAnimation() {
		if (state !is BattleStateMachine.BreathAttack) throw Error()

		val breathPosition = if (state.skill.targetType == SkillTargetType.AllEnemies) {
			val attacker = state.attacker.renderInfo

			// Position the BreathSource of the attacker such that:
			// - The horizontal distance to the targets is roughly 40% of the region height
			// - The Y-coordinate is slightly below the middle of the screen
			val dummyTargetPosition = transformBattleCoordinates(
				PartyLayoutPosition(75, 60), flipX * -1f, region
			)
			val breathX = dummyTargetPosition.x + 0.4f * flipX * region.height
			CombatantRenderPosition(
				breathX + (attacker.core.x - attacker.idleBreathSource.x),
				region.minY + 0.55f * region.height + (attacker.core.y - attacker.idleBreathSource.y),
			)
		} else {
			if (state.targets.size != 1) throw IllegalStateException(
				"Single-target breath attacks must have exactly 1 target"
			)
			val attacker = state.attacker.renderInfo
			val target = state.targets[0].renderInfo

			val originTargetCoordinates = transformBattleCoordinates(
				state.targets[0].getPosition(context.battle), 0f, region
			)

			// We want the X-coordinate of the BreathDistance of the attacker to coincide with the HitPoint of the target
			val strikeX = target.hitPoint.x + (attacker.core.x - attacker.breathDistance.x)

			// The Y-coordinate of the attacker should simply equal the Y-coordinate of the target
			CombatantRenderPosition(strikeX, originTargetCoordinates.y)
		}

		if (state is BattleStateMachine.BreathAttack.MoveTo) {
			chooseMoveToAnimation(
				breathPosition,
				state.startTime,
				{ state.halfWay = true },
				{ state.finished = true },
			)
		}

		if (state is BattleStateMachine.BreathAttack.Attack) {
			chooseAttackAnimation(
				breathPosition,
				state.startTime,
				"breath",
				{ state.canDealDamage = true },
				{ state.finished = true },
			)
		}

		if (state is BattleStateMachine.BreathAttack.JumpBack) {
			chooseJumpBackAnimation(
				breathPosition,
				state.startTime,
				{ state.halfWay = true },
				{ state.finished = true },
			)
		}
	}

	private fun chooseMoveToAnimation(
		attackPosition: CombatantRenderPosition,
		startMoveTime: Long,
		setHalfway: () -> Unit,
		setFinished: () -> Unit,
	) {
		val moveAnimation = animations["moveto"]
		val moveTime = moveAnimation.frames.size * FRAME_LENGTH
		animation = moveAnimation
		relativeTime = context.renderTime - startMoveTime
		if (relativeTime >= moveTime / 2L) setHalfway()
		if (relativeTime >= moveTime) {
			setFinished()
			relativeTime = moveTime - 1L
		}

		val movementProgress = relativeTime.toFloat() / moveTime.toFloat()
		coordinates.x = movementProgress * attackPosition.x + (1f - movementProgress) * coordinates.x
		coordinates.y = movementProgress * attackPosition.y + (1f - movementProgress) * coordinates.y
	}

	private fun chooseAttackAnimation(
		attackPosition: CombatantRenderPosition,
		startAttackTime: Long,
		animationName: String,
		setCanDealDamage: () -> Unit,
		setFinished: () -> Unit,
	) {
		val breathAnimation = animations[animationName]
		val breathTime = breathAnimation.frames.size * FRAME_LENGTH
		animation = breathAnimation
		relativeTime = context.renderTime - startAttackTime

		if (relativeTime >= breathTime / 2) setCanDealDamage()
		if (relativeTime >= breathTime) {
			setFinished()
			relativeTime = breathTime - 1L
		}

		coordinates.x = attackPosition.x
		coordinates.y = attackPosition.y
	}

	private fun chooseJumpBackAnimation(
		strikePosition: CombatantRenderPosition,
		startJumpTime: Long,
		setHalfway: () -> Unit,
		setFinished: () -> Unit,
	) {
		val jumpAnimation = animations["jumpback"]
		val jumpTime = jumpAnimation.frames.size * FRAME_LENGTH

		val relativeJumpTime = context.renderTime - startJumpTime
		if (relativeJumpTime >= jumpTime / 2L) setHalfway()
		if (relativeJumpTime >= jumpTime) {
			relativeTime = jumpTime - 1L
			setFinished()
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

	private fun chooseCastingAnimation() {
		if (state !is BattleStateMachine.CastSkill) throw Error()

		val castAnimation = animations[state.skill.animation ?: "spellcast"]
		val relativeCastTime = context.renderTime - state.startTime
		val castTime = castAnimation.frames.size * FRAME_LENGTH
		if (relativeCastTime < castTime) {
			animation = castAnimation
			relativeTime = relativeCastTime
		} else state.hasFinishedCastingAnimation = true

		if (relativeCastTime > castTime / 2L) state.canSpawnTargetParticles = true
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
			magicElement = stateMachine.skill.element
		}

		if (stateMachine is BattleStateMachine.BreathAttack && stateMachine.attacker === combatant) {
			isMoving = true
		}

		if (stateMachine is BattleStateMachine.MeleeAttack && stateMachine.attacker === combatant) {
			isMoving = true
			meleeElement = stateMachine.skill?.element ?:
					stateMachine.attacker.getEquipment(context.updateContext)[0]?.element
		}

		val scaleX = if (combatant.isOnPlayerSide) coordinates.scale else -coordinates.scale
		val parentMatrix = Matrix3x2f()
			.translate(coordinates.x, coordinates.y)
			.scale(scaleX, coordinates.scale)
		parentMatrix.mul(toJOMLMatrix(animations.rootMatrix))

		val equipment = combatant.getEquipment(context.updateContext)
		val animationContext = AnimationContext(
			renderRegion = region,
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
			portrait = null,
		)

		val modTime = relativeTime % animation.duration.inWholeNanoseconds
		renderCombatantAnimation(animation, animations.skeleton.flatNodes, modTime, animationContext)
		return
	}

	companion object {
		fun sortByDepth(state: BattleState, combatants: List<CombatantState>): List<CombatantState> {
			val sorted = combatants.sortedBy { it.getPosition(state).y }.toMutableList()

			val machine = state.state
			if (machine is BattleStateMachine.MeleeAttack && machine.attacker !== machine.target) {
				var overrideDepth = machine is BattleStateMachine.MeleeAttack.Strike
				if (machine is BattleStateMachine.MeleeAttack.MoveTo && machine.halfWay) overrideDepth = true
				if (machine is BattleStateMachine.MeleeAttack.JumpBack && !machine.halfWay) overrideDepth = true

				if (overrideDepth) {
					sorted.remove(machine.attacker)
					val indexTarget = sorted.indexOf(machine.target)
					sorted.add(indexTarget + 1, machine.attacker)
				}
			}

			return sorted
		}
	}
}
