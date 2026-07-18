package mardek.game

import com.github.knokko.bitser.io.BitInputStream
import mardek.content.BITSER
import mardek.content.Content
import mardek.content.ui.TitleScreenContent
import java.io.File
import java.nio.file.Files

internal fun loadTitleScreenContent(): TitleScreenContent {
	val inputPath = File("${Content.RESOURCES_DIRECTORY}/title-screen.bits").toPath()
	val input = BitInputStream(Files.newInputStream(inputPath))
	val titleScreenContent = BITSER.deserialize(TitleScreenContent::class.java, input)
	input.close()
	return titleScreenContent
}
