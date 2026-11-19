package mardek.editor.client.file

import androidx.compose.foundation.layout.Column
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
import com.github.knokko.zstd.DecompressingInputStream
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import mardek.content.BITSER
import mardek.content.ContentWrapper
import mardek.editor.EditorView
import mardek.editor.client.TestRunState
import java.nio.file.Files
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import mardek.editor.MardekOS
import mardek.editor.client.navbar.NavbarCategory

@Composable
fun ExportingMenu(wrappedContentStruct: BitClientStruct, testRunState: TestRunState) {
	Row {
		Text("Export/test the game...", style = LocalTextStyle.current.copy(fontSize = 1.5.em, fontWeight = FontWeight.Bold))
	}

	var wrappedContent by remember { mutableStateOf<ContentWrapper?>(null) }
	var syncState by remember { mutableStateOf(0) }
	var syncCompressedSize by remember { mutableStateOf(0) }
	var syncDownloadedSoFar by remember { mutableStateOf(0) }
	var lastSync by remember { mutableStateOf<Long?>(null) }

	val mutateScope = rememberCoroutineScope()

	if (wrappedContent == null && syncState == 0) {
		Row { Text("You need to synchronize before you can export or test") }
	}

	lastSync?.let {
		val syncTime = Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault())
		val syncTimeString = syncTime.format(LocalDateTime.Format {
			amPmHour(Padding.NONE)
			char(':')
			minute()
			amPmMarker("am", "pm")
		})
		Text("Last synchronization was at $syncTimeString")
	}

	Row {
		Button(
			onClick = {
				mutateScope.launch { syncState = 1 }
				wrappedContentStruct.executeOperation(EditorView.operationSyncForExport) { output, streamFactory ->
					val responseID = streamFactory.addResponseCallback { response ->
						mutateScope.launch { syncState = 3 }

						val compressedFile = Files.createTempFile("", ".bits")
						val compressedOutput = Files.newOutputStream(compressedFile)

						val compressedSize = IntegerBitser.decodeFullInt(response)
						mutateScope.launch { syncCompressedSize = compressedSize }

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
							mutateScope.launch { syncDownloadedSoFar = compressedSize - remaining }
						}
						compressedOutput.flush()
						compressedOutput.close()

						mutateScope.launch { syncState = 4 }

						val bitInput = BitInputStream(DecompressingInputStream(compressedFile))
						val parsedContent = BITSER.deserialize(ContentWrapper::class.java, bitInput)
						bitInput.close()

						mutateScope.launch {
							wrappedContent = parsedContent
							syncState = 0
							lastSync = System.currentTimeMillis()
						}
						System.gc()
					}
					IntegerBitser.encodeVariableIntegerUsingTerminatorBits(
						responseID.toLong(), 0L, Long.MAX_VALUE, output
					)

					mutateScope.launch { syncState = 2 }
				}
			},
			enabled = syncState == 0,
			content = { Text("Synchronize${ if (lastSync == null) "" else " again"}") }
		)

	}

	if (syncState != 0) {
		val syncStateName = when (syncState) {
			1 -> "sending sync request..."
			2 -> "awaiting sync response..."
			3 -> "downloading sync response..."
			4 -> "parsing sync response..."
			else -> "????"
		}
		Row { Text("Synchronization progress: $syncState / 4: $syncStateName") }
		if (syncState == 3) {
			Row { Text("Downloaded ${(syncDownloadedSoFar / 1000_000)} / ${syncCompressedSize / 1000_000} MB") }
		}
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
	}

	if (runState > 0) {
		Row {
			val runStateName = when (runState) {
				1 -> "preparation"
				2 -> "exporting game files"
				3 -> "initializing game"
				4 -> "running game"
				else -> "???"
			}
			Text("Progress: $runState / 4: $runStateName")
		}
	}

	Row {
		for (os in MardekOS.entries) {
			Column {
				Button(
					onClick = {
						val outputFile = java.io.File("mardek-$os.zip")
						zipGame(wrappedContent, outputFile, os)
					}
				) { Text("Export for $os") }
			}
		}
	}
}
