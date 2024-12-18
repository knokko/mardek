package mardek.renderer

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.io.BitInputStream
import com.github.knokko.bitser.serialize.Bitser
import com.github.knokko.boiler.BoilerInstance
import mardek.assets.area.StoredAreaRenderData
import mardek.renderer.area.*
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.util.*

class SharedResources(boiler: BoilerInstance, areaResourcePath: String, framesInFlight: Int, targetImageFormat: Int) {

	val areaMap = mutableMapOf<UUID, MemoryAreaRenderData>()
	val kimRenderer: KimRenderer

	init {
		val startTime = System.nanoTime()

		val input = DataInputStream(BufferedInputStream(SharedResources::class.java.classLoader.getResourceAsStream(areaResourcePath)!!))
		this.kimRenderer = KimRenderer(
			boiler, targetImageFormat = targetImageFormat, framesInFlight = framesInFlight, spriteInput = input
		)

		@BitStruct(backwardCompatible = false)
		class StoredAreas(
			@BitField(ordering = 0)
			val list: ArrayList<StoredAreaRenderData>
		) {
			@Suppress("unused")
			constructor() : this(ArrayList(0))
		}

		val storedAreas = Bitser(false).deserialize(StoredAreas::class.java, BitInputStream(input))
		for (stored in storedAreas.list) {
			areaMap[stored.areaID] = MemoryAreaRenderData.pack(stored)
		}

		input.close()

		println("Preparing render resources took ${(System.nanoTime() - startTime) / 1_000_000} ms")
	}

	fun destroy() {
		kimRenderer.destroy()
	}
}
