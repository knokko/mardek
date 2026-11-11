package mardek.game

import com.github.knokko.bitser.io.BitInputStream
import com.github.knokko.vk2d.resource.DecompressingInputStream
import mardek.content.BITSER
import mardek.content.Content
import mardek.content.Content.Companion.RESOURCES_DIRECTORY
import java.io.File

fun loadContent(contentFile: File = File("$RESOURCES_DIRECTORY/content.bits")): Content {
	val input = BitInputStream(DecompressingInputStream(contentFile.toPath()))
	val content = BITSER.deserialize(Content::class.java, input)
	input.close()
	return content
}
