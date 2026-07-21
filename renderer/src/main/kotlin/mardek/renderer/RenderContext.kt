package mardek.renderer

import com.github.knokko.vk2d.frame.Vk2dRenderStage
import com.github.knokko.vk2d.frame.Vk2dSwapchainFrame
import com.github.knokko.vk2d.resource.Vk2dResourceBundle
import com.github.knokko.vk2d.text.Vk2dFancyTextStyleCache
import com.github.knokko.vk2d.text.Vk2dTextStyleCache
import mardek.content.Content
import mardek.content.ui.TitleScreenContent
import mardek.state.util.RenderTiming
import mardek.state.GameStateManager
import mardek.state.settings.VideoSettings
import mardek.state.ingame.CampaignState
import mardek.state.settings.UserSettings
import mardek.state.util.Rectangle
import mardek.content.util.Time
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val rawReferenceTime = Time.zero()

class RawRenderContext(
	val stage: Vk2dRenderStage,
	val pipelines: MardekPipelines,
	val textStyleCache: Vk2dTextStyleCache,
	val fancyTextStyleCache: Vk2dFancyTextStyleCache,
	val perFrameDescriptorSet: Long,
	val content: Content?,
	val titleContent: TitleScreenContent,
	val state: GameStateManager,
	val titleScreenBundle: Vk2dResourceBundle,
	val videoSettings: VideoSettings,
	val currentFps: Long,
) {
	// TODO CHAP2 Try to use something more accurate than System.nanoTime(), e.g. some present timing extension
	val timing = RenderTiming(rawReferenceTime, System.nanoTime(), Duration.INFINITE)
}

class RenderContext(
	val frame: Vk2dSwapchainFrame,
	var currentStage: Vk2dRenderStage,
	val framebuffers: MardekFramebuffers,
	val perFrame: PerFrameResources,
	val pipelines: MardekPipelines,
	val textStyleCache: Vk2dTextStyleCache,
	val fancyTextStyleCache: Vk2dFancyTextStyleCache,
	val perFrameDescriptorSet: Long,
	val content: Content,
	val state: GameStateManager,
	val campaign: CampaignState,
	val bundle: Vk2dResourceBundle,
	val userSettings: UserSettings,
	val currentFps: Long,
	extrapolationLimit: Duration,
) {

	// TODO CHAP2 Try to use something more accurate than System.nanoTime(), e.g. some present timing extension
	val timing = RenderTiming(campaign.time, System.nanoTime(), extrapolationLimit)

	fun addColorBatch(initialCapacity: Int) = pipelines.color.addBatch(currentStage, initialCapacity)!!

	fun addMultiplyBatch(initialCapacity: Int) = pipelines.multiply.addBatch(currentStage, initialCapacity)!!

	fun addOvalBatch(initialCapacity: Int) = pipelines.oval.addBatch(
		currentStage, perFrameDescriptorSet, initialCapacity
	)!!

	fun addImageBatch(initialCapacity: Int) = pipelines.image.addBatch(currentStage, initialCapacity, bundle)!!

	fun addTextBatch(initialCapacity: Int) = pipelines.simpleText.addBatch(currentStage, initialCapacity, textStyleCache)!!

	fun addFancyTextBatch(initialCapacity: Int) = pipelines.fancyText.addBatch(
		currentStage, initialCapacity, fancyTextStyleCache
	)!!

	fun addKim3Batch(initialCapacity: Int) = pipelines.kim3.addBatch(currentStage, initialCapacity, bundle, perFrameDescriptorSet)!!

	fun addSimpleWaterBatch(initialCapacity: Int, scissor: Rectangle, scale: Int) = pipelines.simpleWater.addBatch(
		currentStage, initialCapacity, bundle, perFrameDescriptorSet, scissor, scale, timing,
	)

	fun addAreaSpriteBatch(initialCapacity: Int, scissor: Rectangle) = pipelines.areaSprite.addBatch(
		currentStage, initialCapacity, bundle, perFrameDescriptorSet, scissor
	)

	fun addAreaLightBatch(scissor: Rectangle) = pipelines.areaLight.addBatch(currentStage, perFrameDescriptorSet, scissor)

	fun addAnimationPartBatch(initialCapacity: Int) = pipelines.animation.addBatch(currentStage, initialCapacity, bundle)
}
