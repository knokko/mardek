package mardek.importer.battle

import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.jpexs.decompiler.flash.SWF
import com.jpexs.decompiler.flash.helpers.CodeFormatting
import com.jpexs.decompiler.flash.helpers.StringBuilderTextWriter
import com.jpexs.decompiler.flash.tags.*
import com.jpexs.decompiler.flash.types.ColorTransform
import com.jpexs.decompiler.flash.types.MATRIX
import com.jpexs.decompiler.flash.types.RECT
import mardek.assets.animations.*
import mardek.assets.battle.BattleAssets
import mardek.assets.battle.Monster
import mardek.assets.sprite.BcSprite
import mardek.importer.area.FLASH
import mardek.importer.util.resourcesFolder
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.lang.Integer.parseInt
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private fun importSkeleton(swf: SWF, id: Int, spriteIdMapping: MutableMap<Int, BcSprite>): Skeleton {
	val monsterTag = swf.tags.find { it.uniqueId == id.toString() }!! as DefineSpriteTag
	val monster = parseCreature2(monsterTag)
	return convertFlashCreature(monster, spriteIdMapping)
}

internal fun importMonsters(assets: BattleAssets, playerModelMapping: MutableMap<String, BattleModel>) {
	val battleTag = FLASH.tags.find { it.uniqueId == "5118" }!! as DefineSpriteTag
	println("battle tag is $battleTag")

	val labelBlacklist = arrayOf("null", "humans!", "dummy", "Monsters", "Bosses", "Ether Clone")
	val idBlacklist = arrayOf(-1, 0, 1885, 2233, 2294, 2311)

	class Monster(val label: String) {
		val placements = HashSet<Int>()
		val skins = HashSet<String>()
		val intSkins = HashSet<Int>()
		val exportedScripts = HashSet<String>()

		var finished = false

		override fun toString() = label
	}

	val monsters = mutableListOf<Monster>()
	var lastDoAction = ""

	fun extractSkin(monster: Monster, getSource: (StringBuilderTextWriter) -> Unit) {
		val builder = StringBuilder()
		val writer = StringBuilderTextWriter(CodeFormatting(), builder)
		getSource(writer)

		val source = builder.toString()
		if (source.contains("aeropolis_W_theatre")) {
			monster.skins.add("d")
			return
		}

		val prefix = "skin = "
		val index = source.indexOf(prefix)
		if (index == -1) {

			throw RuntimeException("monster is ${monster.label} and source is $source")
		}
		val endIndex = source.indexOf(';', index + prefix.length)
		if (endIndex == -1) throw RuntimeException("source is $source")

		val skinString = source.substring(index + prefix.length, endIndex)
		if (skinString.startsWith("\"")) monster.skins.add(skinString.substring(1, skinString.length - 1))
		else monster.intSkins.add(parseInt(skinString))
	}

	for (tag in battleTag.tags) {
		val monster = monsters.lastOrNull()
		if (monster != null && !monster.finished) {
			if (tag is PlaceObject2Tag && !idBlacklist.contains(tag.characterId)) {
				monster.placements.add(tag.characterId)
				for (child in tag.subItems) {
					extractSkin(monster) { child.getActionScriptSource(it, null) }
				}
			}
			if (tag is PlaceObject3Tag && !idBlacklist.contains(tag.characterId)) {
				monster.placements.add(tag.characterId)
				for (child in tag.subItems) {
					extractSkin(monster) { child.getActionScriptSource(it, null) }
				}
			}
		}
		if (tag is DoActionTag) {
			val builder = StringBuilder()
			val writer = StringBuilderTextWriter(CodeFormatting(), builder)
			tag.getActionScriptSource(writer, null)
			lastDoAction = builder.toString()
		}
		if (tag is FrameLabelTag) {
			val newMonster = Monster(tag.labelName)
			monsters.add(newMonster)
			if (lastDoAction.isNotEmpty()) newMonster.exportedScripts.add(lastDoAction)
			lastDoAction = ""
		}
		if (tag is ShowFrameTag && monster != null) monster.finished = true
	}

	monsters.removeIf { labelBlacklist.contains(it.label) }

	val skeletons = mutableMapOf<Int, Skeleton>()
	val spriteIdMapping = mutableMapOf<Int, BcSprite>()

	for ((index, monster) in monsters.withIndex()) {
		if (monster.placements.isEmpty() && index > 0) monster.placements.addAll(monsters[index - 1].placements)
		if (monster.placements.size != 1) throw RuntimeException("Monster ${monster.label} has ${monster.placements.size}")
		if (monster.exportedScripts.size > 1) {
			throw RuntimeException("Monster ${monster.label} has multiple scripts")
		}

		val skeleton = skeletons.computeIfAbsent(monster.placements.iterator().next()) { id -> importSkeleton(FLASH, id, spriteIdMapping) }

		if (monster.skins.size + monster.intSkins.size > 1) {
			throw RuntimeException("Monster ${monster.label} has multiple skins")
		}

		val skin = if (monster.skins.isNotEmpty()) monster.skins.iterator().next()
				else if (monster.intSkins.isNotEmpty()) "int-${monster.intSkins.iterator().next()}" else null

		if (monster.exportedScripts.isEmpty()) {
			playerModelMapping[monster.label] = BattleModel(skeleton, skin)
		} else assets.monsters.add(Monster(name = monster.label, model = BattleModel(skeleton, skin)))
	}

	for (skeleton in skeletons.values) assets.skeletons.add(skeleton)
}

private fun convertFlashCreature(creature: BattleCreature2, spriteIdMapping: MutableMap<Int, BcSprite>): Skeleton {
	val partMapping = mutableMapOf<Int, SkeletonPart>()
	val exportScale = 4
	val compressedShapesDirectory = File("$resourcesFolder/bc7shapes-x$exportScale")
	
	val parts = creature.bodyParts.map { rawBodyPart ->
		val skins = rawBodyPart.variations.map { BodyPart(name = it.name, entries = it.entries.map { rawEntry ->
			var sprite = spriteIdMapping[rawEntry.id]
			val bc7File = File("$compressedShapesDirectory/${rawEntry.id}.bc7")
			if (sprite == null) {
				if (bc7File.exists()) {
					val input = DataInputStream(Files.newInputStream(bc7File.toPath()))
					val width = input.readInt()
					val height = input.readInt()

					sprite = BcSprite(width, height, 7)
					sprite.data = input.readAllBytes()
					input.close()
				} else {
					val image = ImageIO.read(File("flash/monster-shapes-x$exportScale/${rawEntry.id}.png"))

					sprite = BcSprite(image.width, image.height, 7)
					sprite.bufferedImage = image
					sprite.postEncodeCallback = {
						compressedShapesDirectory.mkdirs()
						val output = DataOutputStream(Files.newOutputStream(bc7File.toPath()))
						output.writeInt(sprite.width)
						output.writeInt(sprite.height)
						output.write(sprite.data!!)
						output.flush()
						output.close()
					}
				}
				spriteIdMapping[rawEntry.id] = sprite
			}

			BodyPartEntry(sprite = sprite, offsetX = rawEntry.rect.Xmin / 20f, offsetY = rawEntry.rect.Ymin / 20f, scale = exportScale)
		}.toTypedArray()) }.toTypedArray()
		val skeletonPart = SkeletonPart(skins = skins)
		partMapping[rawBodyPart.id] = skeletonPart
		skeletonPart
	}
	val animations = creature.animations.mapValues { rawAnimation -> Animation(frames = rawAnimation.value.map { frame ->
		AnimationFrame(parts = frame.parts.filter { part -> part.matrix != null }.map { part ->
			val matrix = part.matrix!!
			val rawColor = part.color

			fun transform(value: Int) = (255.0 * (value / 256.0)).roundToInt()
			fun pack(r: Int, g: Int, b: Int, a: Int) = rgba(transform(r), transform(g), transform(b), transform(a))

			val color = if (rawColor != null) ColorTransform(
				addColor = pack(rawColor.redAdd, rawColor.greenAdd, rawColor.blueAdd, rawColor.alphaAdd),
				multiplyColor = pack(rawColor.redMulti, rawColor.greenMulti, rawColor.blueMulti, rawColor.alphaMulti)
			) else null
			AnimationPart(
				part = partMapping[part.part.id]!!,
				matrix = AnimationMatrix(
					translateX = matrix.translateX / 20f,
					translateY = matrix.translateY / 20f,
					rotateSkew0 = matrix.rotateSkew0,
					rotateSkew1 = matrix.rotateSkew1,
					hasScale = matrix.hasScale,
					scaleX = matrix.scaleX,
					scaleY = matrix.scaleY
				),
				color = color
			)
		}.toTypedArray())
	}.toTypedArray()) }
	return Skeleton(animations = HashMap(animations), parts = parts.toTypedArray())
}

private class FlashShapeEntry(val rect: RECT, val id: Int)

private class FlashShapeVariation(val name: String, val entries: List<FlashShapeEntry>)

private class BodyPart2(val id: Int, val variations: List<FlashShapeVariation>)

private class BattleCreature2(val bodyParts: Set<BodyPart2>, val minDepth: Int, val maxDepth: Int) {
	lateinit var baseState: AnimationState

	val animations = mutableMapOf<String, List<AnimationState>>()
}

private class AnimationPartState {
	lateinit var part: BodyPart2
	var matrix: MATRIX? = null
	var color: ColorTransform? = null

	override fun toString() = "AnimationPS($matrix)"
}

private class AnimationState(private val creature: BattleCreature2) {
	val parts = Array(1 + creature.maxDepth - creature.minDepth) { AnimationPartState() }

	fun update(tag: Tag): Boolean {
		if (tag is PlaceObject2Tag) {
			val part = parts[tag.depth - creature.minDepth]
			if (tag.characterId != 0) part.part = creature.bodyParts.find { it.id == tag.characterId }!!
			part.matrix = tag.matrix
			if (tag.placeFlagHasColorTransform) part.color = tag.colorTransform
			return true
		} else if (tag is RemoveObject2Tag) {
			parts[tag.depth - creature.minDepth].matrix = null
			parts[tag.depth - creature.minDepth].color = null
			return true
		}

		return false
	}

	fun copy(): AnimationState {
		val copied = AnimationState(creature)
		for ((index, part) in parts.withIndex()) {
			if (part.matrix != null) {
				copied.parts[index].part = part.part
				copied.parts[index].matrix = part.matrix
				copied.parts[index].color = part.color
			}
		}
		return copied
	}

	override fun toString() = parts.contentToString()
}

private fun parsePartSprites(partTag: DefineSpriteTag): List<FlashShapeVariation> {
	val shapes = mutableListOf<FlashShapeVariation>()

	var index = 0
	var frameLabel: FrameLabelTag? = null
	val singleShapes = mutableListOf<FlashShapeEntry>()
	while (index < partTag.tags.size()) {
		if (partTag.tags[index] is FrameLabelTag) frameLabel = partTag.tags[index] as FrameLabelTag?
		if (partTag.tags[index] is PlaceObject2Tag) {
			val placement = partTag.tags[index] as PlaceObject2Tag
			parseShape(partTag.swf, placement.characterId, singleShapes)
		}
		if (partTag.tags[index] is ShowFrameTag) {
			if (singleShapes.isNotEmpty()) {
				shapes.add(FlashShapeVariation(frameLabel?.labelName ?: "unknown", singleShapes.toList()))
				frameLabel = null
				singleShapes.clear()
			}
		}
		index += 1
	}

	if (shapes.isEmpty() && partTag.spriteId != 291) println("nothing for $partTag")
	return shapes
}

private fun parseShape(swf: SWF, id: Int, outShapes: MutableList<FlashShapeEntry>) {
	if (id == 0) return
	val shape = swf.tags.find { it.uniqueId == id.toString() } ?: throw RuntimeException("Can't find shape with ID $id")
	val rect = if (shape is DefineShape2Tag) shape.rect else if (shape is DefineShapeTag) shape.rect
	else if (shape is DefineShape3Tag) shape.rect else if (shape is DefineShape4Tag) shape.rect else null
	if (rect != null) {
		outShapes.add(FlashShapeEntry(rect, id))
	} else {
		if (id == 2281) return // TODO Handle glyph rune shield
		if (id == 1717) return // TODO Handle drill-o-matic
		println("unexpected shape $shape")
	}
}

private fun parseVariations(tag: Tag): List<FlashShapeVariation> {
	return if (tag is DefineSpriteTag) {
		if (tag.spriteId == 494) return emptyList() // TODO Support castSparkle
		parsePartSprites(tag)
	} else if (tag is DefineShapeTag || tag is DefineShape3Tag) {
		val singleShapes = mutableListOf<FlashShapeEntry>()
		parseShape(tag.swf, parseInt(tag.uniqueId), singleShapes)
		listOf(FlashShapeVariation("D", singleShapes))
	} else if (tag is DefineMorphShapeTag || tag is DefineMorphShape2Tag) {
		// TODO Do something?
		emptyList()
	} else {
		println("no DefineSpriteTag? ${tag::class.java} $tag")
		emptyList()
	}
}

private fun parseCreature2(creatureTag: DefineSpriteTag): BattleCreature2 {
	var minDepth = 1000
	var maxDepth = 0
	val bodyParts = mutableSetOf<BodyPart2>()
	for (child in creatureTag.tags) {
		if (child is PlaceObject2Tag) {
			minDepth = min(minDepth, child.depth)
			maxDepth = max(maxDepth, child.depth)
			if (child.characterId != 0) bodyParts.add(BodyPart2(child.characterId, parseVariations(
				creatureTag.swf.tags.find { it.uniqueId == child.characterId.toString() }!!
			)))
		}
	}

	val creature = BattleCreature2(bodyParts, minDepth, maxDepth)

	val animationState = AnimationState(creature)
	for (child in creatureTag.tags) {
		if (child is FrameLabelTag) break
		animationState.update(child)
	}

	creature.baseState = animationState.copy()

	var currentLabel: String? = null
	val currentFrames = mutableListOf<AnimationState>()
	var hasChangedAnimation = false
	for (child in creatureTag.tags) {
		if (child is FrameLabelTag) {
			if (currentLabel != null) creature.animations[currentLabel] = currentFrames.toList()
			currentFrames.clear()
			currentLabel = child.labelName
		}

		if (child is ShowFrameTag && hasChangedAnimation) {
			hasChangedAnimation = false
			currentFrames.add(animationState.copy())
		}

		if (currentLabel != null) {
			hasChangedAnimation = animationState.update(child) || hasChangedAnimation
		}
	}

	return creature
}
