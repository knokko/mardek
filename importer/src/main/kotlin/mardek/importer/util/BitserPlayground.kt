package mardek.importer.util

import com.github.knokko.bitser.Bitser
import com.github.knokko.bitser.io.DebugBitOutputStream
import com.github.knokko.profiler.SampleProfiler
import com.github.knokko.profiler.storage.SampleStorage
import mardek.content.Content
import mardek.content.stats.CombatStat
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintWriter
import java.nio.file.Files

fun main() {
	val bitser = Bitser(true)

	val contentInput = BufferedInputStream(Files.newInputStream(File(
		"$projectFolder/game/src/main/resources/mardek/game/content.bits").toPath()
	))
	val bytes = contentInput.readAllBytes()
	contentInput.close()

//	val storage = SampleStorage.frequency()
//	val profiler = SampleProfiler(storage)
//	profiler.start()
	val content = bitser.fromBytes(Content::class.java, bytes, Bitser.BACKWARD_COMPATIBLE)
//	profiler.stop()
//
//	storage.getThreadStorage(Thread.currentThread().id).print(System.out, 10, 1.0)

	val debugWriter = PrintWriter(Files.newOutputStream(File("content-debug.txt").toPath()))
	bitser.serialize(content, DebugBitOutputStream(
		ByteArrayOutputStream(), debugWriter, false
	), Bitser.BACKWARD_COMPATIBLE)
	debugWriter.flush()
	debugWriter.close()
//	val intDistribution = IntegerDistributionTracker()
//	val floatDistribution = FloatDistributionTracker()
//	val sizeAnalysis = BitserSizeAnalysis(ByteArrayOutputStream())
//	bitser.serialize(
//		content, sizeAnalysis, Bitser.BACKWARD_COMPATIBLE,
//		intDistribution, floatDistribution, Bitser.FORBID_LAZY_SAVING
//	)
//	sizeAnalysis.finish()
//	println("done")
//
//	intDistribution.optimize(PrintWriter(System.out), 20, 10, 10)
//	floatDistribution.optimize(
//		PrintWriter(System.out), 20, 5, 10,
//		doubleArrayOf(0.0, 0.001, 0.002, 0.01, 0.05, 0.1)
//	)
	// TODO CHAP1 Make tile grids lazy
}
