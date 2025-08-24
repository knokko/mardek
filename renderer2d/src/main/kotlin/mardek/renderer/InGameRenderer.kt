package mardek.renderer

import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import mardek.renderer.area.renderCurrentArea
import mardek.renderer.battle.renderBattle
import mardek.renderer.menu.MenuRenderContext
import mardek.renderer.menu.determineSectionRenderRegion
import mardek.renderer.menu.renderInGameMenu
import mardek.renderer.menu.renderInGameMenuSectionList
import mardek.state.ingame.InGameState
import mardek.state.util.Rectangle

internal fun renderInGame(context: RenderContext, state: InGameState, region: Rectangle): Vk2dColorBatch {

	var titleColorBatch: Vk2dColorBatch? = null
	val area = state.campaign.currentArea
	if (area != null) {
		val battle = area.activeBattle
		if (battle == null) {
			if (state.menu.shown) {
				val framebuffers = context.framebuffers
				val areaRenderStage = context.pipelines.blur.addSourceStage(
					context.frame, framebuffers.blur, -1
				)
				if (state.menu.currentTab.inside) {
					context.currentStage = context.pipelines.blur.addSourceStage(
						context.frame, framebuffers.sectionBlur, -1
					)

					val menuRegion = determineSectionRenderRegion(region)
					val colorBatch = context.addColorBatch(50)
					val textBatch = context.addFancyTextBatch(250)

					// The image/sprite/oval pipelines are not needed for rendering the section list,
					// but we still need a valid instance of MenuRenderContext
					val ovalBatch = context.addOvalBatch(0)
					val imageBatch = context.addImageBatch(0)
					val spriteBatch = context.addKim3Batch(0)

					val menuContext = MenuRenderContext(
						context, colorBatch, ovalBatch, imageBatch, spriteBatch, textBatch,
						state.menu, state.campaign
					)
					renderInGameMenuSectionList(menuContext, Rectangle(
						0, 0, menuRegion.width, menuRegion.height
					))
				}
				val computeStage = context.pipelines.blur.addComputeStage(
					context.frame, context.perFrame.areaBlurDescriptors,
					framebuffers.blur, 9, 50, -1
				)
				if (state.menu.currentTab.inside) {
					computeStage.additional(
						context.perFrame.sectionsBlurDescriptors,
						framebuffers.sectionBlur, 9, 50
					)
				}
				context.currentStage = areaRenderStage

				val areaRenderRegion = Rectangle(0, 0, areaRenderStage.width, areaRenderStage.height)
				renderCurrentArea(context, area, areaRenderRegion)

				context.currentStage = context.frame.swapchainStage

				val alpha = 0.9f
				fun addColor(brown: Float) = srgbToLinear(rgba(
					0.4f * brown * alpha, 0.25f * brown * alpha, 0.17f * brown * alpha, 1f
				))
				fun multiplyColor() = rgba(1f - alpha, 1f - alpha, 1f - alpha, 0f)

				context.pipelines.blur.addBatch(
					context.frame.swapchainStage,
					framebuffers.blur, context.perFrame.areaBlurDescriptors,
					region.minX.toFloat(), region.minY.toFloat(),
					(region.minX + region.width).toFloat(), (region.minY + region.height).toFloat(),
				).gradientColorTransform(
					addColor(0.4f), multiplyColor(),
					addColor(0.4f), multiplyColor(),
					addColor(0.95f), multiplyColor(),
					addColor(0.95f), multiplyColor(),
				)
				titleColorBatch = renderInGameMenu(context, region, state.menu, state.campaign)
				if (state.menu.currentTab.inside) {
					val sectionRegion = determineSectionRenderRegion(region)
					context.pipelines.blur.addBatch(
						context.frame.swapchainStage,
						framebuffers.sectionBlur, context.perFrame.sectionsBlurDescriptors,
						sectionRegion.minX.toFloat(), sectionRegion.minY.toFloat(),
						sectionRegion.maxX + 1f, sectionRegion.maxY + 1f
					).noColorTransform()
				}
			} else {
				titleColorBatch = renderCurrentArea(context, area, region)
			}
		} else {
			renderBattle(context, state.campaign, battle, region)
			val loot = area.battleLoot
			if (loot != null) TODO("Render battle loot")
		}
	}

	if (titleColorBatch == null) titleColorBatch = context.addColorBatch(12)
	return titleColorBatch
}
