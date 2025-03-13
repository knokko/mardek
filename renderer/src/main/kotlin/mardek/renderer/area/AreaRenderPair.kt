package mardek.renderer.area

import com.github.knokko.bitser.io.BitInputStream
import com.github.knokko.bitser.serialize.Bitser
import mardek.content.area.StoredAreaRenderData
import java.io.ByteArrayInputStream

class AreaRenderPair(
	private val bitser: Bitser,
	private var compressedData: ByteArray?
) {

	val data by lazy {
		val startTime = System.nanoTime()
		val bitInput = BitInputStream(ByteArrayInputStream(compressedData!!))
		val stored = bitser.deserialize(StoredAreaRenderData::class.java, bitInput)
		println("loading area took ${(System.nanoTime() - startTime) / 1000_000} ms")
		compressedData = null
		MemoryAreaRenderData.pack(stored)
	}
}
