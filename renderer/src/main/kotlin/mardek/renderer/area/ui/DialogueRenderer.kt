package mardek.renderer.area.ui

import com.github.knokko.boiler.utilities.ColorPacker.*
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dFancyTextBatch
import com.github.knokko.vk2d.batch.Vk2dImageBatch
import com.github.knokko.vk2d.batch.Vk2dOvalBatch
import com.github.knokko.vk2d.batch.Vk2dSimpleTextBatch
import com.github.knokko.vk2d.text.Vk2dFont
import com.github.knokko.vk2d.text.HarfbuzzChecks.assertHbSuccess
import com.github.knokko.vk2d.text.Vk2dTextStyle
import com.github.knokko.vk2d.text.TextAlignment
import mardek.content.action.*
import mardek.renderer.MardekTextStyles
import mardek.renderer.RenderContext
import mardek.renderer.animation.AnimationContext
import mardek.renderer.animation.AnimationPartBatch
import mardek.renderer.animation.renderPortraitAnimation
import mardek.renderer.area.AreaRenderContext
import mardek.renderer.menu.referenceTime
import mardek.renderer.util.renderBoxButton
import mardek.renderer.util.renderInnerBoxButton
import mardek.state.ingame.actions.CampaignActionsState
import mardek.state.ingame.area.AreaSuspensionActions
import mardek.state.util.Rectangle
import org.joml.Matrix3x2f
import org.lwjgl.system.MemoryStack
import org.lwjgl.util.harfbuzz.HarfBuzz.hb_buffer_add_utf16
import org.lwjgl.util.harfbuzz.HarfBuzz.hb_buffer_clear_contents
import org.lwjgl.util.harfbuzz.HarfBuzz.hb_buffer_get_glyph_infos
import org.lwjgl.util.harfbuzz.HarfBuzz.hb_buffer_get_glyph_positions
import org.lwjgl.util.harfbuzz.HarfBuzz.hb_buffer_guess_segment_properties
import org.lwjgl.util.harfbuzz.HarfBuzz.hb_shape
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.time.Duration

private const val CHOICE_CHAR = '•'

internal class DialogueRenderContext(
	val actionNode: ActionNode,
	val choiceOptions: List<ChoiceEntry>,
	val selectedChoice: Int,
	val region: Rectangle,
	val shownDialogueCharacters: Float,
	val defaultDialogueObject: ActionTargetData?,
	val context: RenderContext,
	val showChatLog: Boolean,

	val uiColorBatch: Vk2dColorBatch,
	val ovalBatch: Vk2dOvalBatch,
	val dialogueElementBatch: Vk2dImageBatch,
	val portraitBatch: AnimationPartBatch,
	val simpleTextBatch: Vk2dSimpleTextBatch,
	val fancyTextBatch: Vk2dFancyTextBatch,
)

internal fun renderCampaignDialogue(
	actions: CampaignActionsState, region: Rectangle, context: RenderContext
): Pair<Vk2dColorBatch, Vk2dSimpleTextBatch> {
	val dialogueContext = DialogueRenderContext(
		actionNode = actions.node,
		choiceOptions = emptyList(),
		selectedChoice = 0,
		region = region,
		shownDialogueCharacters = actions.shownDialogueCharacters,
		defaultDialogueObject = null,
		context = context,
		showChatLog = actions.showChatLog,

		uiColorBatch = context.addColorBatch(200),
		ovalBatch = context.addOvalBatch(40),
		dialogueElementBatch = context.addImageBatch(2),
		portraitBatch = context.addAnimationPartBatch(100),
		simpleTextBatch = context.addTextBatch(5000),
		fancyTextBatch = context.addFancyTextBatch(2),
	)

	renderDialogue(dialogueContext)

	return Pair(dialogueContext.uiColorBatch, dialogueContext.simpleTextBatch)
}

internal fun renderAreaDialogue(areaContext: AreaRenderContext) {
	val suspension = areaContext.state.suspension
	val actions = if (suspension is AreaSuspensionActions) suspension.actions else return
	val actionNode = actions.node ?: return

	val dialogueContext = DialogueRenderContext(
		actionNode = actionNode,
		choiceOptions = actions.choiceOptions,
		selectedChoice = actions.selectedChoice,
		region = areaContext.region,
		shownDialogueCharacters = actions.shownDialogueCharacters,
		defaultDialogueObject = actions.defaultDialogueObject,
		context = areaContext.context,
		showChatLog = actions.showChatLog,
		uiColorBatch = areaContext.uiColorBatch,
		ovalBatch = areaContext.ovalBatch,
		dialogueElementBatch = areaContext.dialogueElementBatch,
		simpleTextBatch = areaContext.simpleTextBatch,
		fancyTextBatch = areaContext.fancyTextBatch,
		portraitBatch = areaContext.portraitBatch,
	)
	renderDialogue(dialogueContext)
}

private fun renderDialogue(dialogueContext: DialogueRenderContext) {
	dialogueContext.run {
		var talkAction: ActionTalk? = null

		if (actionNode is FixedActionNode) {
			val action = actionNode.action
			if (action is ActionTalk) talkAction = action
			if (action is ActionParallel) {
				for (parallelAction in action.actions) {
					if (parallelAction is ActionTalk) talkAction = parallelAction
				}
			}
		}

		if (actionNode is ChoiceActionNode) {
			val combinedText = choiceOptions.withIndex().joinToString("\n") {
				var text = "$CHOICE_CHAR " + it.value.text
				if (it.index == selectedChoice) text = "$$text%"
				text
			}
			talkAction = ActionTalk(
				actionNode.speaker, choiceOptions[selectedChoice].expression, combinedText
			)
		}

		if (talkAction == null) return

		val portrait = when (val speaker = talkAction.speaker) {
			is ActionTargetPlayer -> speaker.player.portraitInfo
			is ActionTargetPartyMember -> context.campaign.party[speaker.index]?.portraitInfo
			is ActionTargetAreaCharacter -> speaker.character.portrait
			is ActionTargetDefaultDialogueObject -> defaultDialogueObject?.portraitInfo
			is ActionTargetCustom -> speaker.data.portraitInfo
			else -> null
		}

		val textRegionHeight = region.height / 4
		val textRegion = Rectangle(
			region.minX, region.boundY - textRegionHeight,
			region.width, textRegionHeight,
		)

		if (portrait != null) {
			val magicScale = region.height / 250f

			val renderWidth = 60f * magicScale
			val renderHeight = 62f * magicScale

			val renderX = textRegion.minX + renderWidth
			val renderY = textRegion.minY - renderHeight

			val animationContext = AnimationContext(
				renderRegion = Rectangle(region.minX, region.minY, region.width, region.height - textRegionHeight),
				renderTime = System.nanoTime(),
				magicScale = context.content.portraits.magicScale,
				parentMatrix = Matrix3x2f().translate(renderX, renderY).scale(-magicScale, magicScale),
				parentColorTransform = null,
				partBatch = portraitBatch,
				noMask = context.content.battle.noMask,
				combat = null,
				portrait = portrait,
				currentChapter = context.campaign.story.evaluate(context.content.story.fixedVariables.chapter) ?: 0,
				portraitExpression = talkAction.expression,
				dialogueLine = talkAction.text,
				shownDialogueCharacters = shownDialogueCharacters,
				animationDuration = Duration.ZERO,
			)
			renderPortraitAnimation(context.content.portraits.animations, animationContext)

			val background = portrait.elementalBackground
			if (background != null) {

				val radius = 0.19f * region.height
				ovalBatch.complex(
					region.minX, textRegion.minY - (0.3f * region.height).roundToInt(),
					region.minX + (0.35f * region.height).roundToInt(), textRegion.minY - 1,
					region.minX + 0.14f * region.height, textRegion.minY - 0.055f * region.height,
					radius, radius,
					multiplyAlpha(background.color, 0.15f),
					0, 0, 0, 0,
					1f, 2f, 2f, 2f,
				)
			}
		}

		run {
			val highColor = srgbToLinear(rgba(109, 91, 52, 242))
			val lowColor = srgbToLinear(rgba(24, 14, 10, 230))
			uiColorBatch.gradient(
				textRegion.minX, textRegion.minY, textRegion.maxX, textRegion.maxY,
				lowColor, lowColor, highColor,
			)
		}

		run {
			val lineColor = srgbToLinear(rgb(208, 193, 142))
			val offset = region.height / 200
			val lineWidth = max(1, region.height / 500)

			val minX = textRegion.minX + offset
			val minY = textRegion.minY + offset
			val maxX = textRegion.maxX - offset
			val maxY = textRegion.maxY - offset
			uiColorBatch.fill(minX, minY, maxX, minY + lineWidth - 1, lineColor)
			uiColorBatch.fill(minX, minY, minX + lineWidth - 1, maxY, lineColor)
			uiColorBatch.fill(minX, maxY + 1 - lineWidth, maxX, maxY, lineColor)
			uiColorBatch.fill(maxX + 1 - lineWidth, minY, maxX, maxY, lineColor)
		}

		val nameRegionHeight = region.height / 12
		val nameRegion = Rectangle(
			region.minX, textRegion.minY - nameRegionHeight,
			region.width, nameRegionHeight,
		)

		run {
			val backgroundColor = srgbToLinear(rgba(24, 14, 10, 230))
			val borderColor = srgbToLinear(rgb(73, 52, 37))
			val lineWidth = max(1, region.height / 400)
			val diagonalX1 = nameRegion.maxX - nameRegion.height * 3
			val diagonalHeight = region.height / 25
			val diagonalX2 = diagonalX1 + diagonalHeight
			uiColorBatch.fill(
				nameRegion.minX, nameRegion.minY, diagonalX1 - 1,
				nameRegion.minY + lineWidth - 1, borderColor,
			)
			uiColorBatch.fill(
				nameRegion.minX, nameRegion.boundY - lineWidth,
				nameRegion.maxX, nameRegion.maxY, borderColor,
			)
			uiColorBatch.fill(
				nameRegion.minX, nameRegion.minY + lineWidth,
				nameRegion.maxX, nameRegion.maxY - lineWidth, backgroundColor,
			)
			uiColorBatch.fill(
				diagonalX2, nameRegion.minY - diagonalHeight,
				nameRegion.maxX, nameRegion.minY + lineWidth - 1 - diagonalHeight, borderColor,
			)
			uiColorBatch.fillUnaligned(
				diagonalX1, nameRegion.minY + lineWidth,
				diagonalX2, nameRegion.minY + lineWidth + 1 - diagonalHeight,
				diagonalX2, nameRegion.minY - diagonalHeight,
				diagonalX1, nameRegion.minY, borderColor,
			)
			uiColorBatch.fillUnaligned(
				diagonalX1 + 1, nameRegion.minY + lineWidth,
				nameRegion.boundX, nameRegion.minY + lineWidth,
				nameRegion.boundX, nameRegion.minY + lineWidth - diagonalHeight,
				diagonalX2 + 1, nameRegion.minY + lineWidth - diagonalHeight, backgroundColor
			)

			if (showChatLog) {
				val weakerBackgroundColor = multiplyAlpha(backgroundColor, 0.9f)
				uiColorBatch.fillUnaligned(
					region.minX, nameRegion.minY,
					diagonalX1, nameRegion.minY,
					diagonalX2, nameRegion.minY - diagonalHeight,
					region.minX, nameRegion.minY - diagonalHeight,
					weakerBackgroundColor,
				)
				uiColorBatch.fill(
					region.minX, region.minY, region.maxX, nameRegion.minY - diagonalHeight - 1,
					weakerBackgroundColor
				)
				renderChatLog(dialogueContext)
			}
		}

		run {
			val boxY = nameRegion.minY - nameRegion.height * 3 / 8
			val boxSize = nameRegion.height * 3 / 8
			val boxRadius = nameRegion.height * 3 / 28
			val borderWidth = max(1, boxSize / 30)
			val boxColor = srgbToLinear(rgb(88, 71, 47))
			val cornerDistances = floatArrayOf(0.85f, 0.9f, 1.0f, 1.05f)

			renderInnerBoxButton(
				uiColorBatch, ovalBatch, simpleTextBatch, fancyTextBatch, context.bundle, context.content.fonts,
				nameRegion.maxX - nameRegion.height * 10 / 4, boxY,
				boxSize, borderWidth, boxRadius, cornerDistances, boxColor, "Q", "Skip",
			)
			renderInnerBoxButton(
				uiColorBatch, ovalBatch, simpleTextBatch, fancyTextBatch, context.bundle, context.content.fonts,
				nameRegion.maxX - nameRegion.height * 5 / 4, boxY,
				boxSize, borderWidth, boxRadius, cornerDistances, boxColor, "L", "Log",
			)
		}

		if (shownDialogueCharacters >= talkAction.text.length || actionNode is ChoiceActionNode) {
			val minBoxSize = textRegion.height * 0.24f
			val maxBoxSize = textRegion.height * 0.26f
			val boxSizePeriod = 1_000_000_000L
			val relativeTime = ((System.nanoTime() - referenceTime) % boxSizePeriod).toFloat() / boxSizePeriod
			val floatBoxSize = minBoxSize + (2f * abs(0.5f - relativeTime)) * (maxBoxSize - minBoxSize)
			val boxSize = floatBoxSize.roundToInt()
			val boxOffset = (textRegion.height * 0.03f + minBoxSize + 0.5f * (boxSize - minBoxSize)).roundToInt()
			val boxX = textRegion.maxX - boxOffset
			val boxY = textRegion.maxY - boxOffset
			renderBoxButton(
				uiColorBatch, ovalBatch, simpleTextBatch, fancyTextBatch, context.bundle, context.content.fonts,
				minBoxSize, boxX, boxY
			)
		}

		run {
			val speakerElement = talkAction.speaker.getElement(
				defaultDialogueObject, context.campaign.party
			)

			if (speakerElement != null) {
				val image = speakerElement.thinSprite
				val desiredHeight = 0.8f * textRegion.height
				val scale = desiredHeight / image.height

				// TODO CHAP2 Find a less ugly way to handle this
				val alpha = if (speakerElement.rawName == "DARK") 0.4f else 0.15f

				dialogueElementBatch.coloredScale(
					textRegion.maxX - 1.2f * desiredHeight,
					textRegion.maxY - 0.9f * textRegion.height,
					scale, image.index, 0,
					rgba(1f, 1f, 1f, alpha),
				)
			}
		}

		run {
			val displayName = talkAction.speaker.getDisplayName(
				defaultDialogueObject, context.campaign.party
			)
			if (displayName != null) {
				val shadowOffset = nameRegion.height * 0.04f
				val textColor = srgbToLinear(rgb(238, 203, 127))
				val shadowColor = srgbToLinear(rgb(90, 65, 38))
				val font = context.bundle.getFont(context.content.fonts.basic2.index)
				val baseX = nameRegion.minX + region.height * if (portrait == null) 0.05f else 0.325f
				simpleTextBatch.drawShadowedString(
					displayName, baseX, nameRegion.maxY - nameRegion.height * 0.3f,
					nameRegion.height * 0.45f, font, textColor, 0, 0f,
					shadowColor, shadowOffset, TextAlignment.LEFT,
				)
			}
		}

		run {
			var font = context.bundle.getFont(context.content.fonts.basic2.index)

			var baseStyle = MardekTextStyles.Dialogue.BASE
			var boldStyle = MardekTextStyles.Dialogue.BOLD
			var shadowStyle: Vk2dTextStyle? = MardekTextStyles.Dialogue.SHADOW

			if (actionNode is ChoiceActionNode) {
				baseStyle = MardekTextStyles.Dialogue.UNSELECTED
				boldStyle = MardekTextStyles.Dialogue.SELECTED
			}

			if (portrait != null) {
				val voice = portrait.voiceStyle
				if (voice != null) {
					font = context.bundle.getFont(voice.font.index)
					baseStyle = MardekTextStyles.Dialogue.custom(voice)
					boldStyle = baseStyle
					shadowStyle = null
				}
			}

			val text = talkAction.text

			val baseTextX = textRegion.minX + textRegion.height * 0.1f
			val maxTextX = textRegion.maxX - textRegion.height * 0.1f
			val minTextY = textRegion.minY + textRegion.height * 0.2f
			val textHeight = textRegion.height * 0.1f

			var shownCharacters = shownDialogueCharacters
			if (actionNode is ChoiceActionNode) shownCharacters = talkAction.text.length.toFloat()

			val implicitLineSpacing = 0.18f * textRegion.height
			val explicitLineSpacing = 0.2f * textRegion.height

			renderDialogueLines(
				text, shownCharacters, baseTextX, baseTextX, maxTextX,
				minTextY, textRegion.maxY.toFloat(),
				textHeight, implicitLineSpacing, explicitLineSpacing, simpleTextBatch,
				font, baseStyle, boldStyle, shadowStyle,
			)
		}
	}
}

internal fun renderDialogueLines(
	text: String, shownCharacters: Float,
	minTextX: Float, firstTextX: Float, maxTextX: Float,
	minTextY: Float, maxLineY: Float, textHeight: Float,
	implicitLineSpacing: Float, explicitLineSpacing: Float,
	textBatch: Vk2dSimpleTextBatch, font: Vk2dFont,
	baseStyle: Vk2dTextStyle, boldStyle: Vk2dTextStyle, shadowStyle: Vk2dTextStyle?,
) : Float {
	hb_buffer_clear_contents(textBatch.cache.hbBuffer)

	MemoryStack.stackPush().use { stack ->
		val textBytes = stack.UTF16(text, false)
		hb_buffer_add_utf16(textBatch.cache.hbBuffer, textBytes, 0, textBytes.capacity())
	}

	hb_buffer_guess_segment_properties(textBatch.cache.hbBuffer)
	hb_shape(font.hbFont, textBatch.cache.hbBuffer, null)

	val glyphInfos = assertHbSuccess(
		hb_buffer_get_glyph_infos(textBatch.cache.hbBuffer),
		"buffer_get_glyph_infos"
	)!!
	val glyphOffsets = assertHbSuccess(
		hb_buffer_get_glyph_positions(textBatch.cache.hbBuffer),
		"buffer_get_glyph_positions"
	)!!

	var textX = firstTextX
	var textY = minTextY

	var remaining = shownCharacters

	var isBold = false
	val shadowOffset = 0.125f * textHeight
	val baseStyleIndex = textBatch.cache.getStyleIndex(baseStyle)
	val boldStyleIndex = textBatch.cache.getStyleIndex(boldStyle)
	var shadowStyleIndex = 0
	if (shadowStyle != null) shadowStyleIndex = textBatch.cache.getStyleIndex(shadowStyle)
	for (glyphIndex in 0 until glyphInfos.limit()) {
		if (remaining <= 0f) break
		val charIndex = glyphInfos[glyphIndex].cluster()
		val nextChar = text[charIndex]

		var peekX = textX
		for (peekGlyphIndex in glyphIndex until glyphInfos.limit()) {
			val peekChar = text[glyphInfos[peekGlyphIndex].cluster()]
			if (peekChar == ' ' || peekChar == '\n') break
			if (peekChar == '%' || peekChar == '$') continue

			peekX += font.getGlyphAdvanceX(glyphOffsets[peekGlyphIndex], textHeight)
			if (peekX > maxTextX) {
				textX = minTextX
				textY += implicitLineSpacing
				break
			}
		}

		if (nextChar == '\n') {
			textX = minTextX
			textY += explicitLineSpacing
			continue
		}

		if (nextChar == '$') {
			isBold = true
			continue
		}

		if (nextChar == '%') {
			isBold = false
			continue
		}

		if (textY > maxLineY) break
		val (textStyle, styleIndex) = if (isBold) Pair(boldStyle, boldStyleIndex)
		else Pair(baseStyle, baseStyleIndex)
		val glyph = glyphInfos[glyphIndex].codepoint()

		val atlas = font.chooseAtlas(textHeight, textStyle.stroke.width, glyph)
		if (shadowStyle != null) {
			textBatch.glyphAt(
				textX + shadowOffset, textY + shadowOffset, glyphOffsets[glyphIndex],
				atlas, textHeight, glyph, shadowStyleIndex,
			)
		}
		textBatch.glyphAt(
			textX, textY, glyphOffsets[glyphIndex], atlas,
			textHeight, glyph, styleIndex,
		)

		textX += font.getGlyphAdvanceX(glyphOffsets[glyphIndex], textHeight)

		remaining -= 1f
	}

	return textY
}
