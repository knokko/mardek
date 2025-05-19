package mardek.renderer.ui

import com.github.knokko.boiler.utilities.ColorPacker.*
import com.github.knokko.text.placement.TextAlignment
import com.github.knokko.ui.renderer.Gradient
import mardek.renderer.InGameRenderContext
import mardek.renderer.ui.tabs.*
import mardek.state.ingame.menu.*

class InGameMenuRenderer(
	private val context: InGameRenderContext,
	private val menu: InGameMenuState,
) {

	private lateinit var tabRenderer: TabRenderer

	fun beforeRendering() {
		val transform = CoordinateTransform.create(
			SpaceLayout.Simple, context.targetImage.width, context.targetImage.height
		)
		val tab = menu.currentTab
		val tabRegion = transform.transform(0f, 0.08f, 0.78f, 0.84f)
		if (tab is PartyTab) this.tabRenderer = PartyTabRenderer()
		if (tab is SkillsTab) this.tabRenderer = SkillsTabRenderer(context, tab, tabRegion)
		if (tab is InventoryTab) this.tabRenderer = InventoryTabRenderer(context, tab, tabRegion)
		if (tab is MapTab) this.tabRenderer = MapTabRenderer(context, tabRegion)

		tabRenderer.beforeRendering()
	}

	fun render() {
		context.uiRenderer.beginBatch()
		val transform = CoordinateTransform.create(
			SpaceLayout.Simple, context.targetImage.width, context.targetImage.height
		)

		val barColor = srgbToLinear(rgb(24, 14, 10))
		val upperBar = transform.transform(0f, 0.92f, 1f, 1f)
		val lowerBar = transform.transform(0f, 0f, 1f, 0.08f)

		context.uiRenderer.fillColor(upperBar.minX, upperBar.minY, upperBar.maxX, upperBar.maxY, barColor)
		context.uiRenderer.fillColor(lowerBar.minX, lowerBar.minY, lowerBar.maxX, lowerBar.maxY, barColor)

		val leftColor = srgbToLinear(rgba(54, 37, 21, 240))
		val rightColor = srgbToLinear(rgba(132, 84, 53, 240))
		context.uiRenderer.fillColor(
			lowerBar.minX, upperBar.boundY, lowerBar.maxX, lowerBar.minY - 1, barColor,
			Gradient(
				0, 0, lowerBar.width, lowerBar.minY - upperBar.boundY,
				leftColor, rightColor, leftColor
			)
		)

		val area = context.campaign.currentArea
		if (area != null) {
			context.uiRenderer.drawString(
				context.resources.font, area.area.properties.displayName, srgbToLinear(rgb(238, 203, 127)),
				IntArray(0), transform.transformX(0.025f), lowerBar.minY, lowerBar.maxX / 2, lowerBar.maxY,
				transform.transformY(0.025f), transform.transformHeight(0.03f), 1, TextAlignment.LEFT
			)
		}

		context.uiRenderer.drawString(
			context.resources.font, menu.currentTab.getText(), srgbToLinear(rgb(132, 81, 37)),
			IntArray(0), transform.transformX(0.025f), upperBar.minY, upperBar.maxX / 2, upperBar.maxY,
			transform.transformY(0.945f), transform.transformHeight(0.035f), 1, TextAlignment.LEFT
		)

		renderTabName(transform, "Party", 0)
		renderTabName(transform, "Skills", 1)
		renderTabName(transform, "Inventory", 2)
		renderTabName(transform, "Map", 3)

		if (menu.currentTab.inside) {
			val tabNamesRegion = transform.transform(0.79f, 0.08f, 0.21f, 0.92f)
			val cover = rgba(0, 0, 0, 150)
			context.uiRenderer.fillColor(tabNamesRegion.minX, tabNamesRegion.minY, tabNamesRegion.maxX, tabNamesRegion.maxY, cover)
		}

		tabRenderer.render()

		context.uiRenderer.endBatch()

		tabRenderer.postUiRendering()
	}

	private fun renderTabName(transform: CoordinateTransform, text: String, index: Int) {
		val rect = transform.transform(0.8f, 0.85f - index * 0.07f, 0.2f, 0.06f)
		if (rect.width < 5) return

		var goldTint = srgbToLinear(rgba(111, 92, 53, 183))
		if (text == menu.currentTab.getText()) {
			goldTint = srgbToLinear(rgba(6, 81, 156, 182))
		}

		context.uiRenderer.fillColor(
			rect.minX, (rect.minY + rect.maxY) / 2, rect.maxX, rect.maxY, 0,
			Gradient(0, 0, rect.width, rect.height / 2, 0, goldTint, 0)
		)

		var upperTextColor = srgbToLinear(rgb(239, 237, 210))
		var lowerTextColor = srgbToLinear(rgb(214, 170, 98))

		if (text == menu.currentTab.getText()) {
			upperTextColor = srgbToLinear(rgb(231, 255, 255))
			lowerTextColor = srgbToLinear(rgb(104, 180, 253))
		}

		context.uiRenderer.drawString(
			context.resources.font, text, lowerTextColor, IntArray(0),
			rect.minX, rect.minY, rect.maxX - transform.transformWidth(0.01f), rect.maxY,
			rect.maxY - rect.height / 5, transform.transformHeight(0.04f), 1, TextAlignment.RIGHT,
			Gradient(0, 0, rect.width, 3 * rect.height / 5, upperTextColor, upperTextColor, upperTextColor)
		)
	}
}
