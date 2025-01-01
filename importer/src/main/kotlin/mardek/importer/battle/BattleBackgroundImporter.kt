package mardek.importer.battle

import com.jpexs.decompiler.flash.SWF
import com.jpexs.decompiler.flash.tags.DefineSpriteTag
import com.jpexs.decompiler.flash.tags.FrameLabelTag
import com.jpexs.decompiler.flash.tags.PlaceObject2Tag
import mardek.assets.battle.BattleAssets
import mardek.assets.battle.BattleBackground
import mardek.assets.sprite.BcSprite
import mardek.importer.util.resourcesFolder
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO

private fun extractFromFlash(swfFile: File) {
	val input = Files.newInputStream(swfFile.toPath())
	val swf = SWF(input, true)
	input.close()

	val backgroundsTag = swf.tags.find { it.uniqueId == "2186" }!! as DefineSpriteTag

	val shapeMapping = mutableMapOf<String, MutableList<Int>>()
	var currentLabel = ""
	for (tag in backgroundsTag.tags) {
		if (tag is FrameLabelTag) currentLabel = tag.labelName
		if (currentLabel.isNotEmpty() && currentLabel != "BELFAN" && tag is PlaceObject2Tag) {
			shapeMapping.computeIfAbsent(currentLabel) { mutableListOf() }.add(tag.characterId)
		}
	}
	println("mapping is $shapeMapping")

	val extractedShapesFolder = File("D:\\images/shapes")
	val destinationFolder = File("importer/src/main/resources/mardek/importer/battle/backgrounds")

	destinationFolder.mkdirs()

	for (entry in shapeMapping) {
		if (entry.value.size > 1) {
			println("do ${entry.key} manually")
			continue
		}
		val sourceFile = File("$extractedShapesFolder/${entry.value[0]}.png")
		val destinationFile = File("$destinationFolder/${entry.key}.png")
		if (destinationFile.exists()) destinationFile.delete()
		Files.copy(sourceFile.toPath(), destinationFile.toPath())
	}
}

fun main() {
	// Copy MARDEK.swf from Steam to ./flash/MARDEK.swf to make this work
	extractFromFlash(File("flash/MARDEK.swf"))
}

internal fun countTranslucentPixels(image: BufferedImage): Int {
	var translucentPixels = 0
	for (y in 0 until image.height) {
		for (x in 0 until image.width) {
			val alpha = Color(image.getRGB(x, y), true).alpha
			if (alpha in 30..200) translucentPixels += 1
		}
	}
	return translucentPixels
}

internal fun importBattleBackgrounds(assets: BattleAssets) {
	val backgroundsFolder = File("$resourcesFolder/battle/backgrounds")
	val threadPool = Executors.newFixedThreadPool(4)
	for (backgroundImageFile in backgroundsFolder.listFiles()!!) {
		threadPool.submit {
			var bufferedImage = ImageIO.read(backgroundImageFile)
			val useBc7 = countTranslucentPixels(bufferedImage) > 10_000
			if (!useBc7 && (bufferedImage.width % 4 != 0 || bufferedImage.height % 4 != 0)) {
				bufferedImage = bufferedImage.getSubimage(
					0, 0, 4 * (bufferedImage.width / 4), 4 * (bufferedImage.height / 4)
				)
			}

			val sprite = BcSprite(bufferedImage.width, bufferedImage.height, if (useBc7) 7 else 1)
			sprite.bufferedImage = bufferedImage

			synchronized(backgroundsFolder) {
				assets.backgrounds.add(BattleBackground(backgroundImageFile.nameWithoutExtension, sprite))
			}
		}
	}
	threadPool.shutdown()
	if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) throw RuntimeException("Battle background importer timed out")
}
