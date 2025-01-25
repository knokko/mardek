package mardek.importer.area

import com.jpexs.decompiler.flash.SWF
import mardek.importer.util.projectFolder
import java.io.File
import java.nio.file.Files

val FLASH = run {
	// The next line will work after you copy MARDEK.swf from your steamgames to the flash directory of this repository
	// I'm reluctant to drop this file in the repository
	val input = Files.newInputStream(File("$projectFolder/flash/MARDEK.swf").toPath())
	val swf = SWF(input, true)
	input.close()

	swf
}
