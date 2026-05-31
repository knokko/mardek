package mardek.state.saves

import com.github.knokko.bitser.io.BitOutputStream
import com.github.knokko.bitser.Bitser
import com.github.knokko.bitser.exceptions.BitserException
import mardek.content.BITSER
import mardek.content.Content
import mardek.state.ingame.CampaignState
import mardek.state.ingame.area.AreaState
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.Locale
import kotlin.io.path.deleteExisting

/**
 * The `SavesFolderManager` keeps track of the save files in the saves folder/directory. It can list the saved
 * campaign names, as well as the saves in those campaigns.
 */
class SavesFolderManager(
	/**
	 * The location of the `saves` directory, which is `~/MARDEK/saves` or `Documents/MARDEK/saves` by default.
	 * Different directories will be used during unit tests.
	 */
	val root: File = SAVES_DIRECTORY
) {

	/**
	 * Lists all campaign names stored on this computer with at least 1 `SaveFile`. Note that the result may contain
	 * some 'false positives' in some edge cases where the saves folder contains third-party subdirectories.
	 */
	fun getCampaignNames(): Array<String> {
		val rawFolders = root.listFiles() ?: return emptyArray()
		return rawFolders.filter { folder ->
			val rawSaveFiles = folder.list()
			rawSaveFiles != null && rawSaveFiles.any { file -> file.endsWith(".bits") }
		}.map { it.name }.sorted().toTypedArray()
	}

	/**
	 * Lists all saves of the campaign with the given name. When this campaign doesn't have any saves, an empty array
	 * will be returned.
	 */
	fun getSaves(campaignName: String): Array<SaveFile> {
		val savesFolder = File("$root/$campaignName")
		val rawSavesFiles = savesFolder.listFiles() ?: return emptyArray()

		val result = rawSavesFiles.map {
			SaveFile.scan(it)
		}.filterNotNull().sortedBy { -it.timestamp }.toTypedArray()
		return result
	}

	/**
	 * Checks whether the given `name` is a valid name for a campaign, at least on the current computer.
	 * Campaign names must be valid directory names, and must not contain a few forbidden characters.
	 *
	 * This method tries to create a directory with the given name in the saves folder, and tries to create a file in
	 * that directory. If the file cannot be created, the campaign name is deemed invalid.
	 */
	fun isCampaignNameValid(name: String): Boolean {
		if (name.isEmpty() || name.contains('/') || name.contains('\\')) return false

		val campaignFolder = File("$root/$name")
		if (campaignFolder.exists()) return false

		if (!campaignFolder.mkdirs()) return false
		val testFile = File("$campaignFolder/crystal-${10 * System.currentTimeMillis()}.bits")
		try {
			val createdFile = Files.createFile(testFile.toPath())
			createdFile.deleteExisting()
			return true
		} catch (_: IOException) {
			return false
		} finally {
			campaignFolder.deleteRecursively()
		}
	}

	/**
	 * Creates a new save for the given campaign state and name. This will:
	 * 1. Create a directory named [campaignName] in the [root] directory of this saves manager
	 * (unless it already exists).
	 * 2. Create a file in that directory, containing all the data of [campaignState]. The name of that file will
	 * contain the current date/time, as well as [type].
	 */
	fun createSave(content: Content, campaignState: CampaignState, campaignName: String, type: SaveFile.Type): Boolean {
		val campaignFolder = File("$root/$campaignName")
		if (!campaignFolder.isDirectory) {
			if (!campaignFolder.mkdirs()) return false
		}

		val fileName = "${type.name.lowercase(Locale.ROOT)}-${System.currentTimeMillis()}.bits"
		return writeSaveTo(content, campaignState, File("$campaignFolder/$fileName"))
	}

	/**
	 * Serializes [campaignState], and stores its data in [destinationFile].
	 *
	 * This method is used by [createSave], and should normally not be called directly by other methods.
	 * This method is only public because that is convenient for unit-testing.
	 */
	fun writeSaveTo(content: Content, campaignState: CampaignState, destinationFile: File): Boolean {
		val partyLevel = campaignState.usedPartyMembers().maxOf { it.state.currentLevel }
		val areaName = when (val state = campaignState.state) {
			is AreaState -> state.area.properties.displayName
			else -> ""
		}
		val saveInfo = SaveInfo(
			areaName = areaName,
			party = campaignState.allPartyMembers().map { it?.first?.id }.toTypedArray(),
			playTime = campaignState.statistics.totalTime,
			partyLevel = partyLevel,
			chapter = campaignState.story.evaluate(content.story.fixedVariables.chapter) ?: -1,
		)

		val infoBytes = BITSER.toBytes(saveInfo, Bitser.BACKWARD_COMPATIBLE)
		if (infoBytes.size >= 256) throw Error("What?! info size is ${infoBytes.size}")

		try {
			val rawOutput = BufferedOutputStream(Files.newOutputStream(destinationFile.toPath()))
			rawOutput.write(infoBytes.size)
			rawOutput.write(infoBytes)

			val bitOutput = BitOutputStream(rawOutput)
			BITSER.serialize(campaignState, bitOutput, content, Bitser.BACKWARD_COMPATIBLE)
			bitOutput.finish()
			rawOutput.flush()
			rawOutput.close()
			return true
		} catch (failed: IOException) {
			failed.printStackTrace()
			return false
		}
	}

	/**
	 * Loads the [CampaignState] that is stored in [saveFile].
	 *
	 * This method will return `null` when this fails for some reason (and print the error to `System.err`).
	 */
	fun loadSave(content: Content, saveFile: SaveFile): CampaignState? {
		try {
			val rawInput = BufferedInputStream(Files.newInputStream(saveFile.file.toPath()))
			val numInfoBytes = rawInput.read()
			rawInput.skip(numInfoBytes.toLong())
			return CampaignState.loadSave(content, rawInput)
		} catch (failed: IOException) {
			failed.printStackTrace()
			return null
		} catch (failed: BitserException) {
			failed.printStackTrace()
			return null
		}
	}
}
