package mardek.editor.client.file

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import com.github.knokko.bitser.Bitser
import com.github.knokko.bitser.IntegerBitser
import com.github.knokko.bitser.connection.BitClientStruct
import com.github.knokko.bitser.io.BitInputStream
import kotlinx.coroutines.launch
import mardek.content.BITSER
import mardek.editor.EditorView
import mardek.exporting.ContentWrapper
import mardek.exporting.DecompressingInputStream
import java.lang.Math.toIntExact
import java.nio.file.Files
import java.util.concurrent.BlockingQueue

@Composable
fun ExportingMenu(wrappedContentStruct: BitClientStruct, testRunQueue: BlockingQueue<ContentWrapper>) {
	Row {
		Text("Export/test the game...", style = LocalTextStyle.current.copy(fontSize = 1.5.em, fontWeight = FontWeight.Bold))
	}

	var wrappedContent by remember { mutableStateOf<ContentWrapper?>(null) }
	var exportState by remember { mutableStateOf(0) }
	var lastSync by remember { mutableStateOf<Long?>(null) }

	val mutateScope = rememberCoroutineScope()
	Row {
		Button(
			onClick = {
				mutateScope.launch { exportState = 1 }
				wrappedContentStruct.executeOperation(EditorView.operationSyncForExport) { output, streamFactory ->
					val responseID = streamFactory.addResponseCallback { response ->
						mutateScope.launch { exportState = 3 }
						val compressedSize = toIntExact(IntegerBitser.decodeVariableIntegerUsingTerminatorBits(
							0L, Long.MAX_VALUE, response
						))
						val compressedContent = ByteArray(compressedSize)
						response.read(compressedContent)
						mutateScope.launch { exportState = 4 }

						val compressedFile = Files.createTempFile("", ".bits").toFile()
						Files.write(compressedFile.toPath(), compressedContent)

						val parsedContent = BITSER.deserialize(
							ContentWrapper::class.java,
							BitInputStream(DecompressingInputStream(compressedFile)),
							Bitser.BACKWARD_COMPATIBLE
						)
						mutateScope.launch {
							wrappedContent = parsedContent
							exportState = 0
							lastSync = System.currentTimeMillis()
						}
						System.gc()
					}
					IntegerBitser.encodeVariableIntegerUsingTerminatorBits(
						responseID.toLong(), 0L, Long.MAX_VALUE, output
					)

					mutateScope.launch { exportState = 2 }
				}
			},
			enabled = exportState == 0,
			content = { Text("Synchronize${ if (lastSync == null) "" else " again"}") }
		)

		lastSync?.let { Text("Last sync: $it") }
	}

	if (wrappedContent == null) {
		Row { Text("You need to synchronize before you can export or test") }
	}

	if (exportState != 0) {
		Row { Text("Synchronization progress: $exportState / 5") }
	}

	wrappedContent?.let { ExportMenuBody(it, testRunQueue) }
}

@Composable
private fun ExportMenuBody(wrappedContent: ContentWrapper, testRunQueue: BlockingQueue<ContentWrapper>) {
	Row {
		Button(onClick = { testRunQueue.offer(wrappedContent) }, content = { Text("Launch test run") })
	}
	// TODO BITSER Export buttons
}
