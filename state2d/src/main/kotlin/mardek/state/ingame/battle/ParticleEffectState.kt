package mardek.state.ingame.battle

import mardek.content.particle.ParticleEffect
import mardek.content.particle.ParticleEmitter
import java.lang.Math.toRadians
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class ParticleEffectState(val particle: ParticleEffect, val position: CombatantRenderPosition) {
	var startTime = 0L
	val emitters = particle.emitters().map { ParticleEmitterState(it) }.toMutableList()

	private var playedInitialSound = false
	private var playedDamageSound = false

	/**
	 * Returns `true` if and only if the particle can be removed
	 */
	fun update(context: BattleUpdateContext, currentTime: Long): Boolean {
		if (startTime == 0L) startTime = currentTime

		if (!playedInitialSound) {
			particle.initialSound()?.let { context.soundQueue.insert(it) }
			playedInitialSound = true
		}

		val passedSeconds = (currentTime - startTime) / 1000_000_000f
		if (!playedDamageSound && passedSeconds >= particle.damageDelay()) {
			particle.damageSound()?.let { context.soundQueue.insert(it) }
			playedDamageSound = true
		}

		// TODO Extra sound delays
		// TODO Quake
		emitters.removeIf { it.update(startTime, currentTime) }
		return playedInitialSound && playedDamageSound && emitters.isEmpty()
	}
}

class ParticleEmitterState(
	val emitter: ParticleEmitter,
) {
	val particles = mutableListOf<ParticleState>()
	var numSpawnedWaves = 0

	internal fun update(startTime: Long, currentTime: Long): Boolean {
		val passedSeconds = (currentTime - startTime) / 1000_000_000f
		val expectedSpawnedWaves = min(emitter.waves.numRounds, 1 + ((passedSeconds - emitter.waves.delay) / emitter.waves.period).toInt())
		while (numSpawnedWaves < expectedSpawnedWaves) {
			val deltaTime = emitter.waves.delay + numSpawnedWaves * emitter.waves.period
			for (index in 0 until emitter.waves.particlesPerWave) {
				particles.add(ParticleState(emitter, index, deltaTime, currentTime))
			}
			numSpawnedWaves += 1
		}

		particles.removeIf { it.hasExpired(currentTime) }
		return particles.isEmpty() && numSpawnedWaves >= emitter.waves.numRounds
	}
}

class ParticleState(
	val emitter: ParticleEmitter,
	indexInWave: Int,
	deltaTime: Float,
	val spawnTime: Long,
) {
	val initialX: Float
	val initialY: Float
	val initialWidth: Float
	val initialHeight: Float
	val initialRotation: Float
	val initialVelocityX: Float
	val initialVelocityY: Float
	val accelerationX: Float
	val accelerationY: Float

	init {
		var initialX = emitter.spawn.baseX + deltaTime * emitter.spawn.shiftX
		var initialY = emitter.spawn.baseY + deltaTime * emitter.spawn.shiftY
		var initialWidth = emitter.size.baseWidth + deltaTime * emitter.size.shiftWidth
		var initialHeight = emitter.size.baseHeight + deltaTime * emitter.size.shiftHeight
		var initialRotation = emitter.spawn.rotation ?: (360f * Random.Default.nextFloat())
		val variationX = emitter.spawn.variationX + deltaTime * emitter.spawn.shiftVariationX
		val variationY = emitter.spawn.variationY + deltaTime * emitter.spawn.shiftVariationY
		initialX += Random.Default.nextFloat() * variationX - 0.5f * variationX
		initialY += Random.Default.nextFloat() * variationY - 0.5f * variationY

		val sizeMultiplier = emitter.size.minSizeMultiplier + Random.Default.nextFloat() *
				(emitter.size.maxSizeMultiplier - emitter.size.minSizeMultiplier)
		initialWidth *= sizeMultiplier
		initialHeight *= sizeMultiplier

		var initialVelocityX = 0f
		var initialVelocityY = 0f

		val linear = emitter.spawn.linear
		if (linear != null) {
			val minVelocityX = linear.minVelocityX + deltaTime * linear.shiftMinVelocityX
			val maxVelocityX = linear.maxVelocityX + deltaTime * linear.shiftMaxVelocityX
			initialVelocityX += minVelocityX + Random.Default.nextFloat() * (maxVelocityX - minVelocityX)
			val minVelocityY = linear.minVelocityY + deltaTime * linear.shiftMinVelocityY
			val maxVelocityY = linear.maxVelocityY + deltaTime * linear.shiftMaxVelocityY
			initialVelocityY += minVelocityY + Random.Default.nextFloat() * (maxVelocityY - minVelocityY)
		}

		val radial = emitter.spawn.radial
		if (radial != null) {
			val evenlySpaced = radial.evenlySpaced
			val moveDirection = if (evenlySpaced != null) {
				val startAngle = evenlySpaced + deltaTime * radial.shiftEvenlySpaced
				val evenAngle = indexInWave * 360f / emitter.waves.particlesPerWave
				startAngle + evenAngle
			} else 360f * Random.Default.nextFloat()

			if (radial.rotateToMoveDirection) initialRotation = moveDirection

			val minRadius = radial.minRadius + deltaTime * radial.shiftMinRadius
			val maxRadius = radial.maxRadius + deltaTime * radial.shiftMaxRadius
			val radius = minRadius + Random.Default.nextFloat() * (maxRadius - minRadius)

			val moveX = cos(toRadians(moveDirection.toDouble())).toFloat()
			val moveY = sin(toRadians(moveDirection.toDouble())).toFloat()
			initialX += radius * moveX
			initialY += radius * moveY

			val initialVelocity = radial.minVelocity + Random.Default.nextFloat() * (radial.maxVelocity - radial.minVelocity)
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

	fun hasExpired(renderTime: Long) = renderTime - spawnTime >= emitter.lifeTime * 1000_000_000L

	/**
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
	fun computeX(renderTime: Long): Float {
		val t = (renderTime - spawnTime) / 1000_000_000f
		val velocityMultiplier = emitter.dynamics.velocityMultiplierX
		if (velocityMultiplier == 1f) return initialX + t * initialVelocityX + t * t * 0.5f * accelerationX

		val steadyVelocity = velocityMultiplier * accelerationX / (1f - velocityMultiplier)
		val lnMultiplier = ln(velocityMultiplier)
		return initialX + t * steadyVelocity + (initialVelocityX - steadyVelocity) *
				velocityMultiplier.pow(t) / lnMultiplier -
				(initialVelocityX - steadyVelocity) / lnMultiplier
	}

	fun computeY(renderTime: Long): Float {
		val t = (renderTime - spawnTime) / 1000_000_000f
		val velocityMultiplier = emitter.dynamics.velocityMultiplierY
		if (velocityMultiplier == 1f) return initialY + t * initialVelocityY + t * t * 0.5f * accelerationY

		val steadyVelocity = velocityMultiplier * accelerationY / (1f - velocityMultiplier)
		val lnMultiplier = ln(velocityMultiplier)
		return initialY + t * steadyVelocity + (initialVelocityY - steadyVelocity) *
				velocityMultiplier.pow(t) / lnMultiplier -
				(initialVelocityY - steadyVelocity) / lnMultiplier
	}

	fun computeRotation(renderTime: Long): Float {
		val t = (renderTime - spawnTime) / 1000_000_000f
		return initialRotation + t * emitter.dynamics.spin
	}
}
