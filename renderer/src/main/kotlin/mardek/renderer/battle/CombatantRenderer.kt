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
import mardek.state.ingame.battle.combatant.CombatantRenderPosition
import mardek.state.ingame.battle.combatant.CombatantState
import mardek.state.ingame.battle.combatant.MonsterCombatantState
import mardek.state.util.Rectangle
import mardek.content.util.Time
import mardek.content.util.min
import mardek.content.util.rem
import org.joml.Matrix3x2f
import kotlin.math.pow
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val FRAME_LENGTH = 1.seconds / 30

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
		mergeColorTransforms(skipTurnTransform(), levelUpColorTransform()),
	)

	private val animations = combatant.getAnimations()
	private var relativeTime = context.context.timing.elapsedTimeSince(Time.ZERO)
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
		if (combatant.renderInfo.lastPointedTo === Time.ZERO) return null

		val blinkTime = 500.milliseconds
		val passedTime = context.context.timing.elapsedTimeSince(combatant.renderInfo.lastPointedTo)
		if (passedTime >= blinkTime) return null

		return selectedColorTransform(1f - (passedTime / blinkTime).toFloat())
	}

	private fun skipTurnTransform(): ColorTransform? {
		val forcedTurn = combatant.renderInfo.lastForcedTurn ?: return null
		val duration = 1.seconds
		val passedTime = context.context.timing.elapsedTimeSince(forcedTurn.time)
		if (passedTime >= duration) return null

		return colorCombineTransform(1f, (1f - passedTime / duration).toFloat(), forcedTurn.color)
	}

	private fun damageColorTransform(blinkColor: Int, intensity: Float) = colorCombineTransform(
		0.8f, intensity, blinkColor
	)

	private fun damageColorTransform(): ColorTransform? {
		var transform: ColorTransform? = null
		for (indicator in combatant.renderInfo.indicatorHistory.get(context.context.timing)) {
			if (indicator.blinkColor != 0 && indicator.blinkIntensity > 0f) {
				val addedTransform = damageColorTransform(srgbToLinear(indicator.blinkColor), indicator.blinkIntensity)
				transform = mergeColorTransforms(transform, addedTransform)
			}
		}

		return transform
	}

	private fun levelUpColorTransform(): ColorTransform? {
		var transform: ColorTransform? = null
		for (indicator in combatant.renderInfo.levelUpHistory.get(context.context.timing)) {
			if (indicator.blinkIntensity > 0f) {
				val blinkColor = srgbToLinear(rgb(250, 250, 80))
				val addedTransform = damageColorTransform(blinkColor, indicator.blinkIntensity)
				transform = mergeColorTransforms(transform, addedTransform)
			}
		}

		return transform
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
				PartyLayoutPosition(0.23f, 0.51f), -1f, region
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
				false,
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
			chooseMoveToAnimation(
				strikePosition,
				state.startTime,
				{ state.halfWay = true },
				{ state.finished = true },
				true,
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
				PartyLayoutPosition(0.42f, 0.59f), flipX * -1f, region
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
				false,
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
			chooseMoveToAnimation(
				breathPosition,
				state.startTime,
				{ state.halfWay = true },
				{ state.finished = true },
				true,
			)
		}
	}

	private fun chooseMoveToAnimation(
		attackPosition: CombatantRenderPosition,
		startMoveTime: Time,
		setHalfway: () -> Unit,
		setFinished: () -> Unit,
		isJumpingBack: Boolean,
	) {
		val moveAnimation = animations[if (isJumpingBack) "jumpback" else "moveto"]
		val moveTime = FRAME_LENGTH * moveAnimation.frames.size
		val stage = context.context.currentStage
		val aspectRatio = stage.width.toDouble() / stage.height
		animation = moveAnimation

		relativeTime = context.context.timing.elapsedTimeSince(startMoveTime) / aspectRatio

		// Jumping back is quicker
		if (isJumpingBack) relativeTime *= 1.5

		if (relativeTime >= moveTime / 2) setHalfway()
		if (relativeTime >= moveTime) {
			setFinished()
			relativeTime = moveTime - 1.milliseconds
		}

		val t = relativeTime / moveTime
		var acceleratedProgress = (3.0 * (1.0 - t).pow(2) * t * -0.2 + 3.0 * (1.0 - t) * t.pow(2) * 1.2 + t.pow(3)).toFloat()
		acceleratedProgress = acceleratedProgress.coerceIn(0f, 1f)
		if (isJumpingBack) acceleratedProgress = 1f - acceleratedProgress

		coordinates.x = acceleratedProgress * attackPosition.x + (1f - acceleratedProgress) * coordinates.x
		coordinates.y = acceleratedProgress * attackPosition.y + (1f - acceleratedProgress) * coordinates.y
	}

	private fun chooseAttackAnimation(
		attackPosition: CombatantRenderPosition,
		startAttackTime: Time,
		animationName: String,
		setCanDealDamage: () -> Unit,
		setFinished: () -> Unit,
	) {
		val breathAnimation = animations[animationName]
		val breathTime = FRAME_LENGTH * breathAnimation.frames.size
		animation = breathAnimation
		relativeTime = context.context.timing.elapsedTimeSince(startAttackTime)

		if (relativeTime >= breathTime / 2) setCanDealDamage()
		if (relativeTime >= breathTime) {
			setFinished()
			relativeTime = breathTime - 1.milliseconds
		}

		coordinates.x = attackPosition.x
		coordinates.y = attackPosition.y
	}

	private fun chooseCastingAnimation() {
		if (state !is BattleStateMachine.CastSkill) throw Error()

		val castAnimation = animations[state.skill.animation ?: "spellcast"]
		val relativeCastTime = context.context.timing.elapsedTimeSince(state.startTime)
		val castTime = FRAME_LENGTH * castAnimation.frames.size
		if (relativeCastTime < castTime) {
			animation = castAnimation
			relativeTime = relativeCastTime
		} else state.hasFinishedCastingAnimation = true

		if (relativeCastTime > castTime / 2) state.canSpawnTargetParticles = true
	}

	private fun chooseItemAnimation() {
		if (state !is BattleStateMachine.UseItem) throw Error()

		val itemAnimation = animations["useitem"]
		val relativeThrowTime = context.context.timing.elapsedTimeSince(state.startTime)
		val throwTime = FRAME_LENGTH * itemAnimation.frames.size
		if (relativeThrowTime < throwTime) {
			animation = itemAnimation
			relativeTime = relativeThrowTime
		} else state.canDrinkItem = true
	}

	private fun choosePassiveAnimation() {
		val lastDamage = combatant.renderInfo.indicatorHistory.mostRecentDamageTakenAt(context.context.timing)
		if (combatant.isAlive() && lastDamage != null) {
			val hurtAnimation = animations["hit"]
			val sinceDamage = context.context.timing.elapsedTimeSince(lastDamage)
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
				val sinceDeath = context.context.timing.elapsedTimeSince(lastDamage)
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
			val elapsedTime = context.context.timing.elapsedTimeSince(state.startTime)
			if (elapsedTime >= BattleStateMachine.Victory.DELAY_UNTIL_ANIMATION) {
				val victoryAnimation = animations["victory"]
				animation = victoryAnimation
				relativeTime = min(
					elapsedTime - BattleStateMachine.Victory.DELAY_UNTIL_ANIMATION,
					(FRAME_LENGTH * victoryAnimation.frames.size) - 1.milliseconds,
				)
			}
		}
	}

	private fun renderAnimation() {
		val animation = this.animation ?: return

		val stateMachine = context.battle.state
		var isSelectedTarget = false
		var isSelectingMove = false
		if (stateMachine is BattleStateMachine.SelectMove && !showcase) {
			isSelectingMove = stateMachine.onTurn === combatant
			isSelectedTarget = stateMachine.selectedMove.targets(context.battle).contains(combatant)
		}

		var meleeElement: Element? = null
		var magicElement: Element? = null
		var isMoving = false
		if (stateMachine is BattleStateMachine.CastSkill && stateMachine.caster === combatant &&
			!showcase && stateMachine.skill.particleEffect != null
		) {
			magicElement = stateMachine.skill.element
		}

		if (stateMachine is BattleStateMachine.BreathAttack && stateMachine.attacker === combatant && !showcase) {
			isMoving = true
		}

		if (stateMachine is BattleStateMachine.MeleeAttack && stateMachine.attacker === combatant && !showcase) {
			isMoving = true
			meleeElement = stateMachine.skill?.element ?:
					stateMachine.attacker.getWeapon(context.updateContext)?.element
		}

		val parentMatrix = Matrix3x2f()
			.translate(coordinates.x, coordinates.y)
			.scale(region.height.toFloat())
		if (!combatant.isOnPlayerSide || showcase) parentMatrix.scale(-1f, 1f)
		parentMatrix.mul(toJOMLMatrix(animations.rootMatrix))

		val animationContext = AnimationContext(
			renderRegion = region,
			timing = context.context.timing,
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
				weaponName = combatant.getWeapon(context.updateContext)?.displayName,
				shieldName = combatant.getEquipment(context.updateContext).find {
					it?.type?.displayName?.contains("SHIELD") ?: false
				}?.displayName,
				renderInfo = combatant.renderInfo,
			),
			portrait = null,
			currentChapter = context.context.campaign.story.evaluate(
				context.context.content.story.fixedVariables.chapter
			) ?: 0,
			animationDuration = animation.duration,
		)

		val modTime = relativeTime % animation.duration

		for (state in combatant.renderInfo.animationParticles.values) {
			state.positions.clear()
		}
		renderCombatantAnimation(
			animation, animations.skeleton.earlyFlatNodes,
			animations.skeleton.lateFlatNodes,
			modTime, animationContext
		)
		combatant.renderInfo.animationParticles.values.removeIf { it.positions.isEmpty() }
		return
	}

	companion object {
		fun sortByDepth(state: BattleState, combatants: List<CombatantState>): List<CombatantState> {
			val sorted = combatants.sortedBy { it.getPosition(state).distanceY }.toMutableList()

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
