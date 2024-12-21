package mardek.renderer.ui.tabs

import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import com.github.knokko.boiler.utilities.ColorPacker.*
import com.github.knokko.ui.renderer.Gradient
import com.github.knokko.ui.renderer.UiRenderer
import mardek.renderer.KimBatch
import mardek.renderer.KimRenderer
import mardek.renderer.KimRequest
import mardek.renderer.SharedResources
import mardek.state.ingame.CampaignState
import mardek.state.ingame.menu.InventoryTab
import mardek.state.title.AbsoluteRectangle
import kotlin.math.max
import kotlin.math.min

private const val CHARACTER_BAR_HEIGHT = 23
private const val CHARACTER_BAR_WIDTH = 120
private const val SIMPLE_SLOT_SIZE = 18
private const val EQUIPMENT_SLOT_SIZE = 20

private const val BASE_WIDTH = 3 + 6 * EQUIPMENT_SLOT_SIZE + CHARACTER_BAR_WIDTH
private const val BASE_HEIGHT = 3 + 4 * CHARACTER_BAR_HEIGHT + 8 * SIMPLE_SLOT_SIZE

private val LINE_COLOR = srgbToLinear(rgb(179, 162, 116))
private val LIGHT_SLOT_COLOR = srgbToLinear(rgb(100, 80, 48))
private val DARK_SLOT_COLOR = srgbToLinear(rgb(74, 48, 30))

class InventoryTabRenderer(
	private val recorder: CommandRecorder,
	private val targetImage: VkbImage,
	private val frameIndex: Int,

	private val tab: InventoryTab,
	private val region: AbsoluteRectangle,
	private val state: CampaignState,
	private val resources: SharedResources,
) : TabRenderer() {

	private val scale = max(1, min(region.width / BASE_WIDTH, region.height / BASE_HEIGHT))

	private lateinit var kimBatch: KimBatch

	override fun beforeRendering() {
		this.kimBatch = resources.kimRenderer.startBatch()
		renderItemGrid(null, resources.kimRenderer)
		renderCharacterBars(null, resources.kimRenderer)
	}

	override fun render() {
		val uiRenderer = resources.uiRenderers[frameIndex]
		renderItemGrid(uiRenderer, null)
		renderCharacterBars(uiRenderer, null)
	}

	override fun postUiRendering() {
		resources.kimRenderer.submit(kimBatch, recorder, targetImage, frameIndex)
	}

	private fun renderItemGrid(uiRenderer: UiRenderer?, kimRenderer: KimRenderer?) {
		val fullSlotSize = scale * SIMPLE_SLOT_SIZE
		val size = 8 * fullSlotSize + 2
		val startX = region.boundX - size
		val startY = region.maxY - size - 2 * scale

		uiRenderer?.fillColor(startX, startY, region.maxX, startY, LINE_COLOR)
		for (row in 0 until 8) {
			val minY = 1 + startY + row * fullSlotSize
			val maxY = minY + fullSlotSize - 1
			uiRenderer?.fillColor(startX, minY, region.maxX, maxY, LINE_COLOR, Gradient(
				2, 1, size - 4, fullSlotSize - 2,
				LIGHT_SLOT_COLOR, LIGHT_SLOT_COLOR, DARK_SLOT_COLOR
			))
		}
		uiRenderer?.fillColor(startX, startY + size - 1, region.maxX, startY + size - 1, LINE_COLOR)

		for (column in 1 until 8) {
			val x = startX + column * fullSlotSize
			uiRenderer?.fillColor(x, startY + 2, x + 1, startY + 8 * fullSlotSize - 1, LINE_COLOR)
		}

		val hoveredItem = tab.hoveringItem
		if (hoveredItem != null) {
			val hoverLineColor = srgbToLinear(rgb(165, 205, 254))
			val hoverLightColor = srgbToLinear(rgb(25, 68, 118))
			val hoverDarkColor = srgbToLinear(rgb(64, 43, 36))
			if (hoveredItem.slotIndex >= 0) {
				val slotX = hoveredItem.slotIndex % 8
				val slotY = hoveredItem.slotIndex / 8

				val x = startX + 1 + slotX * fullSlotSize
				val y = startY + 1 + slotY * fullSlotSize
				uiRenderer?.fillColor(x, y, x + fullSlotSize - 1, y + fullSlotSize - 1, hoverLineColor, Gradient(
					1, 1, fullSlotSize - 2, fullSlotSize - 2,
					hoverLightColor, hoverLightColor, hoverDarkColor
				)
				)
			}
		}

		val pickedItem = tab.pickedUpItem

		if (kimRenderer != null) {
			val party = state.characterSelection.party
			val selectedCharacter = party[tab.partyIndex]
			if (selectedCharacter != null) {
				val inventory = state.characterStates[selectedCharacter]!!.inventory
				for (y in 0 until 8) {
					for (x in 0 until 8) {
						val itemStack = inventory[x + 8 * y] ?: continue
						if (pickedItem != null && itemStack === pickedItem.get()) continue
						kimBatch.requests.add(KimRequest(
							x = startX + 1 + fullSlotSize * x + scale,
							y = startY + 1 + fullSlotSize * y + scale,
							scale = scale, sprite = itemStack.item.sprite, opacity = 1f
						))
					}
				}
			}

			if (pickedItem != null && tab.mouseX >= 0 && tab.mouseY >= 0) {
				kimBatch.requests.add(KimRequest(
					x = tab.mouseX, y = tab.mouseY, scale = scale,
					sprite = pickedItem.get()!!.item.sprite, opacity = 1f
				))
			}
		}

		tab.renderItemsStartX = startX + 1
		tab.renderItemsStartY = startY + 1
		tab.renderItemSlotSize = fullSlotSize
	}

	private fun renderCharacterBars(uiRenderer: UiRenderer?, kimRenderer: KimRenderer?) {
		for (index in state.characterSelection.party.indices) {
			renderCharacterBar(index, uiRenderer, kimRenderer)
		}
	}

	private fun renderCharacterBar(partyIndex: Int, uiRenderer: UiRenderer?, kimRenderer: KimRenderer?) {
		val assetCharacter = state.characterSelection.party[partyIndex] ?: return
		val characterState = state.characterStates[assetCharacter] ?: throw IllegalStateException("Missing state for $assetCharacter")
		val barHeight = scale * CHARACTER_BAR_HEIGHT
		val startX = region.minX + 5 * scale
		val startY = region.minY + 3 * scale + partyIndex * barHeight
		val width = region.boundX - startX

		val lineColor = srgbToLinear(rgb(165, 151, 110))
		val outerLightColor = srgbToLinear(rgb(89, 72, 42))
		val outerRightColor = srgbToLinear(rgb(104, 80, 47))
		val outerDarkColor = srgbToLinear(rgb(39, 26, 16))
		uiRenderer?.fillColor(startX, startY, region.maxX, startY + barHeight - 1, lineColor, Gradient(
			2, 2, width - 4, barHeight - 3,
			outerLightColor, outerRightColor, outerDarkColor
		))

		val innerDarkColor = srgbToLinear(rgb(113, 88, 58))
		val innerLightColor = srgbToLinear(rgb(119, 105, 91))
		val margin = 3 * scale
		uiRenderer?.fillColor(
			startX + margin, startY + margin, region.maxX - margin,
			startY + barHeight * 2 / 3, innerDarkColor, Gradient(
				0, 0, region.width, barHeight, innerDarkColor, innerDarkColor, innerLightColor
			)
		)

		val characterX = startX + margin + margin / 2
		val characterY = startY + margin + scale
		if (kimRenderer != null) {
			kimBatch.requests.add(KimRequest(
				x = characterX, y = characterY, scale = scale,
				sprite = assetCharacter.areaSprites.sprites[0], opacity = 1f // TODO Animate
			))
		}

		val x1 = characterX + 16 * scale + margin / 2

		// TODO Get color from character element
		val elementColor = srgbToLinear(rgb(65, 149, 47))
		val highElementColor = rgba(red(elementColor), green(elementColor), blue(elementColor), 200.toByte())
		uiRenderer?.fillColor(
			x1, startY + margin, characterX + 80 * scale,
			startY + margin + 6 * scale, highElementColor, Gradient(
				0, 0, region.width, region.height, elementColor, 0, highElementColor
			)
		)

		val x2 = x1 + 30 * scale
		val x3 = x2 + 16 * scale
		val x4 = x3 + 8 * scale

		val barX = x4 + 20 * scale

		val textColor = srgbToLinear(rgb(238, 204, 127))
		val textHeight = 5 * scale
		uiRenderer?.drawString(
			resources.font, assetCharacter.name, textColor, intArrayOf(),
			x1 + margin / 2, region.minY, x3, region.maxY, startY + margin + 5 * scale, textHeight, 1
		)
		uiRenderer?.drawString(
			resources.font, "Lv${characterState.currentLevel}", textColor, intArrayOf(),
			x3, region.minY, barX, region.maxY, startY + margin + 5 * scale, textHeight, 1
		)

		// TODO proper health & mana bar
		val baseBarWidth = 40 * scale
		val barsHeight = margin
		val bottomHealthColor = srgbToLinear(rgb(75, 179, 42))
		val topHealthColor = srgbToLinear(rgb(182, 229, 163))
		val rightHealthColor = srgbToLinear(rgb(26, 89, 45))
		uiRenderer?.fillColor(
			barX, startY + margin * 13 / 8, barX + baseBarWidth,
			startY + margin * 13 / 8 + barsHeight, bottomHealthColor, Gradient(
				0, 0, baseBarWidth, barsHeight, bottomHealthColor, rightHealthColor, topHealthColor
			)
		)

		val healthTextColor = srgbToLinear(rgb(122, 217, 62))
		uiRenderer?.drawString(
			resources.font, "${characterState.currentHealth}/1234", healthTextColor, intArrayOf(),
			barX + margin, region.minY, barX + baseBarWidth, region.maxY,
			startY + margin * 34 / 8, margin * 3 / 2, 1
		)

		val bottomManaColor = srgbToLinear(rgb(8, 122, 178))
		val topManaColor = srgbToLinear(rgb(152, 204, 230))
		val rightManaColor = srgbToLinear(rgb(12, 207, 159))
		uiRenderer?.fillColor(
			barX, startY + margin * 37 / 8, barX + baseBarWidth,
			startY + margin * 37 / 8 + barsHeight, bottomManaColor, Gradient(
				0, 0, baseBarWidth, barsHeight, bottomManaColor, rightManaColor, topManaColor
			)
		)

		val manaTextColor = srgbToLinear(rgb(35, 227, 240))
		uiRenderer?.drawString(
			resources.font, "${characterState.currentMana}/1234", manaTextColor, intArrayOf(),
			barX + margin, region.minY, barX + baseBarWidth, region.maxY,
			startY + margin * 59 / 8, margin * 3 / 2, 1
		)

		renderEquipment(partyIndex, startY + margin - 1, uiRenderer, kimRenderer)
	}

	private fun renderEquipment(partyIndex: Int, startY: Int, uiRenderer: UiRenderer?, kimRenderer: KimRenderer?) {
		val assetCharacter = state.characterSelection.party[partyIndex] ?: return
		val characterState = state.characterStates[assetCharacter] ?: throw IllegalStateException("Missing state for $assetCharacter")
		val equipment = characterState.equipment

		val startX = region.maxX - 6 * scale * EQUIPMENT_SLOT_SIZE

		val largeSlotSize = scale * EQUIPMENT_SLOT_SIZE
		val slotSize = scale * SIMPLE_SLOT_SIZE

		val lineColor = srgbToLinear(rgb(208, 193, 142))
		for ((column, item) in equipment.withIndex()) {
			val minX = startX + column * largeSlotSize
			val maxX = minX + slotSize - 1
			val maxY = startY + slotSize - 1
			uiRenderer?.fillColor(minX, startY, maxX, startY, lineColor)
			uiRenderer?.fillColor(minX, maxY, maxX, maxY, lineColor)
			uiRenderer?.fillColor(minX, startY, minX, maxY, lineColor)
			uiRenderer?.fillColor(maxX, startY, maxX, maxY, lineColor)
			if (kimRenderer != null && item != null) kimBatch.requests.add(KimRequest(
				x = minX + scale, y = startY + scale, scale = scale, sprite = item.sprite, opacity = 1f
			))
		}
	}
}
