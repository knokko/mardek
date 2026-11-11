package mardek.importer

import com.github.knokko.bitser.Bitser
import com.github.knokko.bitser.io.BitOutputStream
import com.github.knokko.vk2d.resource.CompressingOutputStream
import mardek.content.BITSER
import mardek.content.Content
import mardek.exporting.ContentWrapper
import mardek.exporting.exportAndPrepareVk2d
import mardek.exporting.exportIcons
import mardek.exporting.exportTitleScreen
import mardek.importer.util.classLoader
import mardek.importer.util.compressKimSprite1
import mardek.importer.util.loadBc7Sprite
import mardek.importer.util.projectFolder
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.system.exitProcess
import kotlin.time.measureTime

fun main() {
	if (!File("$projectFolder/flash/all-shapes-x4").exists()) {
		throw RuntimeException("You need to run mardek.importer.converter.SvgShapeConverter first")
	}

	var succeeded = false
	try {
		val content = importVanillaContent()
		val wrappedContent = wrapContent(content)

		println("exporting content...")
		val mainExportTime = measureTime {
			exportAndPrepareVk2d(
				dataFile = File("${Content.RESOURCES_DIRECTORY}/content.bits"),
				vk2dFile = File("${Content.RESOURCES_DIRECTORY}/content.vk2d"),
				compressionLevel = 15,
				cacheDirectory = File("$projectFolder/flash/bc-cache"),
				target = content,
			)
		}

		println("exporting content took $mainExportTime")

		exportIcons(wrappedContent, Files.newOutputStream(File(
			"${Content.RESOURCES_DIRECTORY}/icons.bin"
		).toPath()), true)

		exportTitleScreen(
			dataFile = File("${Content.RESOURCES_DIRECTORY}/title-screen.bits"),
			vk2dFile = File("${Content.RESOURCES_DIRECTORY}/title-screen.vk2d"),
			wrapped = wrappedContent,
		)

		println("preparing editor server content....")
		val preparationTime = measureTime {
			val fatOutput = BitOutputStream(CompressingOutputStream(
				File("$projectFolder/editor-server/content.bits").toPath(), 1
			))
			BITSER.serialize(wrappedContent, fatOutput, Bitser.BACKWARD_COMPATIBLE)
			fatOutput.finish()
		}
		println("preparing editor server content took $preparationTime")

		succeeded = true
	} catch (failure: Throwable) {
		failure.printStackTrace()
	} finally {
		// For some reason, the process is not terminated by default
		exitProcess(if (succeeded) 0 else -1)
	}
}

private fun cursorSprite(cursor: String) = compressKimSprite1(ImageIO.read(classLoader.getResource(
	"mardek/importer/cursors/$cursor.png"
)))

private fun wrapContent(content: Content) = ContentWrapper(
	content = content,
	neutralMusicNote = loadBc7Sprite("mardek/importer/audio/categories/AllMusic.png"),
	musicNoteShadow = loadBc7Sprite("mardek/importer/audio/categories/Shadow.png"),
	playMusicIcon = loadBc7Sprite("mardek/importer/audio/PlayButton.png"),
	pauseMusicIcon = loadBc7Sprite("mardek/importer/audio/PauseButton.png"),
	playMusicHoveredIcon = loadBc7Sprite("mardek/importer/audio/PlayButtonHovered.png"),
	pauseMusicHoveredIcon = loadBc7Sprite("mardek/importer/audio/PauseButtonHovered.png"),
	applicationIcon = compressKimSprite1(ImageIO.read(classLoader.getResource(
		"mardek/importer/inventory/itemsheet_misc.png"
	)).getSubimage(288, 32, 16, 16)),
	inventoryCursor = cursorSprite("inventory"),
	pointerCursor = cursorSprite("pointer"),
	grabCursor = cursorSprite("grab"),
)
