package mardek.exporting

import com.github.knokko.bitser.options.WithParameter
import com.github.knokko.boiler.utilities.ColorPacker.alpha
import com.github.knokko.boiler.utilities.ColorPacker.blue
import com.github.knokko.boiler.utilities.ColorPacker.green
import com.github.knokko.boiler.utilities.ColorPacker.red
import com.github.knokko.compressor.Kim1Decompressor
import com.github.knokko.compressor.Kim3Decompressor
import com.github.knokko.vk2d.resource.Vk2dGreyscaleChannel
import com.github.knokko.vk2d.resource.Vk2dImageCompression
import com.github.knokko.vk2d.resource.Vk2dResourceWriter
import mardek.content.BITSER
import mardek.content.audio.AudioContent
import mardek.content.sprite.BcSprite
import mardek.content.sprite.KimSprite
import mardek.content.ui.Font
import mardek.content.ui.TitleScreenContent
import org.lwjgl.BufferUtils
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.Collections
import java.util.HashMap
import java.util.IdentityHashMap

private fun addBcImage(resourceWriter: Vk2dResourceWriter, bc: BcSprite) {
	val compression = when (bc.version) {
		0 -> Vk2dImageCompression.NONE
		1 -> Vk2dImageCompression.BC1
		4 -> Vk2dImageCompression.BC4
		7 -> Vk2dImageCompression.BC7
		else -> throw UnsupportedOperationException("Unexpected compression BC${bc.version}")
	}
	if (bc.bufferedImage != null) {
		if (compression == Vk2dImageCompression.BC4) {
			bc.index = resourceWriter.addGreyscaleImage(
				bc.bufferedImage as BufferedImage, compression, Vk2dGreyscaleChannel.ALPHA,
				false, bc.clamped,
			)
		} else {
			bc.index = resourceWriter.addImage(
				bc.bufferedImage as BufferedImage, compression, false, bc.clamped
			)
		}
	} else {
		bc.index = resourceWriter.addPreCompressedImage(
			bc.data, bc.width, bc.height,
			compression, false, bc.clamped,
		)
	}
}

private fun addKimImage(resourceWriter: Vk2dResourceWriter, kim: KimSprite) {
	kim.index = resourceWriter.addFakeImage(kim.width, kim.height, kim.data)
}

private fun addFont(resourceWriter: Vk2dResourceWriter, font: Font, extraAtlases: () -> Unit = {}) {
	val fontDataIndex = resourceWriter.addFontBlob(ByteArrayInputStream(font.data))
	font.index = resourceWriter.addFont(fontDataIndex, 0)
	resourceWriter.addAtlas(
		font.index, 8, 10f, 0.3f,
		0f, 12f, 0f,
		Float.MAX_VALUE, null,
	)
	resourceWriter.addAtlas(
		font.index, 8, 15f, 0.3f,
		0f, 20f, 0f,
		Float.MAX_VALUE, null,
	)
	extraAtlases()
	resourceWriter.addFallbackAtlas(font.index, 8, 30f, 0.3f)
}

fun exportAndPrepareVk2d(
	vk2dOutput: OutputStream, closeOutput: Boolean,
	cacheDirectory: File?, target: Any
) : ByteArray {
	val resourceWriter = Vk2dResourceWriter()

	val allKimSprites = Collections.newSetFromMap(IdentityHashMap<KimSprite, Boolean>())
	val allBcSprites = Collections.newSetFromMap(IdentityHashMap<BcSprite, Boolean>())
	val allFonts = Collections.newSetFromMap(IdentityHashMap<Font, Boolean>())
	val collectionMapping = HashMap<Class<*>, Collection<Any>>()
	collectionMapping[KimSprite::class.java] = allKimSprites
	collectionMapping[BcSprite::class.java] = allBcSprites
	collectionMapping[Font::class.java] = allFonts
	BITSER.collectInstances(target, collectionMapping, hashMapOf())

	for (sprite in allKimSprites) addKimImage(resourceWriter, sprite)
	for (sprite in allBcSprites) addBcImage(resourceWriter, sprite)
	for (font in allFonts) {
		if (target is TitleScreenContent && font === target.largeFont) {
			addFont(resourceWriter, font) {
				resourceWriter.addAtlas(
					font.index,
					8, 200f, 0.12f,
					0f, Float.MAX_VALUE,
					0f, 0.12f, "MARDEK"
				)
			}
		} else addFont(resourceWriter, font)
	}

	resourceWriter.write(vk2dOutput, cacheDirectory)

	if (closeOutput) vk2dOutput.close()

	for (sprite in allBcSprites) {
		if (sprite.data == null) sprite.data = resourceWriter.getImageData(sprite.index)
	}

	return BITSER.toBytes(target, WithParameter("exporting", null))
}

fun exportIcons(wrapped: ContentWrapper, iconOutput: OutputStream, close: Boolean) {
	val imageData = ByteBuffer.allocate(4 * 16 * 16)

	fun decompress(icon: KimSprite) {
		imageData.position(0)
		val compressedBuffer = BufferUtils.createByteBuffer(4 * icon.data!!.size)
		for (intIndex in icon.data!!.indices) {
			compressedBuffer.putInt(4 * intIndex, icon.data!![intIndex])
		}

		when (icon.version) {
			1 -> {
				val decompressor = Kim1Decompressor(compressedBuffer)
				for (y in 0 until decompressor.height) {
					for (x in 0 until decompressor.width) {
						val color = decompressor.getColor(x, y)
						imageData.put(red(color))
						imageData.put(green(color))
						imageData.put(blue(color))
						imageData.put(alpha(color))
					}
				}
			}
			3 -> {
				val decompressor = Kim3Decompressor(compressedBuffer)
				for (y in 0 until decompressor.height) {
					for (x in 0 until decompressor.width) {
						val color = decompressor.getColor(x, y)
						imageData.put(red(color))
						imageData.put(green(color))
						imageData.put(blue(color))
						imageData.put(alpha(color))
					}
				}
			}
			else -> {
				throw IllegalArgumentException("Unexpected icon KIM version ${icon.version}")
			}
		}
		iconOutput.write(imageData.array())
	}

	decompress(wrapped.applicationIcon)
	decompress(wrapped.inventoryCursor)
	decompress(wrapped.pointerCursor)
	decompress(wrapped.grabCursor)

	iconOutput.flush()
	if (close) iconOutput.close()
}

fun exportTitleScreen(wrapped: ContentWrapper, vk2dOutput: OutputStream, closeOutput: Boolean) : ByteArray {
	val titleAudio = AudioContent()
	titleAudio.musicTracks.addAll(wrapped.content.audio.musicTracks)
	titleAudio.musicCategories.addAll(wrapped.content.audio.musicCategories)
	titleAudio.titleScreenTrack = wrapped.content.audio.titleScreenTrack
	titleAudio.gameOverTrack = wrapped.content.audio.gameOverTrack

	val titleContent = TitleScreenContent(
		background = wrapped.content.ui.titleScreenBackground.shallowCopy(),
		arrowHead = wrapped.content.ui.arrowHead.shallowCopy(),
		crystalPointer = wrapped.content.ui.pointer.shallowCopy(),
		basicFont = wrapped.content.fonts.basic2.copy(),
		fatFont = wrapped.content.fonts.fat.copy(),
		largeFont = wrapped.content.fonts.large2.copy(),
		boringFont = wrapped.content.fonts.basic1.copy(),
		audio = titleAudio,
		neutralMusicNote = wrapped.neutralMusicNote,
		musicNoteShadow = wrapped.musicNoteShadow,
		playMusicIcon = wrapped.playMusicIcon,
		pauseMusicIcon = wrapped.pauseMusicIcon,
		playMusicHoveredIcon = wrapped.playMusicHoveredIcon,
		pauseMusicHoveredIcon = wrapped.pauseMusicHoveredIcon,
	)

	return exportAndPrepareVk2d(vk2dOutput, closeOutput, null, titleContent)
}
