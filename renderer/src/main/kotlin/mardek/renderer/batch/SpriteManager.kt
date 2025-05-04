package mardek.renderer.batch

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.buffers.SharedDeviceBufferBuilder
import com.github.knokko.boiler.buffers.SharedMappedBufferBuilder
import com.github.knokko.boiler.buffers.VkbBufferRange
import com.github.knokko.boiler.commands.SingleTimeCommands
import com.github.knokko.boiler.synchronization.ResourceUsage
import org.lwjgl.vulkan.VK10.*
import java.io.DataInputStream

class SpriteManager(
	private val boiler: BoilerInstance,
	private val spriteInput: DataInputStream,
	sharedSpriteBuilder: SharedDeviceBufferBuilder,
	sharedStagingBuilder: SharedMappedBufferBuilder,
	storageIntAlignment: Long,
) {

	private val spritesSize = spriteInput.readInt()
	private val getSpriteBuffer = sharedSpriteBuilder.add(4L * spritesSize, storageIntAlignment)
	private val getStagingBuffer = sharedStagingBuilder.add(4L * spritesSize, 4L)
	lateinit var spriteBuffer: VkbBufferRange
		private set

	fun initBuffers() {
		spriteBuffer = getSpriteBuffer.get()
		val stagingInts = getStagingBuffer.get().intBuffer()
		for (counter in 0 until spritesSize) stagingInts.put(spriteInput.readInt())

		val commands = SingleTimeCommands(boiler)
		commands.submit("Area Staging Transfer") { recorder ->
			recorder.copyBufferRanges(getStagingBuffer.get().range(), spriteBuffer)
			recorder.bufferBarrier(spriteBuffer, ResourceUsage.TRANSFER_DEST, ResourceUsage.shaderRead(
				VK_PIPELINE_STAGE_VERTEX_SHADER_BIT or VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT
			))
		}

		commands.destroy()
	}
}
