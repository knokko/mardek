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

	// load the data for the character to render
	private val assetCharacter = context.campaign.characterSelection.party[tab.partyIndex]!!

	private lateinit var kim1Batch: KimBatch
	private lateinit var kim2Batch: KimBatch
	var printedCount = 0;

	///////////////////////
	// Health Bar Position
	///////////////////////
	val healthBarX = region.minX + IMAGE_SIZE + 15 + MARGIN_LEFT
	val startY = region.minY + MARGIN_TOP + 40 // Offset from the top
	//////////////////////
	// Mana Bar Position
	//////////////////////
	val manaBarX = healthBarX + baseBarWidth + 15

	override fun beforeRendering()
	{
		// for element images import
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
		// index seems never used but removing it cause the gamee to crash
		for ((index, character) in context.campaign.characterSelection.party.withIndex())
		{
			if (character == null || printedCount >= 4) continue
			drawRectangles()
			renderAlternateWins()
			winSelector()
			printedCount++
		}
	}

	////////////////////////////////////
	// Drawing the outer rectangles
	// Drawing character portrait
	// Wins shared content
	//
	// TODO : match the background color
	///////////////////////////////////

	public fun drawRectangles()
	{
		// Colors
		val borderColor = srgbToLinear(rgb(254, 225, 123))
		val innerColor = srgbToLinear(rgb(0, 0, 0))

		val borderThickness = 3
		
		// applying the margin and vertical offset for each box.
		val offsetY = printedCount * (RECT_HEIGHT + 20)
		val rectY = region.minY + MARGIN_TOP + offsetY

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
		// rendering the character image
		render_character_portrait()
	}

	public fun renderAlternateWins()
	{
		val squareSize = 8
		val squareSpacing = 5
		
		val offsetY = printedCount * (RECT_HEIGHT + 20)

		val rectTopY = region.minY + MARGIN_TOP + offsetY
		val startX = region.maxX - (squareSize + squareSpacing) * tab.squaresCount - 10

		for (i in 0 until tab.squaresCount)
		{
			val squareX = startX + i * (squareSize + squareSpacing)

			// Decide square color based on whether this square is active
			val fillColor = if (i == tab.activeValue) {
				srgbToLinear(rgb(254, 225, 123)) // Active (highlighted) color
			} else {
				srgbToLinear(rgb(80, 80, 80)) // Inactive color
			}

			// Draw the square
			context.uiRenderer.fillColor(
				squareX, rectTopY - 10,
				squareX + squareSize, rectTopY - 10 + squareSize,
				fillColor,
				Gradient(0, 0, squareSize, squareSize, fillColor, fillColor, fillColor)
			)
		}
	}

	public fun render_character_portrait()
	{
		val offsetY = printedCount * (RECT_HEIGHT + 20)
		val rectX = region.minX + 3
		val rectY = region.minY + MARGIN_TOP + offsetY
		val imageY = rectY + (RECT_HEIGHT - IMAGE_SIZE) / 2
		val imagePlaceholderColor = srgbToLinear(rgb(150, 150, 150))
		// Draw the placeholder for the character portrait
		context.uiRenderer.fillColor(
			rectX, imageY, rectX + IMAGE_SIZE, imageY + IMAGE_SIZE,
			imagePlaceholderColor,
			Gradient(0, 0, IMAGE_SIZE, IMAGE_SIZE, imagePlaceholderColor, imagePlaceholderColor, imagePlaceholderColor)
		)
	}

	////////////////////////////////////
	// Logic for the window selection
	/////////////////////////////////////

	public fun renderConditionWin()
	{
		drawStatBars()
		renderNameAndClass()	
		renderElement()
	}

	public fun winSelector()
	{
		if (tab.activeValue == 0)
		{
			renderConditionWin()
		}
		// if (tab.activeValue == 1)
		// {
		// 	renderVitalStatistics()
		// }
		// if (tab.activeValue == 2)
		// {
		// 	renderElementalResistances()
		// }
		// if (tab.activeValue == 3)
		// {
		// 	renderStatusResistances()
		// }
		// if (tab.activeValue == 4)
		// {
		// 	renderGrowth()
		// }
		// if (tab.activeValue == 5)
		// {
		// 	renderPerformance1()
		// }
		// if (tab.activeValue == 6)
		// {
		// 	renderPerformance2()
		// }
	}

	////////////////////////////////////
	// Condition Win Rendering
	// Health & Mana Display | OK 
	//
	// TODO : fix name & class string position
	////////////////////////////////////

	public fun drawStatBars()
	{
		val assetCharacter = context.campaign.characterSelection.party[printedCount]!!
		val characterState = context.campaign.characterStates[assetCharacter] ?: throw IllegalStateException("Missing state for $assetCharacter")
		////////////////////////////
		// Size of the stat bars
		////////////////////////////
		// val assetCharacter = state.characterSelection.party[printedCount]!!
		/////////////////////////////
		val offsetY = printedCount * (RECT_HEIGHT + 20)
		
		val maxHealth = characterState.determineMaxHealth(assetCharacter.baseStats, characterState.activeStatusEffects)

		context.uiRenderer.drawString(
			context.resources.font,
			"HP",
			srgbToLinear(rgb(254, 225, 123)),
			intArrayOf(),
			healthBarX - 30, 
			startY + offsetY + MARGIN * 2 - 75,
			healthBarX,
			startY + offsetY + MARGIN * 2 + BASE_BAR_HEIGHT,
			startY + offsetY + MARGIN * 2 + 10,
			14,
			1,
			TextAlignment.RIGHT
		)

		val healthRenderer = ResourceBarRenderer(context, ResourceType.Health, AbsoluteRectangle(
			healthBarX + 5, 
			startY + offsetY + MARGIN * 2 - 5, 
			baseBarWidth * 2, 
			BASE_BAR_HEIGHT - 5
		))
		
		//////////////////////////
		// Mana Bar Rendering
		//////////////////////////
		
		val maxMana = characterState.determineMaxMana(assetCharacter.baseStats, characterState.activeStatusEffects)

		context.uiRenderer.drawString(
			context.resources.font,
			"MP",
			srgbToLinear(rgb(254, 225, 123)),
			intArrayOf(),
			manaBarX + baseBarWidth - 20, 
			startY + offsetY + MARGIN * 2 - 50,
			manaBarX + baseBarWidth + 20, 
			startY + offsetY + MARGIN * 2 + BASE_BAR_HEIGHT,
			startY + offsetY + MARGIN * 2 + 10,
			14,
			1,
			TextAlignment.RIGHT
		)

		healthRenderer.renderBar(characterState.currentHealth, maxHealth)
		healthRenderer.renderTextOverBar(characterState.currentHealth, maxHealth)

		// Rendering the Mana Bar
		val manaRenderer = ResourceBarRenderer(context, ResourceType.Mana, AbsoluteRectangle(
			manaBarX + baseBarWidth + 30, 
			startY + MARGIN * 2 + offsetY - 5, 
			baseBarWidth * 2 - 30, 
			BASE_BAR_HEIGHT - 8
		))

		manaRenderer.renderBar(characterState.currentMana, maxMana)
		manaRenderer.renderTextOverBar(characterState.currentMana, maxMana)

		val darkColor = srgbToLinear(rgb(36, 26, 16))
		val borderColor = srgbToLinear(rgb(254, 225, 123))

		///////////////////////
		// Xp Bar Position
		///////////////////////

		val XpBarX = manaBarX + IMAGE_SIZE + BASE_BAR_WIDTH * 2 + MARGIN_LEFT
		context.uiRenderer.drawString(
			context.resources.font,
			"XP",
			srgbToLinear(rgb(254, 225, 123)),
			intArrayOf(),
			XpBarX - 10,
			startY + offsetY + MARGIN * 2 - 50,
			XpBarX + 20, 
			startY + offsetY + MARGIN * 2 + BASE_BAR_HEIGHT,
			startY + offsetY + MARGIN * 2 + 10,
			14,
			1,
			TextAlignment.RIGHT
		)
		// Rendering Xp Bar
		context.uiRenderer.fillColor(
			XpBarX + 25, 
			startY + MARGIN * 2 + offsetY,
			XpBarX + BASE_BAR_WIDTH, 
			startY + BASE_BAR_HEIGHT + 5 + offsetY,
			darkColor,
			Gradient(
				0, 0, BASE_BAR_WIDTH / 2, BASE_BAR_HEIGHT, 
				darkColor, darkColor, darkColor
			)
		)
	}

	public fun renderNameAndClass()
	{
		val borderColor = srgbToLinear(rgb(254, 225, 123))
		val assetCharacter = context.campaign.characterSelection.party[printedCount]!!
		val barX = region.minX + IMAGE_SIZE
		val offsetY = printedCount * (RECT_HEIGHT + 20)
		val startY = region.minY + MARGIN_TOP + 20// Offset from the top
		val characterState = context.campaign.characterStates[assetCharacter] ?: throw IllegalStateException("Missing state for $assetCharacter")

		/////////////////
		// Name Render
		/////////////////
		context.uiRenderer.drawString(
			context.resources.font, // font used to render the string
			assetCharacter.name, // the string content
			borderColor, // color of the text
			intArrayOf(), // optional outline color (?)
			barX + 20, region.minY + offsetY, // top left corner of the text area
			barX + BASE_BAR_WIDTH + 50, // bottom right x
			region.maxY, // bottom right y
			startY + offsetY + 15, // y position where the baseline of the text starts
			20, // Size of the string
			1, // scale of the text
			TextAlignment.CENTER // text alingment in the box
		)

		////////////////
		// Lvl Render
		////////////////
		context.uiRenderer.drawString(
			context.resources.font, 
			"Lv${characterState.currentLevel}", 
			borderColor,
			intArrayOf(),
			manaBarX, region.minY + offsetY,  // top left corner of the text area
			manaBarX + BASE_BAR_WIDTH * 2 + 50, 
			region.maxY,
			startY + offsetY + 15, 
			18,
			1,
			TextAlignment.CENTER
		)

		//////////////////
		// Class Render
		//////////////////
		context.uiRenderer.drawString(
			context.resources.font, 
			"${assetCharacter.characterClass}",
			borderColor, intArrayOf(),
			manaBarX, region.minY + offsetY,
			manaBarX + BASE_BAR_WIDTH * 4 + 50,
			region.maxY,
			startY + offsetY + 15,
			18, 
			1,
			TextAlignment.CENTER
		)

		// Draw "Condition:" placeholder
		// context.uiRenderer.drawString(
		// 	context.resources.font,
		// 	"Condition",
		// 	borderColor,
		// 	intArrayOf(),
		// 	barX + BASE_BAR_WIDTH + 50,
		// 	region.minY + offsetY - 200,
		// 	barX + BASE_BAR_WIDTH * 2 + 50, // Width of text box
		// 	region.maxY + offsetY - 200,
		// 	startY + MARGIN + offsetY, 5,
		// 	1,
		// 	TextAlignment.LEFT
		// )
	}

	////////////////////////
	// Character's Element
	////////////////////////
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