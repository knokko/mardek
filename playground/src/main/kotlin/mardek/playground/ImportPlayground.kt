package mardek.playground

import com.jpexs.decompiler.flash.SWF
import com.jpexs.decompiler.flash.tags.*
import com.jpexs.decompiler.flash.types.MATRIX
import com.jpexs.decompiler.flash.types.RECT
import org.joml.Matrix3x2f
import org.joml.Vector2f
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_ARGB
import java.io.File
import java.lang.Integer.parseInt
import java.nio.file.Files
import javax.imageio.ImageIO

fun main() {
	// The next line will work after you copy MARDEK.swf from your steamgames to the flash directory of this repository
	// I'm reluctant to drop this file in the repository
	val input = Files.newInputStream(File("flash/MARDEK.swf").toPath())
	val swf = SWF(input, true)
	input.close()

	// deugan = 2427, emela = 2551, sslenck is 3074, monster is 3173
	val monsterTag = swf.tags.find { it.uniqueId == "3173" }!! as DefineSpriteTag
	println("frame count is ${monsterTag.frameCount}")

	val monster = parseCreature(monsterTag)

	val targetImage = BufferedImage(1024, 1024, TYPE_INT_ARGB)
	val graphics = targetImage.createGraphics()

	for (part in monster.parts) {
		//println("shape names are ${part.sprites.shapes.map { it.name }}")
		val shapes = part.sprites.shapes.firstOrNull()
		if (shapes != null) {
			for (shape in shapes.shapes) {
				val image = ImageIO.read(File("flash/shapes/${shape.id}.png"))
				val jomlMatrix = Matrix3x2f()
				val (scaleX, scaleY) = if (part.matrix.hasScale) Pair(part.matrix.scaleX, part.matrix.scaleY) else Pair(
					1f,
					1f
				)

				jomlMatrix.translate(
					(part.matrix.translateX + scaleX * shape.rect.Xmin) / 20f,
					(part.matrix.translateY + scaleY * shape.rect.Ymin) / 20f
				)
				// TODO Rotate?

				val position = jomlMatrix.transformPosition(Vector2f())
				//println("position is ${position.x}, ${position.y} and rect is ${part.sprites.rect} and rect2 is ${shape.rect}")
				graphics.drawImage(
					image, position.x.toInt() + 600, position.y.toInt() + 600,
					(image.width * scaleX).toInt(),
					(image.height * scaleY).toInt(), null
				)
			}
		} else println("no shapes?")
	}

	graphics.dispose()
	ImageIO.write(targetImage, "PNG", File("test-target.png"))
}

class SingleShape(val rect: RECT, val id: Int)

class PartShape(val name: String, val shapes: List<SingleShape>)

class PartSprites(val shapes: List<PartShape>)

private fun parsePartSprites(partTag: DefineSpriteTag): PartSprites {
	val shapes = mutableListOf<PartShape>()

	var index = 0
	var frameLabel: FrameLabelTag? = null
	val singleShapes = mutableListOf<SingleShape>()
	while (index < partTag.tags.size()) {
		if (partTag.tags[index] is FrameLabelTag) frameLabel = partTag.tags[index] as FrameLabelTag?
		if (partTag.tags[index] is PlaceObject2Tag) {
			val placement = partTag.tags[index] as PlaceObject2Tag
			parseShape(partTag.swf, placement.characterId, singleShapes)
		}
		if (partTag.tags[index] is ShowFrameTag) {
			if (singleShapes.isNotEmpty()) {
				shapes.add(PartShape(frameLabel?.labelName ?: "unknown", singleShapes.toList()))
				frameLabel = null
				singleShapes.clear()
			}
		}
		index += 1
	}

	if (shapes.isEmpty()) println("nothing for $partTag")
	return PartSprites(shapes)
}

private fun parseShape(swf: SWF, id: Int, outShapes: MutableList<SingleShape>) {
	val shape = swf.tags.find { it.uniqueId == id.toString() }!!
	val rect = if (shape is DefineShape2Tag) shape.rect else if (shape is DefineShapeTag) shape.rect
	else if (shape is DefineShape3Tag) shape.rect else if (shape is DefineShape4Tag) shape.rect else null
	if (rect != null) {
		outShapes.add(SingleShape(rect, id))
	} else println("unexpected shape $shape") // check out 2281
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
		} else if (partTag is DefineShapeTag || partTag is DefineShape3Tag) {
			val singleShapes = mutableListOf<SingleShape>()
			parseShape(placeTag.swf, parseInt(partTag.uniqueId), singleShapes)
			parts.add(
				BodyPart(
				depth = placeTag.depth,
				matrix = placeTag.matrix,
				sprites = PartSprites(listOf(PartShape("ehm", singleShapes)))
			))
		} else println("no DefineSpriteTag? ${partTag::class.java} $partTag")
	}

	return BattleCreature(parts)
}

class BodyPart(val depth: Int, val matrix: MATRIX, val sprites: PartSprites) {
	// TODO Flags
}

class BattleCreature(val parts: List<BodyPart>) {

}
