package mardek.renderer.battle

import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import com.github.knokko.boiler.utilities.ColorPacker.*
import com.github.knokko.ui.renderer.Gradient
import mardek.content.Content
import mardek.content.animations.BattleModel
import mardek.content.animations.ColorTransform
import mardek.content.battle.PartyLayoutPosition
import mardek.renderer.SharedResources
import mardek.state.ingame.CampaignState
import mardek.state.ingame.battle.BattleState
import mardek.state.ingame.battle.CombatantState
import mardek.state.title.AbsoluteRectangle
import org.joml.Matrix3x2f
import org.joml.Vector2f
import java.lang.Math.toIntExact

class BattleRenderer(
	content: Content,
	private val campaign: CampaignState,
	private val recorder: CommandRecorder,
	private val targetImage: VkbImage,
	private val frameIndex: Int,
	private val state: BattleState,
	private val resources: SharedResources,
) {

	private val turnOrderRenderer = TurnOrderRenderer(state, resources, recorder, targetImage, frameIndex, AbsoluteRectangle(
		minX = 0, minY = targetImage.height / 12, width = targetImage.width, height = targetImage.height / 12
	))
	private val actionBarRenderer = ActionBarRenderer(content, campaign, state, resources, recorder, targetImage, frameIndex, AbsoluteRectangle(
		minX = 0, minY = targetImage.height - targetImage.height / 12 - targetImage.height / 8,
		width = targetImage.width, height = targetImage.height / 12
	))
	private val skillOrItemSelectionRenderer = SkillOrItemSelectionRenderer(
		content, resources, frameIndex, state, campaign.characterStates, AbsoluteRectangle(
			minX = targetImage.width / 3, minY = targetImage.height / 5,
			width = targetImage.width / 3, height = 4 * targetImage.height / 7
		), recorder, targetImage
	)
	private val skillOrItemDescriptionRenderer = SkillOrItemDescriptionRenderer(
		content, resources, frameIndex, campaign, state, AbsoluteRectangle(
			minX = 0, minY = targetImage.height / 12, width = targetImage.width, height = targetImage.height / 9
		), recorder, targetImage
	)
	private val targetSelectionRenderer = TargetSelectionRenderer(
		content, state, campaign.characterStates, resources, recorder, targetImage, frameIndex, AbsoluteRectangle(
			minX = 0, minY = targetImage.height / 12, width = targetImage.width,
			height = targetImage.height - targetImage.height / 8 - targetImage.height / 12
		)
	)
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

		for ((index, enemy) in state.battle.enemies.withIndex()) {
			if (enemy == null) continue
			val region = AbsoluteRectangle(
				minX = index * targetImage.width / 4, minY = 0,
				width = targetImage.width / 4, height = targetImage.height / 12
			)
			enemyBlockRenderers.add(EnemyBlockRenderer(
				resources, frameIndex, enemy, state.enemyStates[index]!!, region, recorder, targetImage
			))
		}

		for ((index, player) in state.players.withIndex()) {
			if (player == null) continue
			val region = AbsoluteRectangle(
				minX = index * targetImage.width / 4, minY = targetImage.height - targetImage.height / 8,
				width = targetImage.width / 4, height = targetImage.height / 8
			)
			playerBlockRenderers.add(PlayerBlockRenderer(
				resources, frameIndex, player, state.playerStates[index]!!, campaign.characterStates[player]!!,
				state.startTime, region, recorder, targetImage
			))
		}

		for (blockRenderer in enemyBlockRenderers) blockRenderer.beforeRendering()
		for (blockRenderer in playerBlockRenderers) blockRenderer.beforeRendering()
	}

	fun render() {
		val relativeTime = System.nanoTime() - state.startTime
		resources.partRenderer.startBatch(recorder)
		resources.partRenderer.render(state.battle.background.sprite, arrayOf(
			Vector2f(-1f, -1f), Vector2f(1f, -1f), Vector2f(1f, 1f), Vector2f(-1f, 1f)
		), null)

		for ((index, enemy) in state.battle.enemies.withIndex()) {
			if (enemy == null) continue

			val rawPosition = state.battle.enemyLayout.positions[index]
			val colorTransform = selectedColorTransform(state.enemyStates[index]!!)
			renderCreature(rawPosition, enemy.monster.model, -1f, relativeTime, colorTransform)
		}

		for ((index, player) in state.players.withIndex()) {
			if (player == null) continue

			val rawPosition = state.playerLayout.positions[index]
			val colorTransform = selectedColorTransform(state.playerStates[index]!!)
			renderCreature(rawPosition, player.battleModel, 1f, relativeTime, colorTransform)
		}

		resources.partRenderer.endBatch()
		resources.uiRenderers[frameIndex].beginBatch()
		val leftColor = srgbToLinear(rgba(90, 76, 44, 200))
		val rightColor = srgbToLinear(rgba(38, 28, 17, 200))
		resources.uiRenderers[frameIndex].fillColor(
			0, 0, targetImage.width, targetImage.height / 12, 0, Gradient(
				0, 0, targetImage.width, targetImage.height, leftColor, rightColor, leftColor
			)
		)
		resources.uiRenderers[frameIndex].fillColor(
			0, targetImage.height - targetImage.height / 8, targetImage.width, targetImage.height, 0, Gradient(
				0, 0, targetImage.width, targetImage.height, rightColor, leftColor, rightColor
			)
		)
		resources.uiRenderers[frameIndex].endBatch()

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
		val coordinates = transformBattleCoordinates(rawPosition, flipX, targetImage)
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
				resources.partRenderer.render(entry.sprite, corners, colorTransform)
			}
		}
	}
}
