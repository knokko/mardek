package mardek.renderer

import com.github.knokko.vk2d.pipeline.Vk2dBlurPipeline

class PerFrameResources(
	val areaBlurDescriptors: Vk2dBlurPipeline.Descriptors,
	val sectionsBlurDescriptors: Vk2dBlurPipeline.Descriptors,
	val actionBarBlurDescriptors: Vk2dBlurPipeline.Descriptors,
) {
}
