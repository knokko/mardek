package mardek.state.ingame.battle

import mardek.content.particle.ParticleEffect
import mardek.content.particle.ParticleEmitter
import mardek.state.util.RenderTiming
import mardek.content.util.Time
import java.lang.Math.toRadians
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.time.DurationUnit

/**
 * Represents the state of a particle effect/emitters at a concrete position. If the `update()` method is called
 * periodically, it will ensure that each emitter of the particle effect is activated once during each of its periods.
 */
class ParticleEffectState(
	/**
	 * The type of particle effect, for instance `pfx_flame1`. Each particle effect has a list of emitters that needs to
	 * be activated once during each of its periods.
	 */
	val particle: ParticleEffect,

	/**
	 * The base position where the particle should be rendered
	 */
	val position: CombatantRenderPosition,

	/**
	 * When true, the particle should be mirrored over the vertical line through `position`
	 */
	val mirrorX: Boolean,
) {

	/**
	 * The time at which the `update()` method was first called
	 */
	var startTime = Time.ZERO

	/**
	 * The concrete particle emitters
	 */
	val emitters = particle.emitters().map { ParticleEmitterState(it) }.toMutableList()

	private var playedInitialSound = false
	private var playedDamageSound = false

	/**
	 * Updates this particle state: this will activate the emitters if the time is right, and also play some sounds if
	 * the time is right.
	 *
	 * Returns `true` if and only if this `ParticleEffectState` can be removed
	 */
	fun update(context: BattleUpdateContext, timing: RenderTiming): Boolean {
		if (startTime == Time.ZERO) startTime = timing.now()

		if (!playedInitialSound) {
			particle.initialSound()?.let { context.soundQueue.insert(it) }
			playedInitialSound = true
		}

		if (!playedDamageSound && timing.elapsedTimeSince(startTime) >= particle.damageDelay()) {
			particle.damageSound()?.let { context.soundQueue.insert(it) }
			playedDamageSound = true
		}

		// TODO CHAP3 Extra sound delays
		// TODO CHAP3 Quake
		emitters.removeIf { it.update(startTime, timing) }
		return playedInitialSound && playedDamageSound && emitters.isEmpty()
	}
}

/**
 * Represents the state of a [ParticleEmitter]. This tracks which particles it has already spawned, as well as how many
 * waves it still needs to spawn.
 */
class ParticleEmitterState(

	/**
	 * The content [ParticleEmitter]. Note that multiple [ParticleEmitterState]s can have
	 * the same `emitter.
	 */
	val emitter: ParticleEmitter,
) {

	/**
	 * The particles that were already spawned by this emitter state
	 */
	val particles = mutableListOf<ParticleState>()

	/**
	 * The number of waves that this emitter state has already spawned. The [update] method will spawn more waves
	 * whenever this value is becoming too low.
	 */
	var numSpawnedWaves = 0

	/**
	 * Updates this emitter state, as well as all the particles that it has spawned. If needed, this method will spawn
	 * a new wave of particles. It also removes expired particles.
	 */
	fun update(startTime: Time, timing: RenderTiming): Boolean {
		val maxEmitterRounds = emitter.waves.numRounds
		val timeSinceStart = timing.elapsedTimeSince(startTime)

		var expectedSpawnedWaves = 1 + ((timeSinceStart - emitter.waves.delay) / emitter.waves.period).toInt()
		if (maxEmitterRounds != null) expectedSpawnedWaves = min(maxEmitterRounds, expectedSpawnedWaves)
		while (numSpawnedWaves < expectedSpawnedWaves) {
			val deltaTime = emitter.waves.delay + emitter.waves.period * numSpawnedWaves
			val deltaTimeSeconds = deltaTime.toDouble(DurationUnit.SECONDS).toFloat()
			for (index in 0 until emitter.waves.particlesPerWave) {
				particles.add(ParticleState(emitter, index, deltaTimeSeconds, timing.now()))
			}
			numSpawnedWaves += 1
		}

		particles.removeIf { it.hasExpired(timing) }
		return maxEmitterRounds != null && particles.isEmpty() && numSpawnedWaves >= maxEmitterRounds
	}
}

/**
 * Represents a single particle spawned from a [ParticleEmitterState]. This class captures the position, size, and
 * velocity when the particle was spawned, and uses this to derive the position and size in the future.
 */
class ParticleState(

	/**
	 * The emitter from which this particle was spawned. We need to remember this because it influences some
	 * dynamics of this particle.
	 */
	val emitter: ParticleEmitter,
	indexInWave: Int,
	deltaTime: Float,

	/**
	 * The time at which this particle was spawned.
	 */
	val spawnTime: Time,
) {

	/**
	 * The X-coordinate of this particle, when it was spawned
	 */
	val initialX: Float

	/**
	 * The Y-coordinate of this particle, when it was spawned
	 */
	val initialY: Float

	/**
	 * The width of this particle, when it was spawned
	 */
	val initialWidth: Float

	/**
	 * The height of this particle, when it was spawned
	 */
	val initialHeight: Float

	/**
	 * The rotation (degrees) of this particle, when it was spawned
	 */
	val initialRotation: Float

	/**
	 * The X-component of the velocity (units per second) of this particle, when it was spawned
	 */
	val initialVelocityX: Float

	/**
	 * The Y-component of the velocity (units per second) of this particle, when it was spawned
	 */
	val initialVelocityY: Float

	/**
	 * The X-component of the acceleration (units per second^2) of this particle, when it was spawned
	 */
	val accelerationX: Float

	/**
	 * The Y-component of the acceleration (units per second^2) of this particle, when it was spawned
	 */
	val accelerationY: Float

	init {
		var initialX = emitter.spawn.baseX + deltaTime * emitter.spawn.shiftX
		var initialY = emitter.spawn.baseY + deltaTime * emitter.spawn.shiftY
		var initialWidth = emitter.size.baseWidth + deltaTime * emitter.size.shiftWidth
		var initialHeight = emitter.size.baseHeight + deltaTime * emitter.size.shiftHeight
		val variationR = emitter.spawn.rotationVariation
		var initialRotation = emitter.spawn.rotation + Random.nextFloat() * variationR - 0.5f * variationR
		val variationX = emitter.spawn.variationX + deltaTime * emitter.spawn.shiftVariationX
		val variationY = emitter.spawn.variationY + deltaTime * emitter.spawn.shiftVariationY
		initialX += Random.nextFloat() * variationX - 0.5f * variationX
		initialY += Random.nextFloat() * variationY - 0.5f * variationY

		val sizeMultiplier = emitter.size.minSizeMultiplier + Random.nextFloat() *
				(emitter.size.maxSizeMultiplier - emitter.size.minSizeMultiplier)
		initialWidth *= sizeMultiplier
		initialHeight *= sizeMultiplier

		var initialVelocityX = 0f
		var initialVelocityY = 0f

		val linear = emitter.spawn.linear
		if (linear != null) {
			val minVelocityX = linear.minVelocityX + deltaTime * linear.shiftMinVelocityX
			val maxVelocityX = linear.maxVelocityX + deltaTime * linear.shiftMaxVelocityX
			initialVelocityX += minVelocityX + Random.nextFloat() * (maxVelocityX - minVelocityX)
			val minVelocityY = linear.minVelocityY + deltaTime * linear.shiftMinVelocityY
			val maxVelocityY = linear.maxVelocityY + deltaTime * linear.shiftMaxVelocityY
			initialVelocityY += minVelocityY + Random.nextFloat() * (maxVelocityY - minVelocityY)
		}

		val radial = emitter.spawn.radial
		if (radial != null) {
			val evenlySpaced = radial.evenlySpaced
			val moveDirection = if (evenlySpaced != null) {
				val startAngle = evenlySpaced + deltaTime * radial.shiftEvenlySpaced
				val evenAngle = indexInWave * 360f / emitter.waves.particlesPerWave
				startAngle + evenAngle
			} else 360f * Random.nextFloat()

			if (radial.rotateToMoveDirection) initialRotation = moveDirection

			val minRadius = radial.minRadius + deltaTime * radial.shiftMinRadius
			val maxRadius = radial.maxRadius + deltaTime * radial.shiftMaxRadius
			val radius = minRadius + Random.nextFloat() * (maxRadius - minRadius)

			val moveX = cos(toRadians(moveDirection.toDouble())).toFloat()
			val moveY = sin(toRadians(moveDirection.toDouble())).toFloat()
			initialX += radius * moveX
			initialY += radius * moveY

			val initialVelocity = radial.minVelocity + Random.nextFloat() * (radial.maxVelocity - radial.minVelocity)
			initialVelocityX += moveX * initialVelocity
			initialVelocityY += moveY * initialVelocity
		}

		initialRotation *= emitter.spawn.rotationMultiplier.pow(deltaTime)

		var accelerationX = emitter.dynamics.accelerationX
		var accelerationY = emitter.dynamics.accelerationY
		accelerationX += deltaTime * emitter.dynamics.shiftAccelerationX
		accelerationY += deltaTime * emitter.dynamics.shiftAccelerationY

		if (emitter.dynamics.radialAcceleration != 0f) {
			val initialVelocity = sqrt(initialVelocityX * initialVelocityX + initialVelocityY * initialVelocityY)
			if (initialVelocity > 0.0001f) {
				val initialDirectionX = initialVelocityX / initialVelocity
				val initialDirectionY = initialVelocityY / initialVelocity
				accelerationX += initialDirectionX * emitter.dynamics.radialAcceleration
				accelerationY += initialDirectionY * emitter.dynamics.radialAcceleration
			}
		}

		this.initialX = initialX
		this.initialY = initialY
		this.initialWidth = initialWidth
		this.initialHeight = initialHeight
		this.initialRotation = initialRotation
		this.initialVelocityX = initialVelocityX
		this.initialVelocityY = initialVelocityY
		this.accelerationX = accelerationX
		this.accelerationY = accelerationY
	}

	/**
	 * Checks whether this particle should expire
	 */
	fun hasExpired(timing: RenderTiming) = timing.elapsedTimeSince(spawnTime) >= emitter.lifeTime

	private fun elapsedSeconds(timing: RenderTiming) = timing.elapsedTimeSince(
		spawnTime
	).toDouble(DurationUnit.SECONDS).toFloat()

	/**
	 * Computes the current X-coordinate of this particle.
	 *
	 * The recurrence formula for the velocity is
	 * ```
	 * v[t] = M * (v[t-1] + A)
	 * ```
	 * where `M` is `velocityMultiplierX`, and `A` is `accelerationX`.
	 * To compute the steady-state velocity `v*`, we solve
	 * ```
	 * v* = M * (v* + A)
	 * v* = M * v* + M * A
	 * v* - M * v* = M * A
	 * (1 - M)v* = M * A
	 * v* = M * A / (1 - M)
	 * ```
	 * Using `v*`, the original formula can be rewritten to
	 * ```
	 * v(t) - v* = (v[0] - v*)M^t
	 * v(t) = v* + (v[0] - v*)M^t
	 * ```
	 *
	 * To compute the (X) position, we need the antiderivative V(t) of v(t): which is
	 * ```
	 * V(t) = tv* + (v[0] - v*)M^t / ln(M) + C
	 * ```
	 * Finally
	 * ```
	 * x(t) = x(0) + V(t) - V(0) =
	 * x(0) + tv* + (v[0] - v*)M^t / ln(M) + C - 0v* - (v[0] - v*)M^0 / ln(M) - C =
	 * x(0) + tv* + (v[0] - v*)M^t / ln(M) - (v[0] - v*) / ln(M)
	 * ```
	 */
	fun computeX(timing: RenderTiming): Float {
		val t = elapsedSeconds(timing)
		val velocityMultiplier = emitter.dynamics.velocityMultiplierX
		if (velocityMultiplier == 1f) return initialX + t * initialVelocityX + t * t * 0.5f * accelerationX

		val steadyVelocity = velocityMultiplier * accelerationX / (1f - velocityMultiplier)
		val lnMultiplier = ln(velocityMultiplier)
		return initialX + t * steadyVelocity + (initialVelocityX - steadyVelocity) *
				velocityMultiplier.pow(t) / lnMultiplier -
				(initialVelocityX - steadyVelocity) / lnMultiplier
	}

	/**
	 * Computes the current Y-coordinate of this particle.
	 *
	 * See the doc comments of [computeX] for a derivation of the formula.
	 */
	fun computeY(timing: RenderTiming): Float {
		val t = elapsedSeconds(timing)
		val velocityMultiplier = emitter.dynamics.velocityMultiplierY
		if (velocityMultiplier == 1f) return initialY + t * initialVelocityY + t * t * 0.5f * accelerationY

		val steadyVelocity = velocityMultiplier * accelerationY / (1f - velocityMultiplier)
		val lnMultiplier = ln(velocityMultiplier)
		return initialY + t * steadyVelocity + (initialVelocityY - steadyVelocity) *
				velocityMultiplier.pow(t) / lnMultiplier -
				(initialVelocityY - steadyVelocity) / lnMultiplier
	}

	/**
	 * Computes the current rotation (degrees) of the particle
	 */
	fun computeRotation(timing: RenderTiming): Float {
		val t = elapsedSeconds(timing)
		return initialRotation + t * emitter.dynamics.spin
	}

	private fun computeSize(timing: RenderTiming, initialSize: Float, grow: Float, dynamicGrow: Float): Float {
		if (grow == 1f && dynamicGrow == 0f) return initialSize

		val t = elapsedSeconds(timing).toDouble()
		if (dynamicGrow == 0f) return (initialSize * grow.toDouble().pow(t)).toFloat()

		/*
		 * The recurrence formula for the size is:
		 *
		 * s[t] = s[t-1] * g[t-1] where
		 *   g[t] = g[t-1] + g_s = g[0] + t * g_s
		 *
		 * So s[t] = s[t-1] * (g[0] + (t-1) * g_s) =
		 *     s[0] * g[0] * (g[0] + g_s) * (g[0] + 2g_s) * (g[0] + 3g_s) * ... * (g[0] + (t-1)g_s)
		 *
		 * According to AI, this is a multiplicative, first-order, non-linear recurrence, which does not have a nice
		 * characteristic equation that can be used to solve it. It looks like this formula does not really have an
		 * exact closed-form expression, so let's compute it step-by-step instead.
		 */
		val numSteps = (t * 30).toInt()
		var size = initialSize
		for (step in 0 until numSteps) {
			size *= (grow + step * dynamicGrow / 30)
		}
		val nextSize = size * (grow + numSteps * dynamicGrow / 30)
		val remainingTime = t - numSteps / 30.0
		val factor = remainingTime * 30.0
		return ((1.0 - factor) * size + factor * nextSize).toFloat()
	}

	/**
	 * Computes the current width of the particle
	 */
	fun computeWidth(timing: RenderTiming) = computeSize(
		timing, initialWidth, emitter.size.growX, emitter.size.dynamicGrowX
	)

	/**
	 * Computes the current height of the particle
	 */
	fun computeHeight(timing: RenderTiming) = computeSize(
		timing, initialHeight, emitter.size.growY, emitter.size.dynamicGrowY
	)
}
