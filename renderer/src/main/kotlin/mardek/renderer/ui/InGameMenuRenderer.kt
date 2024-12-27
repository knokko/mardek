package mardek.renderer.ui

import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import com.github.knokko.boiler.utilities.ColorPacker.*
import com.github.knokko.text.placement.TextAlignment
import com.github.knokko.ui.renderer.Gradient
import com.github.knokko.ui.renderer.UiRenderer
import mardek.renderer.SharedResources
import mardek.renderer.ui.tabs.*
import mardek.state.ingame.InGameState
import mardek.state.ingame.menu.InventoryTab
import mardek.state.ingame.menu.MapTab
import mardek.state.ingame.menu.PartyTab
import mardek.state.ingame.menu.SkillsTab

class InGameMenuRenderer(
	private val recorder: CommandRecorder,
	private val targetImage: VkbImage,
	private val frameIndex: Int,
	private val resources: SharedResources,
	private val state: InGameState,
) {

	private lateinit var tabRenderer: TabRenderer

	fun beforeRendering() {
		val transform = CoordinateTransform.create(SpaceLayout.Simple, targetImage.width, targetImage.height)
		val tab = state.menu.currentTab
		val tabRegion = transform.transform(0f, 0.08f, 0.78f, 0.84f)
		if (tab is PartyTab) this.tabRenderer = PartyTabRenderer()
		if (tab is SkillsTab) this.tabRenderer = SkillsTabRenderer()
		if (tab is InventoryTab) this.tabRenderer = InventoryTabRenderer(
			recorder, targetImage, frameIndex, tab, tabRegion, state.campaign, state.assets, resources
		)
		if (tab is MapTab) this.tabRenderer = MapTabRenderer()

		tabRenderer.beforeRendering()
	}

	fun render(uiRenderer: UiRenderer) {
		uiRenderer.beginBatch()
		val transform = CoordinateTransform.create(SpaceLayout.Simple, targetImage.width, targetImage.height)

		val barColor = srgbToLinear(rgb(24, 14, 10))
		val upperBar = transform.transform(0f, 0.92f, 1f, 1f)
		val lowerBar = transform.transform(0f, 0f, 1f, 0.08f)

		uiRenderer.fillColor(upperBar.minX, upperBar.minY, upperBar.maxX, upperBar.maxY, barColor)
		uiRenderer.fillColor(lowerBar.minX, lowerBar.minY, lowerBar.maxX, lowerBar.maxY, barColor)

		val leftColor = srgbToLinear(rgba(54, 37, 21, 240))
		val rightColor = srgbToLinear(rgba(132, 84, 53, 240))
		uiRenderer.fillColor(
			lowerBar.minX, upperBar.boundY, lowerBar.maxX, lowerBar.minY - 1, barColor,
			Gradient(
				0, 0, lowerBar.width, lowerBar.minY - upperBar.boundY,
				leftColor, rightColor, leftColor
			)
		)

		val area = state.campaign.currentArea
		if (area != null) {
			uiRenderer.drawString(
				resources.font, area.area.properties.displayName, srgbToLinear(rgb(238, 203, 127)),
				IntArray(0), transform.transformX(0.025f), lowerBar.minY, lowerBar.maxX / 2, lowerBar.maxY,
				transform.transformY(0.025f), transform.transformHeight(0.03f), 1, TextAlignment.LEFT
			)
		}

		uiRenderer.drawString(
			resources.font, state.menu.currentTab.getText(), srgbToLinear(rgb(132, 81, 37)),
			IntArray(0), transform.transformX(0.025f), upperBar.minY, upperBar.maxX / 2, upperBar.maxY,
			transform.transformY(0.945f), transform.transformHeight(0.035f), 1, TextAlignment.LEFT
		)

		renderTabName(uiRenderer, transform, "Party", 0)
		renderTabName(uiRenderer, transform, "Skills", 1)
		renderTabName(uiRenderer, transform, "Inventory", 2)
		renderTabName(uiRenderer, transform, "Map", 3)

		if (state.menu.currentTab.inside) {
			val tabNamesRegion = transform.transform(0.79f, 0.08f, 0.21f, 0.92f)
			val cover = rgba(0, 0, 0, 150)
			uiRenderer.fillColor(tabNamesRegion.minX, tabNamesRegion.minY, tabNamesRegion.maxX, tabNamesRegion.maxY, cover)
		}

		tabRenderer.render()

		uiRenderer.endBatch()

		tabRenderer.postUiRendering()
	}

	private fun renderTabName(renderer: UiRenderer, transform: CoordinateTransform, text: String, index: Int) {
		val rect = transform.transform(0.8f, 0.85f - index * 0.07f, 0.2f, 0.06f)

		var goldTint = srgbToLinear(rgba(111, 92, 53, 183))
		if (text == state.menu.currentTab.getText()) {
			goldTint = srgbToLinear(rgba(6, 81, 156, 182))
		}

		renderer.fillColor(
			rect.minX, (rect.minY + rect.maxY) / 2, rect.maxX, rect.maxY, 0,
			Gradient(0, 0, rect.width, rect.height / 2, 0, goldTint, 0)
		)

		var upperTextColor = srgbToLinear(rgb(239, 237, 210))
		var lowerTextColor = srgbToLinear(rgb(214, 170, 98))

		if (text == state.menu.currentTab.getText()) {
			upperTextColor = srgbToLinear(rgb(231, 255, 255))
			lowerTextColor = srgbToLinear(rgb(104, 180, 253))
		}

		renderer.drawString(
			resources.font, text, lowerTextColor, IntArray(0),
			rect.minX, rect.minY, rect.maxX - transform.transformWidth(0.01f), rect.maxY,
			rect.maxY - rect.height / 5, transform.transformHeight(0.04f), 1, TextAlignment.RIGHT,
			Gradient(0, 0, rect.width, 3 * rect.height / 5, upperTextColor, upperTextColor, upperTextColor)
		)
	}
}
