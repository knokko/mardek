package mardek.renderer

import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dGlyphBatch
import mardek.content.action.ActionSaveCampaign
import mardek.content.action.FixedActionNode
import mardek.renderer.area.renderCurrentArea
import mardek.renderer.battle.renderBattle
import mardek.renderer.battle.renderBattleLoot
import mardek.renderer.menu.MenuRenderContext
import mardek.renderer.menu.determineSectionRenderRegion
import mardek.renderer.menu.renderInGameMenu
import mardek.renderer.menu.renderInGameMenuSectionList
import mardek.renderer.save.renderSaveSelectionModal
import mardek.state.ingame.InGameState
import mardek.state.saves.SaveSelectionState
import mardek.state.util.Rectangle

internal fun renderInGame(
	context: RenderContext, state: InGameState, region: Rectangle
): Pair<Vk2dColorBatch, Vk2dGlyphBatch> {

	fun renderBlurred() {
		context.currentStage = context.frame.swapchainStage

		val alpha = 0.9f
		fun addColor(brown: Float) = srgbToLinear(rgba(
			0.4f * brown * alpha, 0.25f * brown * alpha, 0.17f * brown * alpha, 1f
		))
		fun multiplyColor() = rgba(1f - alpha, 1f - alpha, 1f - alpha, 0f)

		context.pipelines.base.blur.addBatch(
			context.frame.swapchainStage,
			context.framebuffers.blur, context.perFrame.areaBlurDescriptors,
			region.minX.toFloat(), region.minY.toFloat(),
			(region.minX + region.width).toFloat(), (region.minY + region.height).toFloat(),
		).gradientColorTransform(
			addColor(0.4f), multiplyColor(),
			addColor(0.4f), multiplyColor(),
			addColor(0.95f), multiplyColor(),
			addColor(0.95f), multiplyColor(),
		)
	}

	var titleColorBatch: Vk2dColorBatch? = null
	var titleTextBatch: Vk2dGlyphBatch? = null

	val area = state.campaign.currentArea
	if (area != null) {
		val battle = area.activeBattle
		if (battle == null) {
			if (state.menu.shown) {
				val framebuffers = context.framebuffers
				val areaRenderStage = context.pipelines.base.blur.addSourceStage(
					context.frame, framebuffers.blur, -1
				)
				if (state.menu.currentTab.inside) {
					context.currentStage = context.pipelines.base.blur.addSourceStage(
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
				val computeStage = context.pipelines.base.blur.addComputeStage(
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
				renderBlurred()

				val batches = renderInGameMenu(context, region, state.menu, state.campaign)
				titleColorBatch = batches.first
				titleTextBatch = batches.second

				if (state.menu.currentTab.inside) {
					val sectionRegion = determineSectionRenderRegion(region)
					context.pipelines.base.blur.addBatch(
						context.frame.swapchainStage,
						framebuffers.sectionBlur, context.perFrame.sectionsBlurDescriptors,
						sectionRegion.minX.toFloat(), sectionRegion.minY.toFloat(),
						sectionRegion.maxX + 1f, sectionRegion.maxY + 1f
					).noColorTransform()
				}
			} else {
				var saveSelection: SaveSelectionState? = null
				val actions = area.actions
				if (actions != null) {
					val node = actions.node
					if (node is FixedActionNode && node.action is ActionSaveCampaign) {
						saveSelection = actions.saveSelectionState
					}
				}

				if (saveSelection != null) {
					val framebuffers = context.framebuffers
					val areaRenderStage = context.pipelines.base.blur.addSourceStage(
						context.frame, framebuffers.blur, -1
					)
					context.pipelines.base.blur.addComputeStage(
						context.frame, context.perFrame.areaBlurDescriptors,
						framebuffers.blur, 9, 50, -1
					)

					context.currentStage = areaRenderStage
					val areaRenderRegion = Rectangle(0, 0, areaRenderStage.width, areaRenderStage.height)
					renderCurrentArea(context, area, areaRenderRegion)
					renderBlurred()

					val basicFont = context.bundle.getFont(context.content.fonts.basic2.index)
					val fatFont = context.bundle.getFont(context.content.fonts.fat.index)
					val upperFont = context.bundle.getFont(context.content.fonts.large2.index)
					val batches = renderSaveSelectionModal(
						context, basicFont, fatFont, upperFont,
						saveSelection, true, region,
					)
					titleColorBatch = batches.first
					titleTextBatch = batches.second
				} else {
					val batches = renderCurrentArea(context, area, region)
					titleColorBatch = batches.first
					titleTextBatch = batches.second
				}
			}
		} else {
			val framebuffers = context.framebuffers
			val loot = area.battleLoot
			if (loot == null) {
				val batches = renderBattle(context, state.campaign, battle, region)
				titleColorBatch = batches.first
				titleTextBatch = batches.second
			} else {
				context.currentStage = context.pipelines.base.blur.addSourceStage(
					context.frame, framebuffers.blur, -1
				)
				context.pipelines.base.blur.addComputeStage(
					context.frame, context.perFrame.areaBlurDescriptors,
					framebuffers.blur, 3, 50, -1
				)
				renderBattle(context, state.campaign, battle, region)

				context.currentStage = context.frame.swapchainStage
				val blurStrength = 240
				val leftBlurColor = srgbToLinear(rgba(54, 37, 21, blurStrength))
				val rightBlurColor = srgbToLinear(rgba(132, 84, 53, blurStrength))
				val inverseBlur = 255 - blurStrength
				val multiplyColor = rgba(inverseBlur, inverseBlur, inverseBlur, inverseBlur)
				context.pipelines.base.blur.addBatch(
					context.frame.swapchainStage,
					framebuffers.blur, context.perFrame.areaBlurDescriptors,
					region.minX.toFloat(), region.minY.toFloat(),
					region.boundX.toFloat(), region.boundY.toFloat(),
				).gradientColorTransform(
					leftBlurColor, multiplyColor,
					leftBlurColor, multiplyColor,
					rightBlurColor, multiplyColor,
					rightBlurColor, multiplyColor,
				)
				val batches = renderBattleLoot(context, loot, state.campaign.characterSelection.party, region)
				titleColorBatch = batches.first
				titleTextBatch = batches.second
			}
		}
	}

	if (titleColorBatch == null) titleColorBatch = context.addColorBatch(36)
	if (titleTextBatch == null) titleTextBatch = context.addTextBatch(25)
	return Pair(titleColorBatch, titleTextBatch)
}
