package mardek.playground

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.buffers.MappedVkbBuffer
import com.github.knokko.boiler.builders.BoilerBuilder
import com.github.knokko.boiler.builders.WindowBuilder
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.descriptors.HomogeneousDescriptorPool
import com.github.knokko.boiler.descriptors.VkbDescriptorSetLayout
import com.github.knokko.boiler.images.VkbImage
import com.github.knokko.boiler.pipelines.GraphicsPipelineBuilder
import com.github.knokko.boiler.synchronization.ResourceUsage
import com.github.knokko.boiler.window.AcquiredImage
import com.github.knokko.boiler.window.SimpleWindowRenderLoop
import com.github.knokko.boiler.window.VkbWindow
import com.github.knokko.boiler.window.WindowEventLoop
import com.jpexs.decompiler.flash.SWF
import com.jpexs.decompiler.flash.tags.*
import com.jpexs.decompiler.flash.types.ColorTransform
import com.jpexs.decompiler.flash.types.MATRIX
import com.jpexs.decompiler.flash.types.RECT
import org.joml.Matrix3x2f
import org.joml.Vector2f
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.memByteBuffer
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_FIFO_KHR
import org.lwjgl.vulkan.VK12.*
import java.io.File
import java.lang.Integer.parseInt
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

fun main() {
	// The next line will work after you copy MARDEK.swf from your steamgames to the flash directory of this repository
	// I'm reluctant to drop this file in the repository
	val input = Files.newInputStream(File("flash/MARDEK.swf").toPath())
	val swf = SWF(input, true)
	input.close()

	// deugan = 2427, emela = 2551, sslenck is 3074, monster is 3173
	val monsterTag = swf.tags.find { it.uniqueId == "3173" }!! as DefineSpriteTag
	println("frame count is ${monsterTag.frameCount}")

	val monster = parseCreature2(monsterTag)

	val boiler = BoilerBuilder(
		VK_API_VERSION_1_2, "ImportPlayground", 1
	)
		.validation()
		.enableDynamicRendering()
		.requiredFeatures12 { it.shaderSampledImageArrayNonUniformIndexing() }
		.featurePicker12 { _, _, toEnable -> toEnable.shaderSampledImageArrayNonUniformIndexing(true) }
		.addWindow(WindowBuilder(800, 600, VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT))
		.build()

	val eventLoop = WindowEventLoop()
	eventLoop.addWindow(CreatureRenderer(boiler.window(), monster))
	eventLoop.runMain()

	boiler.destroyInitialObjects()
}

class FlashShapeEntry(val rect: RECT, val id: Int)

class FlashShapeVariation(val name: String, val entries: List<FlashShapeEntry>)

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

	if (shapes.isEmpty()) println("nothing for $partTag")
	return shapes
}

private fun parseShape(swf: SWF, id: Int, outShapes: MutableList<FlashShapeEntry>) {
	val shape = swf.tags.find { it.uniqueId == id.toString() }!!
	val rect = if (shape is DefineShape2Tag) shape.rect else if (shape is DefineShapeTag) shape.rect
	else if (shape is DefineShape3Tag) shape.rect else if (shape is DefineShape4Tag) shape.rect else null
	if (rect != null) {
		outShapes.add(FlashShapeEntry(rect, id))
	} else println("unexpected shape $shape") // check out 2281
}

private fun parseVariations(tag: Tag): List<FlashShapeVariation> {
	return if (tag is DefineSpriteTag) {
		parsePartSprites(tag)
	} else if (tag is DefineShapeTag || tag is DefineShape3Tag) {
		val singleShapes = mutableListOf<FlashShapeEntry>()
		parseShape(tag.swf, parseInt(tag.uniqueId), singleShapes)
		listOf(FlashShapeVariation("ehm", singleShapes))
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

class BodyPart2(val id: Int, val variations: List<FlashShapeVariation>)

class BattleCreature2(val bodyParts: Set<BodyPart2>, val minDepth: Int, val maxDepth: Int) {
	lateinit var baseState: AnimationState

	val animations = mutableMapOf<String, List<AnimationState>>()
}

class AnimationPartState {
	lateinit var part: BodyPart2
	var matrix: MATRIX? = null
	var color: ColorTransform? = null

	override fun toString() = "AnimationPS($matrix)"
}
class AnimationState(private val creature: BattleCreature2) {
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
				copied.parts[index].matrix = part.matrix // TODO Convert to JOML matrix?
				copied.parts[index].color = part.color
			}
		}
		return copied
	}

	override fun toString() = parts.contentToString()
}

class CreatureRenderer(window: VkbWindow, val monster: BattleCreature2) : SimpleWindowRenderLoop(
	window, 1, true, VK_PRESENT_MODE_FIFO_KHR, // TODO Use frames-in-flight for vertex positions
	ResourceUsage.COLOR_ATTACHMENT_WRITE, ResourceUsage.COLOR_ATTACHMENT_WRITE
) {

	private lateinit var descriptorSetLayout: VkbDescriptorSetLayout
	private lateinit var descriptorPool: HomogeneousDescriptorPool
	private lateinit var images: List<VkbImage>
	private lateinit var shapeEntries: List<Pair<BodyPart2, FlashShapeEntry>>
	private lateinit var vertexBuffer: MappedVkbBuffer
	private lateinit var colorBuffer: MappedVkbBuffer

	private var descriptorSet = 0L

	private var pipelineLayout = 0L
	private var graphicsPipeline = 0L
	private var sampler = 0L

	private val testAnimationState = monster.animations["idle"]!![0]
	//private val testAnimationState = monster.baseState
	private val preferredVariation = "punk"

	override fun setup(boiler: BoilerInstance, stack: MemoryStack) {
		super.setup(boiler, stack)

		val selectedShapes = mutableListOf<Pair<BodyPart2, FlashShapeEntry>>()
		for (bodyPart in testAnimationState.parts) {
			if (bodyPart.matrix == null) continue
			val variation = bodyPart.part.variations.find { it.name == preferredVariation } ?: bodyPart.part.variations[0]
			for (entry in variation.entries) selectedShapes.add(Pair(bodyPart.part, entry))
		}
//		for (bodyPart in monster.bodyParts) {
//			if (bodyPart.variations.isEmpty()) continue
//			val variation = bodyPart.variations.find { it.name == preferredVariation } ?: bodyPart.variations[0]
//			for (entry in variation.entries) selectedShapes.add(Pair(bodyPart, entry))
//		}

		val bufferedImages = selectedShapes.map { ImageIO.read(File("flash/shapes/${it.second.id}.png")) }
		this.shapeEntries = selectedShapes.toList()
		var numPixels = 0
		for (image in bufferedImages) numPixels += image.width * image.height
		this.images = bufferedImages.map { boiler.images.createSimple(
			it.width, it.height, VK_FORMAT_R8G8B8A8_SRGB,
			VK_IMAGE_USAGE_TRANSFER_DST_BIT or VK_IMAGE_USAGE_SAMPLED_BIT,
			VK_IMAGE_ASPECT_COLOR_BIT, "Shape(${it.width},${it.height})"
		) }

		val stagingBuffer = boiler.buffers.createMapped(4L * numPixels, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, "StagingBuffer")
		val stagingPool = boiler.commands.createPool(0, boiler.queueFamilies().graphics.index, "StagingPool")
		val stagingCommandBuffer = boiler.commands.createPrimaryBuffers(stagingPool, 1, "StagingCommandBuffers")[0]
		val recorder = CommandRecorder.begin(stagingCommandBuffer, boiler, stack, "StagingCopy")

		var stagingOffset = 0L
		for ((index, bufferedImage) in bufferedImages.withIndex()) {
			val image = this.images[index]
			boiler.buffers.encodeBufferedImageRGBA(stagingBuffer, bufferedImage, stagingOffset)
			recorder.transitionLayout(image, null, ResourceUsage.TRANSFER_DEST)
			recorder.copyBufferToImage(image, stagingBuffer.range(stagingOffset, 4L * image.width * image.height))
			recorder.transitionLayout(image, ResourceUsage.TRANSFER_DEST, ResourceUsage.shaderRead(
				VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT)
			)

			stagingOffset += 4L * image.width * image.height
		}

		recorder.end()

		val fence = boiler.sync.fenceBank.borrowFence(false, "StagingFence")
		boiler.queueFamilies().graphics.first().submit(stagingCommandBuffer, "StagingCopy", null, fence)
		fence.awaitSignal()
		boiler.sync.fenceBank.returnFence(fence)

		stagingBuffer.destroy(boiler)
		vkDestroyCommandPool(boiler.vkDevice(), stagingPool, null)

		this.vertexBuffer = boiler.buffers.createMapped(
			6L * 8L * this.images.size, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, "VertexPositions"
		)
		this.colorBuffer = boiler.buffers.createMapped(
			50L * 4 * 2, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, "VertexColors"
		)

		val descriptorBindings = VkDescriptorSetLayoutBinding.calloc(3, stack)
		boiler.descriptors.binding(descriptorBindings, 0, VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE, VK_SHADER_STAGE_FRAGMENT_BIT)
		descriptorBindings.get(0).descriptorCount(50) // TODO Sync with shader via spec constant?
		boiler.descriptors.binding(descriptorBindings, 1, VK_DESCRIPTOR_TYPE_SAMPLER, VK_SHADER_STAGE_FRAGMENT_BIT)
		boiler.descriptors.binding(descriptorBindings, 2, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_FRAGMENT_BIT)

		this.descriptorSetLayout = boiler.descriptors.createLayout(stack, descriptorBindings, "CreatureDescriptorSetLayout")
		this.descriptorPool = descriptorSetLayout.createPool(1, 0, "CreatureDescriptorPool")
		this.descriptorSet = descriptorPool.allocate(1)[0]

		this.pipelineLayout = boiler.pipelines.createLayout(
			null, "CreaturePipelineLayout", descriptorSetLayout.vkDescriptorSetLayout
		)

		val vertexBindings = VkVertexInputBindingDescription.calloc(1, stack)
		vertexBindings.get(0).set(0, 8, VK_VERTEX_INPUT_RATE_VERTEX)

		val vertexAttributes = VkVertexInputAttributeDescription.calloc(1, stack)
		vertexAttributes.get(0).set(0, 0, VK_FORMAT_R32G32_SFLOAT, 0)

		val vertexInput = VkPipelineVertexInputStateCreateInfo.calloc(stack)
		vertexInput.`sType$Default`()
		vertexInput.pVertexBindingDescriptions(vertexBindings)
		vertexInput.pVertexAttributeDescriptions(vertexAttributes)

		val builder = GraphicsPipelineBuilder(boiler, stack)
		builder.simpleShaderStages(
			"CreatureShaders", "mardek/playground/shaders/creature.vert.spv",
			"mardek/playground/shaders/creature.frag.spv"
		)
		builder.ciPipeline.pVertexInputState(vertexInput)
		builder.simpleInputAssembly()
		builder.dynamicViewports(1)
		builder.simpleRasterization(VK_CULL_MODE_NONE)
		builder.noMultisampling()
		builder.noDepthStencil()
		builder.simpleColorBlending(1)
		builder.dynamicStates(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR)
		builder.ciPipeline.layout(pipelineLayout)
		builder.dynamicRendering(0, VK_FORMAT_UNDEFINED, VK_FORMAT_UNDEFINED, window.surfaceFormat)
		this.graphicsPipeline = builder.build("CreaturePipeline")

		this.sampler = boiler.images.createSimpleSampler(
			VK_FILTER_NEAREST, VK_SAMPLER_MIPMAP_MODE_NEAREST,
			VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_BORDER, "CreatureSampler"
		)

		val writeImages = VkDescriptorImageInfo.calloc(50, stack)
		for (index in 0 until writeImages.capacity()) {
			val imageView = if (index < this.images.size) this.images[index].vkImageView else this.images[0].vkImageView
			writeImages.get(index).set(VK_NULL_HANDLE, imageView, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
		}
		val writeSampler = VkDescriptorImageInfo.calloc(1, stack)
		writeSampler.get(0).set(sampler, VK_NULL_HANDLE, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
		val descriptorWrites = VkWriteDescriptorSet.calloc(3, stack)
		boiler.descriptors.writeImage(descriptorWrites, descriptorSet, 0, VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE, writeImages)
		boiler.descriptors.writeImage(descriptorWrites, descriptorSet, 1, VK_DESCRIPTOR_TYPE_SAMPLER, writeSampler)
		boiler.descriptors.writeBuffer(stack, descriptorWrites, descriptorSet, 2, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, colorBuffer.fullRange())
		vkUpdateDescriptorSets(boiler.vkDevice(), descriptorWrites, null)
	}

	override fun recordFrame(
		stack: MemoryStack,
		frameIndex: Int,
		recorder: CommandRecorder,
		acquiredImage: AcquiredImage,
		boiler: BoilerInstance
	) {
		val hostVertexPositions = memByteBuffer(vertexBuffer.hostAddress, vertexBuffer.size.toInt())
		val hostVertexColors = memByteBuffer(colorBuffer.hostAddress, colorBuffer.size.toInt())
		var imageIndex = 0
		for ((rawDepth, partState) in testAnimationState.parts.withIndex()) {
			val matrix = partState.matrix ?: continue
			val (scaleX, scaleY) = if (matrix.hasScale) Pair(matrix.scaleX, matrix.scaleY) else Pair(
				1f,
				1f
			)

			val variation = partState.part.variations.find { it.name == preferredVariation } ?: partState.part.variations.first()
			for (entry in variation.entries) {
				val magicScale = 20f // I have no clue why, but I need to divide all translations by this
				val jomlMatrix = Matrix3x2f(
					scaleX, matrix.rotateSkew0, matrix.rotateSkew1, scaleY,
					matrix.translateX / magicScale,
					matrix.translateY / magicScale
				).translate(entry.rect.Xmin / magicScale, entry.rect.Ymin / magicScale)

				val jpexExportScale = 1
				val color = partState.color
				if (color != null) {
					fun transform(value: Int) = (255.0 * (value / 256.0)).roundToInt()
					fun transform(values: IntArray): Int {
						val red = transform(values[0])
						val green = transform(values[1]) shl 8
						val blue = transform(values[2]) shl 16
						val alpha = transform(values[3]) shl 24
						return red or green or blue or alpha
					}

					hostVertexColors.putInt(transform(intArrayOf(color.redMulti, color.greenMulti, color.blueMulti, color.alphaMulti)))
					hostVertexColors.putInt(transform(intArrayOf(color.redAdd, color.greenAdd, color.blueAdd, color.alphaAdd)))
				} else hostVertexColors.putInt(-1).putInt(0)

				for (corner in arrayOf(Pair(0f, 0f), Pair(1f, 0f), Pair(1f, 1f), Pair(1f, 1f), Pair(0f, 1f), Pair(0f, 0f))) {
					val image = this.images[imageIndex]
					val position = jomlMatrix.transformPosition(Vector2f(
						corner.first * image.width.toFloat() / jpexExportScale,
						corner.second * image.height.toFloat() / jpexExportScale
					))
					hostVertexPositions.putFloat(
						position.x * 0.01f * acquiredImage.height() / acquiredImage.width()
					).putFloat(
						position.y * 0.01f
					)
				}
				imageIndex += 1 // TODO properly map this
			}
		}

		val colorAttachments = VkRenderingAttachmentInfo.calloc(1, stack)
		recorder.simpleColorRenderingAttachment(
			colorAttachments.get(0), acquiredImage.image().vkImageView, VK_ATTACHMENT_LOAD_OP_CLEAR,
			VK_ATTACHMENT_STORE_OP_STORE, 0.1f, 0.3f, 0.9f, 1f
		)
		recorder.beginSimpleDynamicRendering(
			acquiredImage.width(), acquiredImage.height(),
			colorAttachments, null, null
		)
		vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline)
		recorder.dynamicViewportAndScissor(acquiredImage.width(), acquiredImage.height())
		recorder.bindGraphicsDescriptors(pipelineLayout, descriptorSet)
		vkCmdBindVertexBuffers(recorder.commandBuffer, 0, stack.longs(vertexBuffer.vkBuffer()), stack.longs(0))
		vkCmdDraw(recorder.commandBuffer, 6 * this.images.size, 1, 0, 0)
		recorder.endDynamicRendering()
	}

	override fun cleanUp(boiler: BoilerInstance) {
		super.cleanUp(boiler)

		vkDestroyPipeline(boiler.vkDevice(), graphicsPipeline, null)
		vkDestroyPipelineLayout(boiler.vkDevice(), pipelineLayout, null)
		vkDestroySampler(boiler.vkDevice(), sampler, null)

		vertexBuffer.destroy(boiler)
		colorBuffer.destroy(boiler)
		descriptorPool.destroy()
		descriptorSetLayout.destroy()

		for (image in images) image.destroy(boiler)
	}
}
