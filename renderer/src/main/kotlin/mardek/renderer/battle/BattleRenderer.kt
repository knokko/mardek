package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.*
import com.github.knokko.ui.renderer.Gradient
import mardek.content.animations.BattleModel
import mardek.content.animations.ColorTransform
import mardek.content.battle.PartyLayoutPosition
import mardek.renderer.InGameRenderContext
import mardek.state.ingame.battle.BattleState
import mardek.state.ingame.battle.CombatantState
import mardek.state.title.AbsoluteRectangle
import org.joml.Matrix3x2f
import org.joml.Vector2f
import java.lang.Math.toIntExact

class BattleRenderer(context: InGameRenderContext, battleState: BattleState) {

	private val context = BattleRenderContext(battleState, context)

	private val turnOrderRenderer = TurnOrderRenderer(this.context, AbsoluteRectangle(
		minX = 0, minY = context.targetImage.height / 12,
		width = context.targetImage.width, height = context.targetImage.height / 12
	))
	private val actionBarRenderer = ActionBarRenderer(this.context, AbsoluteRectangle(
		minX = 0, minY = context.targetImage.height - context.targetImage.height / 12 - context.targetImage.height / 8,
		width = context.targetImage.width, height = context.targetImage.height / 12
	))
	private val skillOrItemSelectionRenderer = SkillOrItemSelectionRenderer(this.context, AbsoluteRectangle(
		minX = context.targetImage.width / 3, minY = context.targetImage.height / 5,
		width = context.targetImage.width / 3, height = 4 * context.targetImage.height / 7
	))
	private val skillOrItemDescriptionRenderer = SkillOrItemDescriptionRenderer(this.context, AbsoluteRectangle(
		minX = 0, minY = context.targetImage.height / 12,
		width = context.targetImage.width, height = context.targetImage.height / 9
	))
	private val targetSelectionRenderer = TargetSelectionRenderer(this.context, AbsoluteRectangle(
		minX = 0, minY = context.targetImage.height / 12, width = context.targetImage.width,
		height = context.targetImage.height - context.targetImage.height / 8 - context.targetImage.height / 12
	))
	private val enemyBlockRenderers = mutableListOf<EnemyBlockRenderer>()
	private val playerBlockRenderers = mutableListOf<PlayerBlockRenderer>()

	private fun selectedColorTransform(intensity: Float) = ColorTransform(
		addColor = rgba(0f, 0f, 0.5f * intensity, 0f),
		multiplyColor = rgb(1f - 0.5f * intensity, 1f - 0.5f * intensity, 1f - 0.5f * intensity)
	)

	private fun selectedColorTransform(state: CombatantState): ColorTransform? {
		if (state.lastPointedTo == 0L) return null

		val blinkTime = 500_000_000L
		val passedTime = System.nanoTime() - state.lastPointedTo
		if (passedTime >= blinkTime) return null

		return selectedColorTransform(1f - passedTime.toFloat() / blinkTime)
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
		val multipleColor = rgba(
			normalize(red(base.multiplyColor)) * normalize(red(top.multiplyColor)),
			normalize(green(base.multiplyColor)) * normalize(green(top.multiplyColor)),
			normalize(blue(base.multiplyColor)) * normalize(blue(top.multiplyColor)),
			normalize(alpha(base.multiplyColor)) * normalize(alpha(top.multiplyColor)),
		)
		return ColorTransform(addColor = addColor, multiplyColor = multipleColor)
	}

	fun beforeRendering() {
		turnOrderRenderer.beforeRendering()
		actionBarRenderer.beforeRendering()
		skillOrItemSelectionRenderer.beforeRendering()
		skillOrItemDescriptionRenderer.beforeRendering()
		targetSelectionRenderer.beforeRendering()

		for ((index, enemy) in context.battle.enemies.withIndex()) {
			if (enemy == null) continue
			val region = AbsoluteRectangle(
				minX = index * context.targetImage.width / 4, minY = 0,
				width = context.targetImage.width / 4, height = context.targetImage.height / 12
			)
			enemyBlockRenderers.add(EnemyBlockRenderer(context, enemy, context.battle.enemyStates[index]!!, region))
		}

		for ((index, player) in context.battle.players.withIndex()) {
			if (player == null) continue
			val region = AbsoluteRectangle(
				minX = index * context.targetImage.width / 4,
				minY = context.targetImage.height - context.targetImage.height / 8,
				width = context.targetImage.width / 4, height = context.targetImage.height / 8
			)
			playerBlockRenderers.add(PlayerBlockRenderer(
				context, player, context.battle.playerStates[index]!!,
				context.campaign.characterStates[player]!!, region
			))
		}

		for (blockRenderer in enemyBlockRenderers) blockRenderer.beforeRendering()
		for (blockRenderer in playerBlockRenderers) blockRenderer.beforeRendering()
	}

	fun render() {
		val relativeTime = System.nanoTime() - context.battle.startTime
		context.resources.partRenderer.startBatch(context.recorder)
		context.resources.partRenderer.render(context.battle.battle.background.sprite, arrayOf(
			Vector2f(-1f, -1f), Vector2f(1f, -1f), Vector2f(1f, 1f), Vector2f(-1f, 1f)
		), null)

		for ((index, enemy) in context.battle.enemies.withIndex()) {
			if (enemy == null) continue

			val rawPosition = context.battle.battle.enemyLayout.positions[index]
			val colorTransform = selectedColorTransform(context.battle.enemyStates[index]!!)
			renderCreature(rawPosition, enemy.monster.model, -1f, relativeTime, colorTransform)
		}

		for ((index, player) in context.battle.players.withIndex()) {
			if (player == null) continue

			val rawPosition = context.battle.playerLayout.positions[index]
			val colorTransform = selectedColorTransform(context.battle.playerStates[index]!!)
			renderCreature(rawPosition, player.battleModel, 1f, relativeTime, colorTransform)
		}

		context.resources.partRenderer.endBatch()
		context.uiRenderer.beginBatch()
		val leftColor = srgbToLinear(rgba(90, 76, 44, 200))
		val rightColor = srgbToLinear(rgba(38, 28, 17, 200))
		context.uiRenderer.fillColor(
			0, 0, context.targetImage.width, context.targetImage.height / 12, 0, Gradient(
				0, 0, context.targetImage.width, context.targetImage.height, leftColor, rightColor, leftColor
			)
		)
		context.uiRenderer.fillColor(
			0, context.targetImage.height - context.targetImage.height / 8,
			context.targetImage.width, context.targetImage.height, 0, Gradient(
				0, 0, context.targetImage.width, context.targetImage.height, rightColor, leftColor, rightColor
			)
		)
		context.uiRenderer.endBatch()

		turnOrderRenderer.render()
		actionBarRenderer.render()
		skillOrItemSelectionRenderer.render()
		skillOrItemDescriptionRenderer.render()
		targetSelectionRenderer.render()
		for (blockRenderer in enemyBlockRenderers) blockRenderer.render()
		for (blockRenderer in playerBlockRenderers) blockRenderer.render()
	}

	private fun renderCreature(
		rawPosition: PartyLayoutPosition, model: BattleModel, flipX: Float,
		relativeTime: Long, effectColorTransform: ColorTransform?
	) {
		val coordinates = transformBattleCoordinates(rawPosition, flipX, context.targetImage)
		val animation = model.skeleton.getAnimation("idle")

		val frameLength = 33_000_000L
		val animationLength = animation.frames.size * frameLength
		val modTime = relativeTime % animationLength
		val frameIndex = toIntExact(modTime / frameLength)
		val frame = animation.frames[frameIndex]
		for (animationPart in frame.parts) {
			val matrix = animationPart.matrix
			val (scaleX, scaleY) = if (matrix.hasScale) Pair(matrix.scaleX, matrix.scaleY) else Pair(
				1f,
				1f
			)

			if (animationPart.part.skins.isEmpty()) continue
			val bodyPart = animationPart.part.skins.find { it.name == model.skin } ?:
					animationPart.part.skins.find { it.name == "D_LL" } ?:
					animationPart.part.skins.find { it.name == "D" } ?:
					animationPart.part.skins.first()

			if (bodyPart.name == "unknown") continue
			for (entry in bodyPart.entries) {
				val jomlMatrix = Matrix3x2f(
					scaleX * flipX, matrix.rotateSkew0,
					matrix.rotateSkew1 * flipX, scaleY,
					matrix.translateX * flipX, matrix.translateY
				).translate(entry.offsetX, entry.offsetY)

				val corners = arrayOf(Pair(0f, 0f), Pair(1f, 0f), Pair(1f, 1f), Pair(0f, 1f)).map { rawCorner ->
					val position = jomlMatrix.transformPosition(Vector2f(
						rawCorner.first * entry.sprite.width.toFloat() / entry.scale,
						rawCorner.second * entry.sprite.height.toFloat() / entry.scale
					))

					Vector2f(
						coordinates.x + position.x * coordinates.scaleX,
						coordinates.y + position.y * coordinates.scaleY
					)
				}.toTypedArray()

				val colorTransform = mergeColorTransforms(animationPart.color, effectColorTransform)
				context.resources.partRenderer.render(entry.sprite, corners, colorTransform)
			}
		}
	}
}
