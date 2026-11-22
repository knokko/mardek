package mardek.editor.server.operation

import com.github.knokko.bitser.IntegerBitser
import com.github.knokko.bitser.connection.BitServerContext
import com.github.knokko.bitser.connection.BitServerStructController
import com.github.knokko.bitser.connection.BitServerStructOperation
import com.github.knokko.bitser.io.BitInputStream
import com.github.knokko.bitser.io.BitOutputStream
import com.github.knokko.zstd.CompressingOutputStream
import mardek.content.BITSER
import mardek.content.ContentWrapper
import java.lang.Math.toIntExact
import java.nio.file.Files
import kotlin.io.path.fileSize
import kotlin.system.measureNanoTime

class OperationSyncForExport : BitServerStructOperation<ContentWrapper> {

	private var responseID = 0

	override fun readRequestData(fromClient: BitInputStream) {
		responseID = toIntExact(IntegerBitser.decodeVariableIntegerUsingTerminatorBits(
			0L, Long.MAX_VALUE, fromClient
		))
	}

	override fun execute(
		controller: BitServerStructController<ContentWrapper>,
		context: BitServerContext,
		connectionHandle: Any,
	) {
		val tempFile = Files.createTempFile("editor-server-content", ".bits")
		tempFile.toFile().deleteOnExit()

		context.globalQueue.add {
			val compressionTime = measureNanoTime {
				val output = CompressingOutputStream(tempFile, 5)
				val bitOutput = BitOutputStream(output)
				BITSER.serialize(context.rootStruct, bitOutput)
				bitOutput.finish()
			}
			println("Saving & compressing took ${compressionTime / 1_000_000} ms")

			context.respondToClient.send(connectionHandle, responseID) { toClient ->
				val transmissionTime = measureNanoTime {
					val fileSize = toIntExact(tempFile.fileSize())
					IntegerBitser.encodeFullInt(fileSize, toClient)
					val tempInput = Files.newInputStream(tempFile)
					val sendBuffer = ByteArray(1024 * 1024)
					var remaining = fileSize
					while (remaining > 0) {
						if (remaining < sendBuffer.size) {
							tempInput.readNBytes(sendBuffer, 0, remaining)
							toClient.write(sendBuffer.sliceArray(0 until remaining))
							break
						} else {
							tempInput.readNBytes(sendBuffer, 0, sendBuffer.size)
							toClient.write(sendBuffer)
							remaining -= sendBuffer.size
						}
					}
				}
				println("Transmission took ${transmissionTime / 1_000_000} ms")
			}
		}
	}
}

/*
Investigate hang, which may or may not be fixed after updating kwik:
"BikServer.respondToClient" #69 [47366] daemon prio=5 os_prio=0 cpu=195,42ms elapsed=96,74s tid=0x000078c8c8037000 nid=47366 waiting on condition  [0x000078c8cf7fe000]
   java.lang.Thread.State: WAITING (parking)
	at jdk.internal.misc.Unsafe.park(java.base@25.0.5.1/Native Method)
	- parking to wait for  <0x000004000700cb60> (a java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject)
	at java.util.concurrent.locks.LockSupport.park(java.base@25.0.5.1/LockSupport.java:369)
	at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionNode.block(java.base@25.0.5.1/AbstractQueuedSynchronizer.java:520)
	at java.util.concurrent.ForkJoinPool.unmanagedBlock(java.base@25.0.5.1/ForkJoinPool.java:4364)
	at java.util.concurrent.ForkJoinPool.managedBlock(java.base@25.0.5.1/ForkJoinPool.java:4310)
	at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(java.base@25.0.5.1/AbstractQueuedSynchronizer.java:1752)
	at tech.kwik.core.stream.SendBuffer.write(SendBuffer.java:83)
	at tech.kwik.core.stream.StreamOutputStreamImpl.write(StreamOutputStreamImpl.java:98)
	at tech.kwik.core.stream.StreamOutputStreamImpl.write(StreamOutputStreamImpl.java:115)
	at tech.kwik.core.stream.StreamOutputStreamImpl.write(StreamOutputStreamImpl.java:90)
	at com.github.knokko.bitser.io.BitOutputStream.write(BitOutputStream.java:99)
	at mardek.editor.server.operation.OperationSyncForExport.execute$lambda$0$1(OperationSyncForExport.kt:58)
	at mardek.editor.server.operation.OperationSyncForExport$$Lambda/0x0000000040240b28.write(Unknown Source)
	at com.github.knokko.bitser.kwik.BikServer.lambda$respondToClient$0(BikServer.java:35)
	at com.github.knokko.bitser.kwik.BikServer$$Lambda/0x0000000040240d58.run(Unknown Source)
	at java.lang.Thread.runWith(java.base@25.0.5.1/Thread.java:1487)
	at java.lang.Thread.run(java.base@25.0.5.1/Thread.java:1474)

SendBuffer line 83: notFull.await();
StreamOutputStreamImpl line 98: sendBuffer.write(data, off, len);
StreamOutputStreamImpl line 115: write(data, off + times * halfBuffersize, rest);


"BikClientStreamFactory.addResponseCallback.processResponse" #64 [47367] daemon prio=6 os_prio=0 cpu=247,19ms elapsed=1035,40s tid=0x000079b9c400c4e0 nid=47367 in Object.wait()  [0x000079b9e81e6000]
   java.lang.Thread.State: TIMED_WAITING (on object monitor)
	at java.lang.Object.wait0(java.base@25.0.5.1/Native Method)
	- waiting on <no object reference available>
	at java.lang.Object.wait(java.base@25.0.5.1/Object.java:389)
	at tech.kwik.core.stream.StreamInputStreamImpl.read(StreamInputStreamImpl.java:192)
	- locked <0x0000040001af7bc8> (a java.lang.Object)
	at com.github.knokko.bitser.io.BitInputStream.read(BitInputStream.java:93)
	at mardek.editor.client.file.ExportingMenuKt.ExportingMenu$lambda$18$0$0$0$0(ExportingMenu.kt:87)
	at mardek.editor.client.file.ExportingMenuKt$$Lambda/0x000000007339ab38.read(Unknown Source)
	at com.github.knokko.bitser.kwik.BikClientStreamFactory.lambda$addResponseCallback$1(BikClientStreamFactory.java:40)
	at com.github.knokko.bitser.kwik.BikClientStreamFactory$$Lambda/0x00000000733a0230.run(Unknown Source)
	at java.lang.Thread.runWith(java.base@25.0.5.1/Thread.java:1487)
	at java.lang.Thread.run(java.base@25.0.5.1/Thread.java:1474)

StreamInputStreamImpl line 192: addMonitor.wait(waitPeriod);
 */