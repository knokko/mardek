package mardek.renderer

import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dSimpleTextBatch
import mardek.content.action.ActionItemStorage
import mardek.content.action.ActionSaveCampaign
import mardek.content.action.ActionShop
import mardek.content.action.FixedActionNode
import mardek.renderer.actions.renderCampaignActions
import mardek.renderer.area.AreaRenderContext
import mardek.renderer.area.renderCurrentArea
import mardek.renderer.area.ui.renderChestLoot
import mardek.renderer.area.ui.shop.renderShopUi
import mardek.renderer.area.ui.storage.renderItemStorage
import mardek.renderer.battle.renderBattle
import mardek.renderer.battle.renderBattleLoot
import mardek.renderer.battle.renderMasteryScreen
import mardek.renderer.menu.MenuRenderContext
import mardek.renderer.menu.determineSectionRenderRegion
import mardek.renderer.menu.renderInGameMenu
import mardek.renderer.menu.renderInGameMenuSectionList
import mardek.renderer.save.renderSaveSelectionModal
import mardek.state.ingame.ConsiderCampaignExit
import mardek.state.ingame.InGameState
import mardek.state.ingame.actions.CampaignActionsState
import mardek.state.ingame.actions.ItemStorageInteractionState
import mardek.state.ingame.actions.ShopInteractionState
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.area.AreaSuspensionActions
import mardek.state.ingame.area.AreaSuspensionBattle
import mardek.state.ingame.area.AreaSuspensionOpeningChest
import mardek.state.ingame.area.loot.BattleLoot
import mardek.state.ingame.battle.BattleStateMachine
import mardek.state.ingame.menu.ShownState
import mardek.state.ingame.worldmap.WorldMapState
import mardek.state.saves.SaveSelectionState
import mardek.state.util.Rectangle
import kotlin.math.abs
import kotlin.math.pow

internal fun renderInGame(
	context: RenderContext, state: InGameState, region: Rectangle
): Pair<Vk2dColorBatch, Vk2dSimpleTextBatch> {

	fun renderBlurred(blurStrength: Float) {
		context.currentStage = context.frame.swapchainStage

		val alpha = 0.9f * blurStrength.pow(0.3f)
		fun addColor(brown: Float) = srgbToLinear(rgba(
			0.4f * brown * alpha, 0.25f * brown * alpha, 0.17f * brown * alpha, 1f
		))
		fun multiplyColor() = rgba(1f - alpha, 1f - alpha, 1f - alpha, 0f)

		context.pipelines.blur.addBatch(
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
	var titleTextBatch: Vk2dSimpleTextBatch? = null

	when (val stateMachine = state.campaign.state) {
		is AreaState -> {
			val supportedBlurFilterSizes = arrayOf(0, 1, 3, 4, 9)

			val suspension = stateMachine.suspension
			if (suspension !is AreaSuspensionBattle) {
				val considerExit = state.considerExit
				if (state.menu.shown !is ShownState.FullyHidden || considerExit != null) {
					val framebuffers = context.framebuffers
					val areaRenderStage = context.pipelines.blur.addSourceStage(
						context.frame, framebuffers.blur, -1
					)

					val menuOpacity = if (considerExit == null) {
						when (val shown = state.menu.shown) {
							is ShownState.FullyShown -> 1f
							is ShownState.FadingIn -> context.timing.interpolate(
								shown.since, 0f,
								ShownState.FADE_DURATION, 1f, true,
							)
							is ShownState.FadingOut -> context.timing.interpolate(
								shown.since, 1f,
								ShownState.FADE_DURATION, 0f, true
							)
							else -> throw RuntimeException("Unexpected shown state $shown")
						}
					} else {
						val cancelTime = considerExit.cancelledAt
						if (cancelTime != null) {
							context.timing.interpolate(
								cancelTime, 1f,
								ConsiderCampaignExit.CANCEL_FADE_OUT, 0f, true
							)
						} else {
							context.timing.interpolate(
								considerExit.consideredAt, 0f,
								ConsiderCampaignExit.FADE_IN, 1f, true
							)
						}
					}

					val blurFilterSize = supportedBlurFilterSizes.minBy { abs(it - 9f * menuOpacity) }

					if (considerExit == null && state.menu.currentTab.inside && state.menu.currentTab.shouldShowSectionList()) {
						context.currentStage = context.pipelines.blur.addSourceStage(
							context.frame, framebuffers.sectionBlur, -1
						)

						val menuRegion = determineSectionRenderRegion(region)
						val colorBatch = context.addColorBatch(50)
						val fancyTextBatch = context.addFancyTextBatch(300)

						// The image/sprite/oval pipelines are not needed for rendering the section list,
						// but we still need a valid instance of MenuRenderContext
						val simpleTextBatch = context.addTextBatch(0)
						val ovalBatch = context.addOvalBatch(0)
						val areaSpriteBatch = context.addAreaSpriteBatch(0, region)
						val imageBatch = context.addImageBatch(0)
						val spriteBatch = context.addKim3Batch(0)
						val animationBatch = context.addAnimationPartBatch(0)
						val lateColorBatch = context.addColorBatch(0)

						val menuContext = MenuRenderContext(
							context, colorBatch, ovalBatch, areaSpriteBatch, imageBatch, spriteBatch,
							animationBatch, lateColorBatch,
							simpleTextBatch, fancyTextBatch, state.menu, state.campaign,
						)
						renderInGameMenuSectionList(menuContext, Rectangle(
							0, 0, menuRegion.width, menuRegion.height
						))
					}
					val computeStage = context.pipelines.blur.addComputeStage(
						context.frame, context.perFrame.areaBlurDescriptors,
						framebuffers.blur, blurFilterSize, 50, -1
					)
					if (considerExit == null && state.menu.currentTab.inside && state.menu.currentTab.shouldShowSectionList()) {
						computeStage.additional(
							context.perFrame.sectionsBlurDescriptors,
							framebuffers.sectionBlur, blurFilterSize, 50
						)
					}

					context.currentStage = areaRenderStage
					val areaRenderRegion = Rectangle(0, 0, areaRenderStage.width, areaRenderStage.height)
					renderCurrentArea(context, stateMachine, areaRenderRegion)
					renderBlurred(menuOpacity)

					if (considerExit == null) {
						val batches = renderInGameMenu(context, region, 1f - menuOpacity, state.menu, state.campaign)
						titleColorBatch = batches.first
						titleTextBatch = batches.second

						if (state.menu.currentTab.inside && state.menu.currentTab.shouldShowSectionList()) {
							val sectionRegion = determineSectionRenderRegion(region)
							context.pipelines.blur.addBatch(
								context.frame.swapchainStage,
								framebuffers.sectionBlur, context.perFrame.sectionsBlurDescriptors,
								sectionRegion.minX.toFloat(), sectionRegion.minY.toFloat(),
								sectionRegion.maxX + 1f, sectionRegion.maxY + 1f
							).noColorTransform()
						}
					} else {
						val batches = renderCampaignExitModal(considerExit, menuOpacity, context, region)
						titleColorBatch = batches.first
						titleTextBatch = batches.second
					}
				} else {
					var saveSelection: SaveSelectionState? = null
					var itemStorage: ItemStorageInteractionState? = null
					var shopState: ShopInteractionState? = null
					if (suspension is AreaSuspensionActions) {
						val node = suspension.actions.node
						if (node is FixedActionNode) {
							if (node.action is ActionSaveCampaign) {
								saveSelection = suspension.actions.saveSelectionState
							}
							if (node.action is ActionItemStorage) {
								itemStorage = suspension.actions.itemStorageInteraction
							}
							if (node.action is ActionShop) {
								shopState = suspension.actions.shopInteraction
							}
						}
					}

					val batches: Pair<Vk2dColorBatch, Vk2dSimpleTextBatch>
					if (saveSelection != null || suspension is AreaSuspensionOpeningChest) {
						var blurStrength = 1f
						if (suspension is AreaSuspensionOpeningChest) {
							blurStrength = context.timing.interpolate(
								suspension.openedAt, 0f,
								AreaSuspensionOpeningChest.FADE_DURATION, 1f, true
							)
							if (suspension.closedAt != null) {
								blurStrength *= context.timing.interpolate(
									suspension.closedAt!!, 1f,
									AreaSuspensionOpeningChest.FADE_DURATION, 0f, true
								)
							}
						}
						val blurFilterSize = supportedBlurFilterSizes.minBy { abs(it - 9f * blurStrength) }

						val framebuffers = context.framebuffers
						val areaRenderStage = context.pipelines.blur.addSourceStage(
							context.frame, framebuffers.blur, -1
						)
						context.pipelines.blur.addComputeStage(
							context.frame, context.perFrame.areaBlurDescriptors,
							framebuffers.blur, blurFilterSize, 50, -1
						)

						context.currentStage = areaRenderStage
						val areaRenderRegion = Rectangle(0, 0, areaRenderStage.width, areaRenderStage.height)
						renderCurrentArea(context, stateMachine, areaRenderRegion)
						renderBlurred(blurStrength)

						if (saveSelection != null) {
							val basicFont = context.bundle.getFont(context.content.fonts.basic2.index)
							val fatFont = context.bundle.getFont(context.content.fonts.fat.index)
							val upperFont = context.bundle.getFont(context.content.fonts.large2.index)
							batches = renderSaveSelectionModal(
								context, basicFont, fatFont, upperFont,
								saveSelection, true, region, context.timing,
							)
						} else {
							val areaContext = AreaRenderContext.create(context, stateMachine, region)
							batches = Pair(areaContext.uiColorBatch, areaContext.simpleTextBatch)
							renderChestLoot(areaContext, blurStrength)
						}
					} else if (itemStorage != null) {
						batches = renderItemStorage(context, itemStorage, region)
					} else if (shopState != null) {
						if (shopState.isFading(context.timing.now())) {
							renderCurrentArea(context, stateMachine, region)
						}
						batches = renderShopUi(context, shopState, region)
					} else {
						batches = renderCurrentArea(context, stateMachine, region)
					}
					titleColorBatch = batches.first
					titleTextBatch = batches.second
				}
			} else {
				val framebuffers = context.framebuffers
				val loot = suspension.loot
				if (loot == null) {
					val batches = renderBattle(context, state.campaign, suspension.battle, region)
					titleColorBatch = batches.first
					titleTextBatch = batches.second
				} else {
					context.currentStage = context.pipelines.blur.addSourceStage(
						context.frame, framebuffers.blur, -1
					)
					context.pipelines.blur.addComputeStage(
						context.frame, context.perFrame.areaBlurDescriptors,
						framebuffers.blur, 3, 50, -1
					)
					renderBattle(context, state.campaign, suspension.battle, region)

					context.currentStage = context.frame.swapchainStage

					val blurStrength = context.timing.interpolate(
						loot.startTime, 0f,
						BattleStateMachine.Victory.VICTORY_TEXT_FADE_OUT, 1f, true
					)
					renderBlurred(blurStrength)
					val batches = if (loot.showMasteryScreen) {
						renderMasteryScreen(context, loot, state.campaign.usedPartyMembers(), region)
					} else renderBattleLoot(context, loot, state.campaign.usedPartyMembers(), region)

					if (loot.startedFadeOut != null) {
						val fadeAlpha = context.timing.interpolate(
							loot.startedFadeOut!!, 0,
							BattleLoot.FADE_OUT_DURATION, 255, true
						)
						if (fadeAlpha > 0) {
							context.addColorBatch(2).fill(
								region.minX, region.minY, region.maxX, region.maxY,
								rgba(0, 0, 0, fadeAlpha),
							)
						}
					}

					titleColorBatch = batches.first
					titleTextBatch = batches.second
				}
			}
		}
		is CampaignActionsState -> {
			val batches = renderCampaignActions(context, stateMachine, region)
			titleColorBatch = batches.first
			titleTextBatch = batches.second
		}
		is WorldMapState -> {
			val batches = renderWorldMap(context, stateMachine, region)
			titleColorBatch = batches.first
			titleTextBatch = batches.second
		}
	}

	if (titleColorBatch == null) titleColorBatch = context.addColorBatch(36)
	if (titleTextBatch == null) titleTextBatch = context.addTextBatch(25)
	return Pair(titleColorBatch, titleTextBatch)
}
