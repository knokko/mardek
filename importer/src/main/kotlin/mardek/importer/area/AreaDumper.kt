package mardek.importer.area

import com.jpexs.decompiler.flash.tags.DoActionTag
import mardek.importer.animation.getScript
import mardek.importer.util.resourcesFolder
import java.io.File
import java.nio.file.Files
import kotlin.system.exitProcess

fun main() {
    try {
        for (tag in FLASH.tags) {
            if (tag !is DoActionTag) continue
            val rawName = tag.scriptName
            val prefix = "(name: "
            val startIndex = rawName.indexOf(prefix)
            if (startIndex == -1) continue
            val endIndex = rawName.indexOf(')', startIndex)
            if (endIndex == -1) continue

            val name = rawName.substring(startIndex + prefix.length, endIndex)
            val blacklist = arrayOf(
                "titlescreen", "MusicPlayer", "plotroll", "NYearsLater",
                "newgame_CHAPTER1", "newgame_CHAPTER2", "newgame_CHAPTER3",
                "ChapterFader", "end_chapter", "WORLDMAP", "GameOver", "MAP_EDITOR"
            )

            if (name in blacklist) continue

            println(name)

            val content = getScript(tag)
            val destinationPath = "$resourcesFolder/area/data-raw/$name.txt"
            Files.writeString(File(destinationPath).toPath(), content)
        }

        exitProcess(0)
    } catch (failed: Exception) {
        failed.printStackTrace()
        exitProcess(-1)
    }
}
