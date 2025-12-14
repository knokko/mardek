package mardek.state.saves

import com.github.knokko.bitser.io.BitInputStream
import com.github.knokko.bitser.Bitser
import mardek.content.Content
import mardek.state.ingame.CampaignState
import java.io.File
import java.lang.Long.parseLong
import java.nio.file.Files
import java.util.Locale

/**
 * The `Bitser` instance that is used to decode/encode `SaveInfo`s and load/save `SaveFile`s
 */
val savesBitser = Bitser(true)

/**
 * Each `SaveFile` contains the data about 1 campaign at 1 time instant. Players can start the game by either starting
 * a *New Game*, or by loading a `SaveFile`. Players can create `SaveFile`s at save crystals. Furthermore, I intend to
 * add auto-saves in the future, and perhaps more opportunities.
 *
 * Use `SaveFile.scan(file)` to get instances of this class.
 */
class SaveFile private constructor(
	/**
	 * The file where the campaign state is stored
	 */
	val file: File,

	/**
	 * Some information about the campaign state that is displayed in the "Load Game" menu
	 */
	val info: SaveInfo,

	/**
	 * The name of the campaign for which this file was saved
	 */
	val campaignName: String,

	/**
	 * The type of `SaveFile`: basically how the save file was created (typically `Type.Crystal`)
	 */
	val type: Type,

	/**
	 * The result of `System.currentTimeMillis()` at the time the save was created
	 */
	val timestamp: Long,
	private val dataOffset: Long,
) {

	/**
	 * The possible types of `SaveFile`s
	 */
	enum class Type {
		/**
		 * The player created this save using a save crystal
		 */
		Crystal,

		/**
		 * The game created this save automatically
		 */
		Auto,

		/**
		 * The player used Control+S to save without save crystal
		 */
		Cheat,
	}

	/**
	 * Attempts to load the `CampaignState` stored in this `SaveFile`. Returns the deserialized `CampaignState` if
	 * successful, and returns `null` on failures. When this method fails, the cause is printed to stderr.
	 */
	fun load(content: Content): CampaignState? {
		try {
			val input = Files.newInputStream(file.toPath())
			input.skipNBytes(dataOffset)
			return CampaignState.loadSave(content, input)
		} catch (failed: Exception) {
			println("Failed to load save $file: ${failed.message}")
			return null
		}
	}

	companion object {

		/**
		 * Tries to extract the `SaveFile` info that is presumably stored in `file`. Returns the `SaveFile` info on
		 * success, or `null` when an error occurs (e.g. the file was deleted or corrupted)
		 */
		fun scan(file: File): SaveFile? {
			if (file.extension != "bits") return null
			val name = file.nameWithoutExtension
			val campaignName = file.parentFile.name

			val splitIndex = name.indexOf('-')
			if (splitIndex == -1) return null
			val lowerTypeName = name.substring(0 until splitIndex).lowercase(Locale.ROOT)
			if (lowerTypeName.isEmpty()) return null
			val typeName = lowerTypeName.take(1).uppercase(Locale.ROOT) +
					lowerTypeName.substring(1).lowercase(Locale.ROOT)
			val rawTimestamp = name.substring(splitIndex + 1)

			val type = try { Type.valueOf(typeName) } catch (_: IllegalArgumentException) { return null }
			val timestamp = try { parseLong(rawTimestamp ) } catch (_: NumberFormatException) { return null }

			try {
				val input = Files.newInputStream(file.toPath())
				val infoSize = input.read().toUByte()
				val dataOffset = infoSize.toLong() + 1L
				val info = savesBitser.deserialize(
					SaveInfo::class.java,
					BitInputStream(input),
					Bitser.BACKWARD_COMPATIBLE,
				)
				return SaveFile(file, info, campaignName, type, timestamp, dataOffset)
			} catch (failed: Exception) {
				println("Failed to scan $file: ${failed.message}")
				return null
			}
		}
	}
}
