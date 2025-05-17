package mardek.renderer.ui.tabs

import mardek.renderer.InGameRenderContext

import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import com.github.knokko.boiler.utilities.ColorPacker.*
import com.github.knokko.text.placement.TextAlignment
import com.github.knokko.ui.renderer.Gradient
import com.github.knokko.ui.renderer.UiRenderer
import mardek.renderer.SharedResources
import mardek.content.Content
import mardek.renderer.batch.KimBatch
import mardek.renderer.batch.Kim1Renderer
import mardek.renderer.batch.KimRequest
import mardek.state.ingame.CampaignState
import mardek.renderer.ui.renderDescription
import mardek.renderer.ui.ResourceBarRenderer
import mardek.renderer.ui.ResourceType
import mardek.renderer.ui.renderDescription
import mardek.state.ingame.menu.PartyTab
import mardek.state.title.AbsoluteRectangle
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt


///////////////
// Constants //
//////////////////////////////////////////
private const val PARTY_BAR_HEIGHT = 30;
private const val BAR_MARGIN = 5;

// private const val BASE_WIDTH = 500;
// private const val BASE_HEIGHT = 100;

private const val SCALE = 2;

// margin of the main containers
private const val MARGIN_TOP = 20;
private const val MARGIN_LEFT = 20;
// size of the main containers
private const val RECT_WIDTH = 500;
private const val RECT_HEIGHT = 80;

// Placeholder portrait image
private const val IMAGE_SIZE = 75;

private const val BASE_BAR_WIDTH = 40 * SCALE
private const val BASE_BAR_HEIGHT = 20
private const val MARGIN = 10

private const val BASE_WIDTH = 400
private const val BASE_HEIGHT = 400
/////////////////////////////////////////

class PartyTabRenderer(
	private val context: InGameRenderContext,
	private val tab: PartyTab,
	private val region: AbsoluteRectangle,
): TabRenderer()
{
	private val scale = max(1, min(region.width / BASE_WIDTH, region.height / BASE_HEIGHT))
	private val baseBarWidth = 40 * scale * 2

	private val assetCharacter = context.campaign.characterSelection.party[tab.partyIndex]!!

	private lateinit var kim1Batch: KimBatch
	private lateinit var kim2Batch: KimBatch
	var printedCount = 0;

	override fun beforeRendering()
	{
		// this.kim1Batch = resources.kim1Renderer.startBatch()
		// this.kim2Batch = resources.kim2Renderer.startBatch()
		this.kim1Batch = context.resources.kim1Renderer.startBatch()
		this.kim2Batch = context.resources.kim2Renderer.startBatch()
	}

	override fun render()
	{
		context.uiRenderer.beginBatch()
		mainProcedure()
		context.uiRenderer.endBatch()
		context.resources.kim1Renderer.submit(kim1Batch, context.recorder, context.targetImage)
		context.resources.kim2Renderer.submit(kim2Batch, context.recorder, context.targetImage)
	}

	public fun mainProcedure()
	{
		// Iterate through the party characters using withIndex()
		for ((index, character) in context.campaign.characterSelection.party.withIndex())
		{
			if (character == null || printedCount >= 4) continue
			drawRectangles()
			drawStatBars()
			renderNameAndClass()
			renderXp()
			renderElement()
			printedCount++
		}
	}

	public fun drawRectangles()
	{
		// Colors
		val imagePlaceholderColor = srgbToLinear(rgb(150, 150, 150))
		val borderColor = srgbToLinear(rgb(254, 225, 123))
		val innerColor = srgbToLinear(rgb(0, 0, 0))

		// local variables
		val imageMargin = 10
		val borderThickness = 3
		
		// applying the margin and vertical offset for each box.
		val offsetY = printedCount * (RECT_HEIGHT + 20)
		val rectX = region.minX + MARGIN_LEFT
		val rectY = region.minY + MARGIN_TOP + offsetY
		// Position the image box inside the character box (left side)
		val imageX = rectX + imageMargin - 8
		val imageY = rectY + (RECT_HEIGHT - IMAGE_SIZE) / 2

		// Draw the outer rectangle (border)
		context.uiRenderer.fillColor(
		 	region.minX, // starting point
			rectY,
			region.maxX, // finishing point
			rectY + RECT_HEIGHT,
		 	borderColor,
		 	Gradient(0, 0, RECT_WIDTH, RECT_HEIGHT, borderColor, borderColor, borderColor)
		)
		// Draw the inner rectangle (empty area) inset by borderThickness.
		context.uiRenderer.fillColor(
			region.minX + borderThickness,
			rectY + borderThickness,
			region.maxX - borderThickness,
			rectY + RECT_HEIGHT - borderThickness,
			innerColor,
			Gradient(0, 0, RECT_WIDTH - 2 * borderThickness, RECT_HEIGHT - 2 * borderThickness, innerColor, innerColor, innerColor)
		)
		// Draw the placeholder for the character portrait
		context.uiRenderer.fillColor(
			imageX, imageY, imageX + IMAGE_SIZE, imageY + IMAGE_SIZE,
			imagePlaceholderColor,
			Gradient(0, 0, IMAGE_SIZE, IMAGE_SIZE, imagePlaceholderColor, imagePlaceholderColor, imagePlaceholderColor)
		)
	}

	public fun drawStatBars()
	{
		val assetCharacter = context.campaign.characterSelection.party[printedCount]!!
		val characterState = context.campaign.characterStates[assetCharacter] ?: throw IllegalStateException("Missing state for $assetCharacter")
		///////////// Health Bar Color //////////////////////
		// val bottomHealthColor = srgbToLinear(rgb(75, 179, 42))
		// val topHealthColor = srgbToLinear(rgb(182, 229, 163))
		// val rightHealthColor = srgbToLinear(rgb(26, 89, 45))
		// val healthTextColor = srgbToLinear(rgb(122, 217, 62))
		///////////// Mana Bar Color //////////////////////
		// val bottomManaColor = srgbToLinear(rgb(8, 122, 178))
		// val topManaColor = srgbToLinear(rgb(152, 204, 230))
		// val rightManaColor = srgbToLinear(rgb(12, 207, 159))
		// val manaTextColor = srgbToLinear(rgb(100, 200, 255))
		////////////////////////////
		// Size of the stat bars
		////////////////////////////
		// val assetCharacter = state.characterSelection.party[printedCount]!!
		/////////////////////////////
		val barSpacing = 15
		val offsetY = printedCount * (RECT_HEIGHT + 20)
		
		// Placeholder values for the bars
		// val currentMana = 75 
		// val maxMana = 120     
		// val currentHealth = 150   
		//val maxHealth = 200

		val darkColor = srgbToLinear(rgb(39, 26, 16))
		 ///////////////////////
		 // Xp Bar Position
		 ///////////////////////
		 val XpBarX = region.minX + IMAGE_SIZE + BASE_BAR_WIDTH * 2 + MARGIN_LEFT + 50

		val maxHealth = characterState.determineMaxHealth(assetCharacter.baseStats, characterState.activeStatusEffects)

		///////////////////////
		// Health Bar Position
		///////////////////////
		val healthBarX = region.minX + IMAGE_SIZE + barSpacing + MARGIN_LEFT
		val startY = region.minY + MARGIN_TOP + 40// Offset from the top
		//////////////////////
		// Mana Bar Position
		//////////////////////
		val manaBarX = healthBarX + baseBarWidth + barSpacing

		val healthRenderer = ResourceBarRenderer(context, ResourceType.Health, AbsoluteRectangle(
			healthBarX, 
			startY + offsetY + MARGIN * 2, 
			baseBarWidth * 2, 
			BASE_BAR_HEIGHT - 5
		))

		//////////////////////////
		// Mana Bar Rendering
		//////////////////////////

		val maxMana = characterState.determineMaxMana(assetCharacter.baseStats, characterState.activeStatusEffects)

		healthRenderer.renderBar(characterState.currentHealth, maxHealth)
		healthRenderer.renderTextOverBar(characterState.currentHealth, maxHealth)

		// Rendering the Mana Bar
		val manaRenderer = ResourceBarRenderer(context, ResourceType.Mana, AbsoluteRectangle(
			manaBarX + baseBarWidth, 
			startY + MARGIN * 2 + offsetY, 
			baseBarWidth * 2, 
			BASE_BAR_HEIGHT - 5
		))

		manaRenderer.renderBar(characterState.currentMana, maxMana)
		manaRenderer.renderTextOverBar(characterState.currentMana, maxMana)

		// val xpBarX = manaBarX + baseBarWidth * 2


		// Rendering placeholder Xp Bar
		// context.uiRenderer.fillColor(
		// 	XpBarX, startY + MARGIN,
		// 	XpBarX + baseBarWidth, startY,
		// 	darkColor, Gradient(
		// 		0, 0, BASE_BAR_WIDTH / 2, BASE_BAR_HEIGHT, 
		// 		darkColor, darkColor, darkColor
		// 	)
		// )

		// val nextLevel = characterState.

		// val xpRenderer =  ResourceBarRenderer(context, ResourceType.Experience, AbsoluteRectangle(
		// 	xpBarX,
		// 	startY + MARGIN * 2 + offsetY,
		// 	baseBarWidth,
		// 	BASE_BAR_HEIGHT - 5
		// ))

		// xpRenderer.renderBar(characterState.currentLevel, 0)
		// xpRenderer.renderTextOverBar(characterState.currentLevel, 0)

	}

	public fun renderNameAndClass()
	{
		val borderColor = srgbToLinear(rgb(254, 225, 123))
		val assetCharacter = context.campaign.characterSelection.party[printedCount]!!
		val barX = region.minX + IMAGE_SIZE + 15 + MARGIN_LEFT
		val offsetY = printedCount * (RECT_HEIGHT + 20)
		val startY = region.minY + MARGIN_TOP + 40// Offset from the top
		val characterState = context.campaign.characterStates[assetCharacter] ?: throw IllegalStateException("Missing state for $assetCharacter")

		// Health Text
		context.uiRenderer.drawString(
			context.resources.font, assetCharacter.name, borderColor, intArrayOf(),
			barX, region.minY + offsetY, barX + BASE_BAR_WIDTH, region.maxY + offsetY,
			startY + MARGIN + offsetY, MARGIN, 1, TextAlignment.CENTER
		)

		// Health Text
		context.uiRenderer?.drawString(
			context.resources.font, "Lv${characterState.currentLevel}", borderColor, intArrayOf(),
			barX + BASE_BAR_WIDTH, region.minY + offsetY, barX + BASE_BAR_WIDTH * 2, region.maxY + offsetY,
			startY + MARGIN + offsetY, MARGIN, 1, TextAlignment.CENTER
		)

		// Health Text
		context.uiRenderer?.drawString(
			context.resources.font, "${assetCharacter.characterClass}", borderColor, intArrayOf(),
			barX + BASE_BAR_WIDTH + 50, region.minY + offsetY, barX + BASE_BAR_WIDTH * 2 + 50, region.maxY + offsetY,
			startY + MARGIN + offsetY, MARGIN, 1, TextAlignment.CENTER
		)
	}

	public fun renderXp()
	{
		 val offsetY = printedCount * (RECT_HEIGHT + 20)
		 ///////////// Xp Bar Colors //////////////////////
		 // val lineColor = srgbToLinear(rgb(165, 151, 110))
		 // val outerLightColor = srgbToLinear(rgb(89, 72, 42))
		 // val outerRightColor = srgbToLinear(rgb(104, 80, 47))

		
	}

	public fun renderElement()
	{
		val offsetY = printedCount * (RECT_HEIGHT + 20)
		val startY = region.minY + MARGIN_TOP + offsetY
		val assetCharacter = context.campaign.characterSelection.party[printedCount]!!

		val element = assetCharacter.element
	
		kim2Batch.requests.add(KimRequest(
		 	x = region.maxX - BASE_BAR_WIDTH,
			y = startY + BASE_BAR_HEIGHT - 20,
			scale = (scale * (0.8)).toFloat(),
			sprite = element.sprite,
			opacity = 0.8f
		))
	}
}