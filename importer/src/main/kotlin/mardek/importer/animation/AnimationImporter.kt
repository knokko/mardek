package mardek.importer.animation

import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.jpexs.decompiler.flash.helpers.CodeFormatting
import com.jpexs.decompiler.flash.helpers.StringBuilderTextWriter
import com.jpexs.decompiler.flash.tags.DefineSpriteTag
import com.jpexs.decompiler.flash.tags.DoActionTag
import com.jpexs.decompiler.flash.tags.ShowFrameTag
import com.jpexs.decompiler.flash.tags.base.ASMSource
import com.jpexs.decompiler.flash.tags.base.MorphShapeTag
import com.jpexs.decompiler.flash.tags.base.PlaceObjectTypeTag
import com.jpexs.decompiler.flash.tags.base.ShapeTag
import mardek.content.animation.AnimationFrames
import mardek.content.animation.AnimationNode
import mardek.content.animation.AnimationSprite
import mardek.content.animation.AnimationFrame
import mardek.content.animation.AnimationMask
import mardek.content.animation.AnimationMaskFrame
import mardek.content.animation.AnimationMatrix
import mardek.content.animation.ColorTransform
import mardek.content.animation.SkinnedAnimation
import mardek.content.animation.SpecialAnimationNode
import mardek.content.sprite.BcSprite
import mardek.importer.area.FLASH
import mardek.importer.particle.FLASH_FRAMES_PER_SECOND
import java.io.File
import java.io.IOException
import java.lang.Integer.parseInt
import java.util.Locale
import javax.imageio.ImageIO
import kotlin.time.Duration.Companion.seconds

internal fun getScript(source: ASMSource): String {
	val stringBuilder = StringBuilder()
	val writer = StringBuilderTextWriter(CodeFormatting(), stringBuilder)
	source.getActionScriptSource(writer, null)
	return stringBuilder.toString()
}

private fun extractEquipmentSpecial(animation: SkinnedAnimation): SpecialAnimationNode? {
	for (childFrames in animation.skins.values) {
		for (frame in childFrames.frames) {
			for (node in frame.nodes) {
				if (node.animation == null) {
					if (node.special == SpecialAnimationNode.Weapon || node.special == SpecialAnimationNode.Shield) {
						return node.special
					}
				}
			}
		}
	}

	return null
}

internal fun importAnimationNode(
	tag: PlaceObjectTypeTag, childID: Int, instanceName: String?,
	initialAnimationColor: ColorTransform?, mask: AnimationMask,
	initialSpecial: SpecialAnimationNode?, context: AnimationImportContext
): AnimationNode {
	val animationMatrix = convertTransformationMatrix(tag.matrix)

	var animation: SkinnedAnimation? = null
	var sprite: AnimationSprite? = null
	var animationColor: ColorTransform? = initialAnimationColor
	var special: SpecialAnimationNode? = initialSpecial

	if (instanceName == "HitPoint") special = SpecialAnimationNode.HitPoint
	if (instanceName == "StrikePoint") special = SpecialAnimationNode.StrikePoint
	if (childID == 2304) special = SpecialAnimationNode.ElementalSwing
	if (childID == 219) special = SpecialAnimationNode.ElementalCastingCircle
	if (childID == 2232) special = SpecialAnimationNode.ElementalCastingBackground
	if (instanceName == "statusFX") special = SpecialAnimationNode.StatusEffectPoint
	// TODO CHAP2 What's the difference between those two?
	if (instanceName == "StfxPoint") special = SpecialAnimationNode.StatusEffectPoint
	if (instanceName == "core") special = SpecialAnimationNode.Core
	if (childID == 2311) special = SpecialAnimationNode.Exclaim

	val exportName = FLASH.getExportName(tag.characterId)
	if (exportName == "castSparkle" || childID == 494) special = SpecialAnimationNode.ElementalCastingSparkle

	if (instanceName == "target_cursor") special = SpecialAnimationNode.TargetingCursor
	if (instanceName == "act_cursor") special = SpecialAnimationNode.OnTurnCursor

	if (instanceName == "face") special = SpecialAnimationNode.PortraitFace
	if (instanceName == "eye1" || instanceName == "eye2") special = SpecialAnimationNode.PortraitEye
	if (instanceName == "eyebrow1" || instanceName == "eyebrow2") special = SpecialAnimationNode.PortraitEyeBrow
	if (instanceName == "hair") special = SpecialAnimationNode.PortraitHair
	if (instanceName == "torso" || instanceName == "head" || instanceName == "mouth") {
		special = SpecialAnimationNode.PortraitExpressions
	}
	if (childID == 947) special = SpecialAnimationNode.PortraitArmor
	if (childID == 997 && initialSpecial != SpecialAnimationNode.PortraitMouth) {
		special = SpecialAnimationNode.PortraitEthnicity
	}
	if (childID == 1059 || childID == 1349) special = SpecialAnimationNode.PortraitMouth
	if (childID == 1569) special = SpecialAnimationNode.PortraitRobe
	if (childID == 1650 || childID == 1725) special = SpecialAnimationNode.PortraitExpressions

	val skipSpecial = special != null && special.skipChildren

	if (!skipSpecial && childID != 2222) {
		val childTag = FLASH.getCharacter(childID)
		if (childTag is DefineSpriteTag) {
			animation = importSkinnedAnimation(childTag, context)
			if (special == null) special = extractEquipmentSpecial(animation)
		}
		if (childTag is ShapeTag) sprite = importAnimationSprite(childTag, false, context)
	}

	var selectSkin: String? = null
	if (exportName == "torchlist" || childID == 516) {
		selectSkin = "orange"
		animationColor = ColorTransform(
			addColor = 0,
			multiplyColor = rgba(1f, 1f, 1f, 0.35f),
			subtractColor = 0,
		)
	}

	if (tag.clipActions != null) {
		for (clip in tag.clipActions.clipActionRecords) {
			if (instanceName == "mdl") {
				val script = getScript(clip)
				val prefix = "skin = "
				val startIndex = script.indexOf(prefix)
				val endIndex = script.indexOf(';', startIndex + prefix.length)
				if (startIndex != -1 && endIndex != -1) {
					selectSkin = script.substring(startIndex + prefix.length, endIndex)
					if (selectSkin.startsWith('"')) selectSkin = selectSkin.substring(1, selectSkin.length - 1)
				} else {
					println("Skipping skin selection of $tag with script $script")
				}
			}
			if (exportName == "torchlight" || childID == 516) {
				val script = getScript(clip)
				val prefix = "gotoAndStop("
				val startIndex = script.indexOf(prefix)
				val endIndex = script.indexOf(')', startIndex + prefix.length)
				if (startIndex != -1 && endIndex != -1) {
					selectSkin = script.substring(startIndex + prefix.length, endIndex)
					if (selectSkin.startsWith('"')) selectSkin = selectSkin.substring(1, selectSkin.length - 1)
					if (selectSkin == "1") selectSkin = "orange"
				}
			}
		}
	}

	if (selectSkin != null) selectSkin = selectSkin.lowercase(Locale.ROOT)
	return AnimationNode(
		depth = tag.depth,
		animation = animation,
		sprite = sprite,
		matrix = animationMatrix,
		color = animationColor,
		special = special,
		selectSkin = selectSkin,
		mask = mask,
	)
}

internal fun importSkinnedAnimation(tag: DefineSpriteTag, context: AnimationImportContext): SkinnedAnimation {
	val tagID = parseInt(tag.uniqueId)
	val existing = context.spriteMapping[tagID]
	if (existing != null) return existing

	val rawSkins = simulateAnimations(tag)
	val skins = HashMap<String, AnimationFrames>()

	val skinsMap = mutableMapOf<String, MutableList<DoActionTag>>()
	context.scriptMapping[tagID] = skinsMap

	for ((name, animation) in rawSkins) {
		val frames = animation.frames.map { frame ->
			var initialSpecial: SpecialAnimationNode? = null
			val nodes = mutableListOf<AnimationNode>()
			for (script in frame.scripts) {
				skinsMap.computeIfAbsent(name.lowercase(Locale.ROOT)) { mutableListOf() }.add(script)

				val content = getScript(script)
				if (content.contains("stats.shield")) initialSpecial = SpecialAnimationNode.Shield
				if (content.contains("this.gotoAndStop(_parent._parent.stats.weapon")) {
					initialSpecial = SpecialAnimationNode.Weapon
				}
			}

			for ((depth, node) in frame.nodes.withIndex()) {
				if (node == null || node.clipDepth >= 0) continue

				var rawMask: RawAnimationNode? = null
				for ((maskDepth, candidateMask) in frame.nodes.withIndex()) {
					if (maskDepth <= depth && candidateMask != null && candidateMask.clipDepth >= depth) {
						if (rawMask != null) throw UnsupportedOperationException(
							"Conflicting masks in $tag at depth $depth"
						)
						rawMask = candidateMask
					}
				}

				val mask = if (rawMask != null) {
					val childTag = FLASH.tags.find { it.uniqueId == rawMask.childID.toString() }
					val matrix = convertTransformationMatrix(rawMask.tag.matrix) ?: AnimationMatrix.DEFAULT
					if (childTag is ShapeTag) {
						arrayOf(AnimationMaskFrame(importAnimationSprite(
							childTag, true, context
						), matrix, 1.seconds / FLASH_FRAMES_PER_SECOND))
					} else if (childTag is MorphShapeTag) {
						println("Ignoring morph mask $childTag")
						emptyArray()
					} else if (childTag is DefineSpriteTag) {
						val maskList = mutableListOf<AnimationMaskFrame>()
						var nextFrame: AnimationMaskFrame? = null
						for (maskTag in childTag.tags) {
							if (maskTag is PlaceObjectTypeTag) {
								val childMaskID = maskTag.characterId
								val childMask = FLASH.tags.find { it.uniqueId == childMaskID.toString() } as ShapeTag
								val childMaskSprite = importAnimationSprite(childMask, true, context)
								val childMaskMatrix = convertTransformationMatrix(maskTag.matrix)
								if (childMaskMatrix != null && childMaskMatrix != AnimationMatrix.DEFAULT) {
									throw UnsupportedOperationException("Unexpected complex mask $childTag")
								}
								nextFrame = AnimationMaskFrame(
									childMaskSprite, matrix,
									1.seconds / FLASH_FRAMES_PER_SECOND
								)
							}
							if (maskTag is ShowFrameTag) {
								maskList.add(nextFrame!!)
							}
						}
						maskList.toTypedArray()
					} else throw UnsupportedOperationException("Unexpected mask $childTag")
				} else emptyArray()

				var animationColor = convertColorTransform(node.colors)
				if (animationColor == ColorTransform.DEFAULT) animationColor = null
				nodes.add(importAnimationNode(
					node.tag, node.childID, node.instanceName, animationColor,
					AnimationMask(mask), initialSpecial, context,
				))
			}
			AnimationFrame(duration = 1.seconds / FLASH_FRAMES_PER_SECOND, nodes = nodes.toTypedArray())
		}
		if (frames.isNotEmpty()) skins[name.lowercase(Locale.ROOT)] = AnimationFrames(frames.toTypedArray())
	}

	val result = SkinnedAnimation(
		defineSpriteFlashID = tagID,
		skins = skins,
	)
	context.spriteMapping[tagID] = result
	return result
}

private fun importAnimationSprite(tag: ShapeTag, isMask: Boolean, context: AnimationImportContext): AnimationSprite {
	val shapeID = parseInt(tag.uniqueId)
	val existing = context.shapeMapping[Pair(shapeID, isMask)]
	if (existing != null) return existing

	val expectedFile = File("${context.shapesDirectory}/${tag.uniqueId}.png")
	val sprite: BcSprite
	try {
		val image = ImageIO.read(expectedFile)
		sprite = BcSprite(image.width, image.height, if (isMask) 4 else 7)
		sprite.bufferedImage = image
	} catch (failed: IOException) {
		println(expectedFile.absoluteFile)
		println(expectedFile.exists())
		throw RuntimeException("Failed to load shape from $expectedFile", failed)
	}

	val result = AnimationSprite(
		defineShapeFlashID = shapeID,
		image = sprite,
		offsetX = tag.rect.Xmin / 20f,
		offsetY = tag.rect.Ymin / 20f,
	)
	context.shapeMapping[Pair(shapeID, isMask)] = result
	return result
}

internal fun findDependencies(parentNodes: List<AnimationNode>): Pair<Array<AnimationSprite>, Array<SkinnedAnimation>> {
	val usedSprites = mutableMapOf<Pair<Int, Boolean>, AnimationSprite>()
	val usedAnimations = mutableMapOf<Int, SkinnedAnimation>()

	var nodesToProcess = parentNodes
	val nextNodes = mutableListOf<AnimationNode>()

	while (nodesToProcess.isNotEmpty()) {
		for (node in nodesToProcess) {
			val animation = node.animation
			if (animation != null && !usedAnimations.containsKey(animation.defineSpriteFlashID)) {
				usedAnimations[animation.defineSpriteFlashID] = animation
				for (frames in animation.skins.values) {
					for (frame in frames) {
						for (childNode in frame.nodes) nextNodes.add(childNode)
					}
				}
			}

			val sprite = node.sprite
			if (sprite != null && !usedSprites.containsKey(Pair(sprite.defineShapeFlashID, false))) {
				usedSprites[Pair(sprite.defineShapeFlashID, false)] = sprite
			}

			for (frame in node.mask) {
				if (!usedSprites.containsKey(Pair(frame.sprite.defineShapeFlashID, true))) {
					usedSprites[Pair(frame.sprite.defineShapeFlashID, true)] = frame.sprite
				}
			}
		}

		nodesToProcess = nextNodes.toList()
		nextNodes.clear()
	}

	return Pair(usedSprites.values.toTypedArray(), usedAnimations.values.toTypedArray())
}
