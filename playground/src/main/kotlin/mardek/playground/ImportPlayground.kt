package mardek.playground

import com.jpexs.decompiler.flash.SWF
import com.jpexs.decompiler.flash.tags.DefineShape2Tag
import com.jpexs.decompiler.flash.tags.DefineShape3Tag
import com.jpexs.decompiler.flash.tags.DefineShapeTag
import com.jpexs.decompiler.flash.tags.DefineSpriteTag
import com.jpexs.decompiler.flash.tags.FrameLabelTag
import com.jpexs.decompiler.flash.tags.PlaceObject2Tag
import com.jpexs.decompiler.flash.tags.ShowFrameTag
import com.jpexs.decompiler.flash.tags.SoundStreamHead2Tag
import com.jpexs.decompiler.flash.types.MATRIX
import com.jpexs.decompiler.flash.types.RECT
import org.joml.Matrix3f
import org.joml.Matrix3x2f
import org.joml.Vector2f
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_ARGB
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.min

fun main() {
	// The next line will work after you copy MARDEK.swf from your steamgames to the flash directory of this repository
	// I'm reluctant to drop this file in the repository
	val input = Files.newInputStream(File("flash/MARDEK.swf").toPath())
	val swf = SWF(input, true)
	input.close()

	// deugan = 2427, emela = 2551
	val monsterTag = swf.tags.find { it.uniqueId == "2551" }!! as DefineSpriteTag
	println("frame count is ${monsterTag.frameCount}")

	val monster = parseCreature(monsterTag)

	val targetImage = BufferedImage(1024, 1024, TYPE_INT_ARGB)
	val graphics = targetImage.createGraphics()

	for (part in monster.parts) {
		println("shape names are ${part.sprites.shapes.map { it.name }}")
		val shape = part.sprites.shapes.firstOrNull()
		if (shape != null) {
			val image = ImageIO.read(File("flash/shapes/${shape.shapeID}.png"))
			val jomlMatrix = Matrix3x2f()
			val (scaleX, scaleY) = if (part.matrix.hasScale) Pair(part.matrix.scaleX, part.matrix.scaleY) else Pair(1f, 1f)

			val rect = part.sprites.rect

			//jomlMatrix.translate((part.matrix.translateX + scaleX * shape.rect.Xmin) / 20f, (part.matrix.translateY + shape.rect.Ymin) / 20f)
			jomlMatrix.translate((part.matrix.translateX + scaleX * shape.rect.Xmin) / 20f, (part.matrix.translateY + shape.rect.Ymin) / 20f)



			println("rotate? ${part.matrix.hasRotate}")
			// TODO Rotate?

			val position = jomlMatrix.transformPosition(Vector2f())
			println("position is ${position.x}, ${position.y} and rect is ${part.sprites.rect} and rect2 is ${shape.rect}")
			graphics.drawImage(
				image, position.x.toInt() + 600, position.y.toInt() + 600,
				(image.width * scaleX).toInt(),
				(image.height * scaleY).toInt(), null
			)
		}
	}

	graphics.dispose()
	ImageIO.write(targetImage, "PNG", File("test-target.png"))
}

class PartShape(val rect: RECT, val name: String, val shapeID: Int)

class PartSprites(val rect: RECT, val shapes: List<PartShape>)

private fun parsePartSprites(partTag: DefineSpriteTag): PartSprites {
	println("offset is ${partTag.rect}")
	val shapes = mutableListOf<PartShape>()
	for (index in 2 until partTag.tags.size() - 2 step 3) {
		val frameLabel = partTag.tags[index]
		val placement = partTag.tags[index + 1]
		val show = partTag.tags[index + 2]

		if (!(frameLabel is FrameLabelTag && placement is PlaceObject2Tag && show is ShowFrameTag)) break
		val shape = partTag.swf.tags.find { it.uniqueId == placement.characterId.toString() }!!
		val rect = if (shape is DefineShape2Tag) shape.rect else if (shape is DefineShapeTag) shape.rect
		else if (shape is DefineShape3Tag) shape.rect else null
		if (rect != null) {
			shapes.add(PartShape(rect, frameLabel.labelName, placement.characterId))
		} else println("unexpected shape $shape")
	}

	return PartSprites(partTag.rect, shapes)
}

private fun parseCreature(creatureTag: DefineSpriteTag): BattleCreature {
	val parts = mutableListOf<BodyPart>()
	for (child in creatureTag.tags) {
		if (child is ShowFrameTag) break
		if (child is SoundStreamHead2Tag) continue

		val placeTag = child as PlaceObject2Tag
		val partTag = creatureTag.swf.tags.find { it.uniqueId == placeTag.characterId.toString() }!!
		if (partTag is DefineSpriteTag) {
			parts.add(BodyPart(
				depth = placeTag.depth,
				matrix = placeTag.matrix,
				sprites = parsePartSprites(partTag)
			))
			println("depth is ${placeTag.depth} and matrix is ${placeTag.matrix} and character ID is ${placeTag.characterId}")
		}
	}

	return BattleCreature(parts)
}

class BodyPart(val depth: Int, val matrix: MATRIX, val sprites: PartSprites) {
	// TODO Flags
}

class BattleCreature(val parts: List<BodyPart>) {

}
