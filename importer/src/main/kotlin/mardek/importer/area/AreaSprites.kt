package mardek.importer.area

import com.github.knokko.compressor.Kim1Compressor
import mardek.assets.area.Direction
import mardek.assets.area.OptimizedArea
import mardek.assets.area.ParsedArea
import mardek.assets.area.objects.AreaSwitch
import mardek.assets.area.objects.SwitchColor
import mardek.assets.area.sprites.DirectionalSpritesheet
import mardek.assets.area.sprites.KimImage
import mardek.assets.area.sprites.ObjectSpritesheet
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.DataOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import javax.imageio.ImageIO
import kotlin.collections.ArrayList

class AreaSprites {

	val characters = mutableListOf<DirectionalSpritesheet>()
	val objects = mutableListOf<ObjectSpritesheet>()
	private val tileSpriteOffsets = mutableMapOf<Pair<String, Int>, IntArray>()

	private val kimSprites = ArrayList<KimImage>()
	private val highTileSprites = ArrayList<KimImage>()
	private var kimOffset = 0
	private var highTileOffset = 0

	fun register(area: ParsedArea): OptimizedArea {
		val tiles = parseTilesheet(area.tilesheetName)
		val lowTiles = IntArray(area.width * area.height)
		val highTiles = IntArray(area.width * area.height)

		fun registerSprite(sprite: KimImage): Int {
			val result = kimOffset
			kimOffset += sprite.data.size
			kimSprites.add(sprite)
			return result
		}

		fun registerSheet(sheet: ObjectSpritesheet): ObjectSpritesheet {
			if (sheet.indices == null) sheet.indices = sheet.frames!!.map(::registerSprite).toIntArray()
			return sheet
		}

		fun registerSwitch(switchObject: AreaSwitch, sheetName: String) {
			val offSheet = registerSheet(getObject(sheetName, frameIndex = SwitchColor.Off.ordinal, numFrames = 1))
			val onSheet = registerSheet(getObject(sheetName, frameIndex = switchObject.color.ordinal, numFrames = 1))
			switchObject.offSpriteOffset = offSheet.indices!![0]
			switchObject.onSpriteOffset = onSheet.indices!![0]
		}

		for (y in 0 until area.height) {
			for (x in 0 until area.width) {
				val tile = tiles.tiles[area.getTileId(x, y)!!]!!
				if (tile.sprites.size > 3) throw UnsupportedOperationException("Tile too high: ${tile.sprites.size}")
				if (tileSpriteOffsets.containsKey(Pair(area.tilesheetName, tile.id))) continue
				tileSpriteOffsets[Pair(area.tilesheetName, tile.id)] = IntArray(tile.sprites.size)
				for ((index, image) in tile.sprites.withIndex()) {
					val sprite = compress(image)
					if (index == 0) {
						tileSpriteOffsets[Pair(area.tilesheetName, tile.id)]!![index] = registerSprite(sprite)
					} else {
						val spriteIndex = highTileOffset
						highTileOffset += sprite.data.size
						highTileSprites.add(sprite)
						tileSpriteOffsets[Pair(area.tilesheetName, tile.id)]!![index] = spriteIndex
					}
				}
			}
		}

		for (y in 0 until area.height) {
			for (x in 0 until area.width) {
				val mainTileID = area.getTileId(x, y)!!
				val tile = tiles.tiles[mainTileID]!!

				var lowTile = tileSpriteOffsets[Pair(area.tilesheetName, mainTileID)]!!.last()
				if (lowTile >= 1 shl 24) throw UnsupportedOperationException("Tile sprite index too large: $lowTile")

				if (tile.canWalkOn) lowTile = lowTile or (1 shl 24)
				lowTile = lowTile or (tile.waterType.ordinal shl 25)
				lowTiles[x + y * area.width] = lowTile

				val midTileId = area.getTileId(x, y + 1)
				var midPart = -1
				if (midTileId != null) {
					val midOffsets = tileSpriteOffsets[Pair(area.tilesheetName, midTileId)]!!
					if (midOffsets.size > 1) {
						if (midOffsets[1] < 0 || midOffsets[1] > UShort.MAX_VALUE.toInt()) {
							throw UnsupportedOperationException("Uh ooh: mid tile sprite offset is ${midOffsets[1]}")
						}
						midPart = midOffsets[1]
					}
				}

				val highTileId = area.getTileId(x, y + 2)
				var highPart = -1
				if (highTileId != null) {
					val highOffsets = tileSpriteOffsets[Pair(area.tilesheetName, highTileId)]!!
					if (highOffsets.size > 2) {
						if (highOffsets[2] < 0 || highOffsets[2] > UShort.MAX_VALUE.toInt()) {
							throw UnsupportedOperationException("Uh ooh: high tile sprite offset is {$highOffsets[2")
						}
						highPart = highOffsets[0]
					}
				}

				highTiles[x + y * area.width] = midPart or (highPart shl 16)
			}
		}

		for (character in area.objects.characters) {
			val sheet = getCharacter(character.spritesheetName.replace("spritesheet_", ""))
			if (sheet.indices != null) continue
			sheet.indices = sheet.sprites!!.map(::registerSprite).toIntArray()
			character.spritesheet = sheet
		}
		for (decoration in area.objects.decorations) {
			if (decoration.spritesheetName == null) continue
			val sheet = getObject(
				decoration.spritesheetName!!, offsetY = decoration.spritesheetOffsetY ?: 0, height = decoration.spriteHeight
			)
			decoration.spritesheet = registerSheet(sheet)
		}
		for (door in area.objects.doors) {
			val height = if (door.spritesheetName.contains("BIG")) 32 else 16
			val sheet = getObject(
				door.spritesheetName, offsetY = door.spriteRow * height, height = height
			)
			door.spritesheet = registerSheet(sheet)
		}
		for (obj in area.objects.objects) {
			val sheet = getObject(
				obj.spritesheetName, frameIndex = obj.firstFrameIndex ?: 0, numFrames = obj.numFrames
			)
			obj.spritesheet = registerSheet(sheet)
		}
		for (portal in area.objects.portals) {
			val sheet = getObject("portal")
			portal.spritesheet = registerSheet(sheet)
		}
		for (gate in area.objects.switchGates) registerSwitch(gate, "switch_gate")
		for (orb in area.objects.switchOrbs) registerSwitch(orb, "switch_orb")
		for (platform in area.objects.switchPlatforms) registerSwitch(platform, "switch_platform")
		for (transition in area.objects.transitions) {
			if (transition.arrow == null) continue
			val sheet = getObject(
				"trans_arrows", numFrames = 1,
				frameIndex = Direction.entries.find { it.abbreviation.uppercase(Locale.ROOT) == transition.arrow}!!.ordinal
			)
			transition.arrowSprite = registerSheet(sheet)
		}

		println("kimOffset is $kimOffset and high offset is $highTileOffset")

		return OptimizedArea(
			width = area.width, height = area.height,
			lowTileGrid = lowTiles, highTileGrid = highTiles,
			objects = area.objects, randomBattles = area.randomBattles,
			flags = area.flags, properties = area.properties
		)
	}

	fun writeSprites(output: OutputStream) {
		val data = DataOutputStream(output)
		data.writeInt(kimOffset)
		for (sprite in kimSprites) {
			for (value in sprite.data) data.writeInt(value)
		}

		data.writeInt(highTileOffset)
		for (sprite in highTileSprites) {
			for (value in sprite.data) data.writeInt(value)
		}
		data.flush()
	}

	private fun compress(image: BufferedImage): KimImage {
		val pixelBuffer = ByteBuffer.allocate(4 * image.width * image.height)
		for (y in 0 until image.height) {
			for (x in 0 until image.width) {
				val color = Color(image.getRGB(x, y), true)
				pixelBuffer.put(color.red.toByte())
				pixelBuffer.put(color.green.toByte())
				pixelBuffer.put(color.blue.toByte())
				pixelBuffer.put(color.alpha.toByte())
			}
		}
		pixelBuffer.position(0)
		val compressor = Kim1Compressor(pixelBuffer, image.width, image.height, 4)
		val compressedBuffer = ByteBuffer.allocate(4 * compressor.intSize).order(ByteOrder.nativeOrder())
		compressor.compress(compressedBuffer)

		return KimImage(IntArray(compressor.intSize) { index -> compressedBuffer.getInt(4 * index)})
	}

	fun getCharacter(flashName: String): DirectionalSpritesheet {
		for (sheet in characters) {
			if (sheet.flashName == flashName) return sheet
		}

		val spritePath = "sheets/character/$flashName.png"
		val input = AreaSprites::class.java.getResourceAsStream(spritePath)
			?: throw IllegalArgumentException("Can't find sprite at $spritePath")
		val sheetImage = ImageIO.read(input)
		input.close()

		val numSprites = sheetImage.width / 16

		val sheet = DirectionalSpritesheet(flashName)
		val sprites = (0 until numSprites).map { sheetImage.getSubimage(it * 16, 0, 16, sheetImage.height) }

		sheet.sprites = sprites.map(::compress).toTypedArray()

		characters.add(sheet)
		return sheet
	}

	fun getObject(
		flashName: String, frameIndex: Int = 0, numFrames: Int? = null,
		offsetY: Int = 0, height: Int? = null
	): ObjectSpritesheet {
		for (sheet in objects) {
			if (sheet.flashName == flashName && sheet.offsetY == offsetY &&
				sheet.frameIndex == frameIndex && sheet.numFrames == numFrames) return sheet
		}

		val imageName = flashName.replace("spritesheet_", "").replace("obj_", "")
		val imagePath = "sheets/objects/$imageName.png"
		val input = AreaSprites::class.java.getResourceAsStream(imagePath) ?:
				throw IllegalArgumentException("Can't get resource $imagePath")
		val sheetImage = ImageIO.read(input)
		input.close()

		var spriteHeight = height ?: sheetImage.height
		// TODO Maybe move this if logic elsewhere
		if (flashName == "chests") spriteHeight = 16

		// Disgusting formula, but I can't find anything logical
		val spriteWidth = if (flashName.startsWith("spritesheet_")) {
			16 * (sheetImage.height / 16)
		} else 16

		val images = ((0 until (numFrames ?: (sheetImage.width / spriteWidth)))).map {
			sheetImage.getSubimage(spriteWidth * (frameIndex + it), offsetY, spriteWidth, spriteHeight)
		}

		val result = ObjectSpritesheet(flashName = flashName, frameIndex = frameIndex, offsetY = offsetY, numFrames = numFrames)
		result.frames = images.map(::compress).toTypedArray()
		objects.add(result)
		return result
	}
}
