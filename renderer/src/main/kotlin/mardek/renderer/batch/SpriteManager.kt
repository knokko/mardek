package mardek.renderer.batch

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.buffers.VkbBufferRange
import com.github.knokko.boiler.commands.SingleTimeCommands
import com.github.knokko.boiler.synchronization.ResourceUsage
import org.lwjgl.vulkan.VK10.*
import java.io.DataInputStream

class SpriteManager(
	boiler: BoilerInstance,
	spriteInput: DataInputStream,
) {

	val spriteBuffer: VkbBufferRange

	init {
		val spritesSize = spriteInput.readInt()
		spriteBuffer = boiler.buffers.create(
			4L * spritesSize,
			VK_BUFFER_USAGE_STORAGE_BUFFER_BIT or VK_BUFFER_USAGE_TRANSFER_DST_BIT,
			"KimSpriteBuffer"
		).fullRange()

		val stagingBuffer = boiler.buffers.createMapped(
			spriteBuffer.size, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, "Area Staging Buffer"
		)

		val stagingInts = stagingBuffer.fullMappedRange().intBuffer()
		for (counter in 0 until spritesSize) stagingInts.put(spriteInput.readInt())

		val commands = SingleTimeCommands(boiler)
		commands.submit("Area Staging Transfer") { recorder ->
			recorder.copyBufferRanges(stagingBuffer.fullRange(), spriteBuffer)
			recorder.bufferBarrier(spriteBuffer, ResourceUsage.TRANSFER_DEST, ResourceUsage.shaderRead(
				VK_PIPELINE_STAGE_VERTEX_SHADER_BIT or VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT
			))
		}

		commands.destroy()
		stagingBuffer.destroy(boiler)
	}

	fun destroy(boiler: BoilerInstance) {
		spriteBuffer.buffer.destroy(boiler)
	}
}
