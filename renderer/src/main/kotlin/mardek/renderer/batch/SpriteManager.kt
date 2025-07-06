package mardek.renderer.batch

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.memory.MemoryCombiner
import com.github.knokko.boiler.synchronization.ResourceUsage
import org.lwjgl.vulkan.VK10.*
import java.io.DataInputStream

class SpriteManager(
	boiler: BoilerInstance,
	private val spriteInput: DataInputStream,
	persistentCombiner: MemoryCombiner,
	stagingCombiner: MemoryCombiner,
) {

	private val spritesSize = spriteInput.readInt()
	val spriteBuffer = persistentCombiner.addBuffer(
		4L * spritesSize, boiler.deviceProperties.limits().minStorageBufferOffsetAlignment(),
		VK_BUFFER_USAGE_STORAGE_BUFFER_BIT or VK_BUFFER_USAGE_TRANSFER_DST_BIT, 0.5f
	)!!
	private val stagingBuffer = stagingCombiner.addMappedBuffer(
		4L * spritesSize, 4L, VK_BUFFER_USAGE_TRANSFER_SRC_BIT
	)

	fun prepare() {
		val stagingInts = stagingBuffer.intBuffer()
		repeat(spritesSize) {
			stagingInts.put(spriteInput.readInt())
		}
	}

	fun prepare(recorder: CommandRecorder) {
		recorder.copyBuffer(stagingBuffer, spriteBuffer)
		recorder.bufferBarrier(spriteBuffer, ResourceUsage.TRANSFER_DEST, ResourceUsage.shaderRead(
			VK_PIPELINE_STAGE_VERTEX_SHADER_BIT or VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT
		))
	}
}
