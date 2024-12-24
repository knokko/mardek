package mardek.renderer.batch

import com.github.knokko.boiler.builders.BoilerBuilder
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.lwjgl.vulkan.VK10.VK_API_VERSION_1_0
import org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestPerFrameBuffer {

	private val boiler = BoilerBuilder(
		VK_API_VERSION_1_0, "TestPerFrameBuffer", 1
	).validation().forbidValidationErrors().build()

	@Test
	fun testBasic() {
		val buffer = boiler.buffers.createMapped(10, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, "PerFrame")

		val perFrame = PerFrameBuffer(buffer.fullMappedRange())

		perFrame.startFrame(0)
		assertEquals(0L, perFrame.allocate(2, 3).offset)
		assertEquals(2L, perFrame.allocate(2, 2).offset)
		assertEquals(5L, perFrame.allocate(1, 5).offset)

		perFrame.startFrame(1)
		assertEquals(6L, perFrame.allocate(3, 3).offset)
		assertEquals(9L, perFrame.allocate(1, 3).offset)

		perFrame.startFrame(0)
		assertEquals(0L, perFrame.allocate(3, 1).offset)

		buffer.destroy(boiler)
	}

	@Test
	fun testOverflowFirstFrame() {
		val buffer = boiler.buffers.createMapped(10, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, "PerFrame")

		val perFrame = PerFrameBuffer(buffer.fullMappedRange())

		perFrame.startFrame(0)
		assertEquals(0L, perFrame.allocate(6, 3).offset)
		assertThrows<IllegalStateException> { perFrame.allocate(6, 1) }

		buffer.destroy(boiler)
	}

	@Test
	fun testOverflowSecondFrame() {
		val buffer = boiler.buffers.createMapped(10, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, "PerFrame")

		val perFrame = PerFrameBuffer(buffer.fullMappedRange())

		perFrame.startFrame(0)
		assertEquals(0L, perFrame.allocate(6, 3).offset)
		perFrame.startFrame(1)
		assertThrows<IllegalStateException> { perFrame.allocate(6, 1) }

		buffer.destroy(boiler)
	}

	@Test
	fun testRespectAlignmentWithRangeOffset() {
		val buffer = boiler.buffers.createMapped(15, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, "PerFrame")

		val perFrame = PerFrameBuffer(buffer.mappedRange(3, 8))
		perFrame.startFrame(1)
		assertEquals(5L, perFrame.allocate(3, 5).offset)
		assertEquals(8L, perFrame.allocate(2, 4).offset)

		perFrame.startFrame(1)
		assertEquals(5L, perFrame.allocate(3, 5).offset)

		buffer.destroy(boiler)
	}

	@Test
	fun testWrapAlignmentOverflow1() {
		val buffer = boiler.buffers.createMapped(15, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, "PerFrame")

		val perFrame = PerFrameBuffer(buffer.mappedRange(1, 10))
		perFrame.startFrame(0)
		assertEquals(1L, perFrame.allocate(3, 1).offset)

		perFrame.startFrame(1)
		assertEquals(5L, perFrame.allocate(3, 5).offset)
		assertThrows<IllegalStateException> { perFrame.allocate(3, 5) }

		buffer.destroy(boiler)
	}

	@Test
	fun testWrapAlignmentOverflow2() {
		val buffer = boiler.buffers.createMapped(15, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, "PerFrame")

		val perFrame = PerFrameBuffer(buffer.mappedRange(1, 10))
		perFrame.startFrame(0)
		assertEquals(1L, perFrame.allocate(2, 1).offset)

		perFrame.startFrame(1)
		assertEquals(3L, perFrame.allocate(2, 1).offset)

		perFrame.startFrame(0)
		assertEquals(5L, perFrame.allocate(3, 1).offset)

		perFrame.startFrame(1)
		assertThrows<IllegalStateException> { perFrame.allocate(5, 5) }

		buffer.destroy(boiler)
	}

	@AfterAll
	fun tearDown() {
		boiler.destroyInitialObjects()
	}
}
