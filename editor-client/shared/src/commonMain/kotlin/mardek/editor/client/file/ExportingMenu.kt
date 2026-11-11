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
import com.github.knokko.bitser.IntegerBitser
import com.github.knokko.bitser.connection.BitClientStruct
import com.github.knokko.bitser.io.BitInputStream
import com.github.knokko.vk2d.resource.DecompressingInputStream
import kotlinx.coroutines.launch
import mardek.content.BITSER
import mardek.editor.EditorView
import mardek.editor.client.TestRunState
import mardek.exporting.ContentWrapper
import java.nio.file.Files

@Composable
fun ExportingMenu(wrappedContentStruct: BitClientStruct, testRunState: TestRunState) {
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

						val compressedFile = Files.createTempFile("", ".bits")
						val compressedOutput = Files.newOutputStream(compressedFile)

						val compressedSize = IntegerBitser.decodeFullInt(response)

						val receiveBuffer = ByteArray(1024 * 1024)
						var remaining = compressedSize
						while (remaining > 0) {
							if (remaining < receiveBuffer.size) {
								val lastBuffer = ByteArray(remaining)
								response.read(lastBuffer)
								compressedOutput.write(lastBuffer)
								break
							} else {
								response.read(receiveBuffer)
								compressedOutput.write(receiveBuffer)
								remaining -= receiveBuffer.size
							}
						}
						compressedOutput.flush()
						compressedOutput.close()

						mutateScope.launch { exportState = 4 }

						val bitInput = BitInputStream(DecompressingInputStream(compressedFile))
						val parsedContent = BITSER.deserialize(ContentWrapper::class.java, bitInput)
						bitInput.close()

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

	wrappedContent?.let { ExportMenuBody(it, testRunState) }
}

@Composable
private fun ExportMenuBody(wrappedContent: ContentWrapper, testRunState: TestRunState) {
	var runState by remember { mutableStateOf(0) }
	val mutateScope = rememberCoroutineScope()

	Row {
		Button(
			onClick = { testRunState.requestStart(wrappedContent) { mutateScope.launch { runState = it } } },
			enabled = runState == 0,
			content = { Text("Launch test run") },
		)
		if (runState > 0) {
			Text("Progress: $runState / 7")
		}
	}
	// TODO BITSER Export buttons
}
