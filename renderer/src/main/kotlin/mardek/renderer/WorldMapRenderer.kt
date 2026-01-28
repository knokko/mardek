package mardek.renderer

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dGlyphBatch
import com.github.knokko.vk2d.text.TextAlignment
import mardek.content.area.Direction
import mardek.state.ingame.worldmap.WorldMapState
import mardek.state.util.Rectangle
import org.joml.Math.atan2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

internal fun renderWorldMap(
	context: RenderContext, mapState: WorldMapState, region: Rectangle
): Pair<Vk2dColorBatch, Vk2dGlyphBatch?> {
	val mapSprite = mapState.map.sprite
	val sourceAspect = mapSprite.width.toFloat() / mapSprite.height
	val destinationAspect = region.width.toFloat() / region.height

	val (offsetX, offsetY, mapScale) = if (sourceAspect >= destinationAspect) {
		val scale = region.width.toFloat() / mapSprite.width
		val renderHeight = scale * mapSprite.height
		Triple(region.minX, region.minY + (0.5f * (region.height - renderHeight)).toInt(), scale)
	} else {
		val scale = region.height.toFloat() / mapSprite.height
		val renderWidth = scale * mapSprite.width
		Triple((region.minX + 0.5f * (region.width - renderWidth)).toInt(), region.minY, scale)
	}

	val backgroundImageBatch = context.addImageBatch(2)
	backgroundImageBatch.simpleScale(offsetX.toFloat(), offsetY.toFloat(), mapScale, mapSprite.index)

	val colorBatch = context.addColorBatch(300) // should be enough for nearly 50 edges

	for (edge in mapState.map.edges) {
		if (context.campaign.story.evaluate(edge.node1.wasDiscovered) == null) continue
		if (context.campaign.story.evaluate(edge.node2.wasDiscovered) == null) continue
		val fillColor = srgbToLinear(rgb(185, 168, 130))
		val borderColor = srgbToLinear(rgb(104, 89, 57))

		val edgeWidth = 15.0
		val angle = atan2(edge.node2.y.toDouble() - edge.node1.y, edge.node2.x.toDouble() - edge.node1.x)
		val leftAngle = angle + 0.25 * PI
		val rightAngle = angle - 0.25 * PI
		val rawX1 = edge.node1.x + 0.5 * edgeWidth * cos(leftAngle)
		val rawY1 = edge.node1.y + 0.5 * edgeWidth * sin(leftAngle)
		val rawX2 = edge.node1.x + 0.5 * edgeWidth * cos(rightAngle)
		val rawY2 = edge.node1.y + 0.5 * edgeWidth * sin(rightAngle)
		val rawX3 = edge.node2.x + 0.5 * edgeWidth * cos(rightAngle)
		val rawY3 = edge.node2.y + 0.5 * edgeWidth * sin(rightAngle)
		val rawX4 = edge.node2.x + 0.5 * edgeWidth * cos(leftAngle)
		val rawY4 = edge.node2.y + 0.5 * edgeWidth * sin(leftAngle)

		val x1 = offsetX + (mapScale * rawX1).roundToInt()
		val y1 = offsetY + (mapScale * rawY1).roundToInt()
		val x2 = offsetX + (mapScale * rawX2).roundToInt()
		val y2 = offsetY + (mapScale * rawY2).roundToInt()
		val x3 = offsetX + (mapScale * rawX3).roundToInt()
		val y3 = offsetY + (mapScale * rawY3).roundToInt()
		val x4 = offsetX + (mapScale * rawX4).roundToInt()
		val y4 = offsetY + (mapScale * rawY4).roundToInt()
		colorBatch.fillUnaligned(x1, y1, x2, y2, x3, y3, x4, y4, borderColor)

		val borderWidth = 0.3
		colorBatch.fillUnaligned(
			x1 + (borderWidth * (x2 - x1)).roundToInt(), y1 + (borderWidth * (y2 - y1)).roundToInt(),
			x2 + (borderWidth * (x1 - x2)).roundToInt(), y2 + (borderWidth * (y1 - y2)).roundToInt(),
			x3 + (borderWidth * (x4 - x3)).roundToInt(), y3 + (borderWidth * (y4 - y3)).roundToInt(),
			x4 + (borderWidth * (x3 - x4)).roundToInt(), y4 + (borderWidth * (y3 - y4)).roundToInt(),
			fillColor,
		)
	}

	val overlayImageBatch = context.addImageBatch(100) // should be enough for nearly 50 nodes
	val nextNode = mapState.nextNode
	for (node in mapState.map.nodes) {
		if (context.campaign.story.evaluate(node.wasDiscovered) == null) continue
		var nodeSprite = context.content.ui.worldMapDiscoveredArea
		if (nextNode == null && node === mapState.currentNode) {
			nodeSprite = context.content.ui.worldMapCurrentArea
		}

		val desiredSize = 0.0225f * mapScale * mapSprite.width
		overlayImageBatch.simpleScale(
			offsetX + mapScale * node.x - desiredSize * 0.5f,
			offsetY + mapScale * node.y - desiredSize * 0.5f,
			desiredSize / nodeSprite.width, nodeSprite.index,
		)
	}

	val spriteBatch = context.addKim3Batch(2)
	val characterSprites = context.campaign.usedPartyMembers().first().character.areaSprites
	var rawCharacterX = mapState.currentNode.x.toDouble()
	var rawCharacterY = mapState.currentNode.y.toDouble()
	var direction = Direction.Down
	if (nextNode != null) {
		val progress = (mapState.currentTime - nextNode.startTime) / (nextNode.arrivalTime - nextNode.startTime)
		rawCharacterX = (1.0 - progress) * rawCharacterX + progress * nextNode.destination.x
		rawCharacterY = (1.0 - progress) * rawCharacterY + progress * nextNode.destination.y
		direction = Direction.bestDelta(
			nextNode.destination.x - mapState.currentNode.x,
			nextNode.destination.y - mapState.currentNode.y,
		) ?: direction
	}

	val baseCharacterX = offsetX + (mapScale * rawCharacterX).roundToInt()
	val baseCharacterY = offsetY + (mapScale * rawCharacterY).roundToInt()
	val characterScale = max(1, (region.height / 275f).roundToInt())
	var characterSprite = characterSprites.sprites[2 * direction.ordinal]
	val walkingAnimationPeriod = 750_000_000L
	if ((mapState.currentTime.inWholeNanoseconds % walkingAnimationPeriod) >= walkingAnimationPeriod / 2) {
		characterSprite = characterSprites.sprites[2 * direction.ordinal + 1]
	}
	spriteBatch.simple(
		baseCharacterX - characterScale * characterSprite.width / 2,
		baseCharacterY - characterScale * characterSprite.height,
		characterScale, characterSprite.index,
	)

	val textBatch = if (nextNode == null) {
		val areaNameScroll = context.content.ui.worldMapScroll
		overlayImageBatch.simpleScale(
			offsetX + 0.02f * mapScale * mapSprite.width,
			offsetY + 0.9f * mapScale * mapSprite.height,
			0.0725f * mapScale * mapSprite.height / areaNameScroll.height,
			areaNameScroll.index,
		)

		val textBatch = context.addTextBatch(100)
		val font = context.bundle.getFont(context.content.fonts.basic2.index)
		val textColor = srgbToLinear(rgb(90, 77, 54))
		textBatch.drawString(
			mapState.currentNode.entrances[0].area.properties.displayName,
			offsetX + 0.14f * mapScale * mapSprite.width,
			offsetY + 0.94f * mapScale * mapSprite.height,
			0.02f * mapScale * mapSprite.height,
			font, textColor, TextAlignment.CENTERED,
		)
		textBatch
	} else null

	var inverseFadeIn = 1.0
	if (mapState.currentTime < WorldMapState.FADE_DURATION) {
		inverseFadeIn = mapState.currentTime / WorldMapState.FADE_DURATION
	}

	var inverseFadeOut = 1.0
	val exiting = mapState.exiting
	if (exiting != null) {
		inverseFadeOut = (exiting.exitAt - mapState.currentTime) / WorldMapState.FADE_DURATION
	}

	val fade = 1f - (inverseFadeIn * inverseFadeOut).toFloat()
	if (fade > 0.001f) {
		context.addColorBatch(2).fill(
			region.minX, region.minY, region.maxX, region.maxY,
			rgba(0f, 0f, 0f, fade),
		)
	}

	return Pair(colorBatch, textBatch)
}
