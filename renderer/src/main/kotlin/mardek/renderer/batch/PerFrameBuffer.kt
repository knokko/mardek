package mardek.renderer.batch

import com.github.knokko.boiler.buffers.MappedVkbBufferRange

// TODO Move this to vk-boiler
class PerFrameBuffer(
	private val range: MappedVkbBufferRange,
) {

	private val limits = mutableMapOf<Int, Long>()

	/**
	 * The next byte will be written to `range[currentOffset]`
	 */
	private var currentOffset = 0L

	/**
	 * The first index into `range` to which we must **not** write
	 */
	private var currentLimit = 0L

	fun startFrame(frameIndex: Int) {
		limits.remove(frameIndex)
		currentLimit = limits.values.filter { it >= currentOffset }.minOrNull() ?:
				(limits.values.minOrNull() ?: (currentOffset - 1))
		if (currentLimit < 0) currentLimit = range.size
		limits[frameIndex] = currentOffset - 1
	}

	private fun align(alignment: Long) {
		var fullOffset = range.offset + currentOffset
		if (fullOffset % alignment == 0L) return

		fullOffset = (1L + fullOffset / alignment) * alignment
		currentOffset = fullOffset - range.offset
	}

	fun allocate(byteSize: Long, alignment: Long): MappedVkbBufferRange {
		align(alignment)
		var nextOffset = currentOffset + byteSize
		if (currentOffset > currentLimit && nextOffset > range.size) {
			currentOffset = 0
			align(alignment)
			if (currentOffset >= currentLimit) {
				throw IllegalStateException("Alignment $alignment is larger than limit $currentLimit")
			}
			nextOffset = currentOffset + byteSize
		}
		if (currentLimit in currentOffset..<nextOffset) {
			throw IllegalStateException("Buffer is too small: current offset is $currentOffset and " +
					"next offset is $nextOffset, but limit is $currentLimit")
		}
		val result = range.buffer.mappedRange(range.offset + currentOffset, byteSize)
		currentOffset = nextOffset
		return result
	}
}
