package mardek.renderer

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage

abstract class StateRenderer(protected val boiler: BoilerInstance) {

	abstract fun render(recorder: CommandRecorder, targetImage: VkbImage, frameIndex: Int)
}
