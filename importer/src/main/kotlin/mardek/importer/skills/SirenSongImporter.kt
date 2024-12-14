package mardek.importer.skills

import mardek.assets.skill.SirenNote
import mardek.assets.skill.SirenSong
import mardek.importer.area.parseFlashString
import mardek.importer.util.parseActionScriptNestedList
import mardek.importer.util.parseActionScriptObjectList
import java.lang.Integer.parseInt

fun parseSirenSongs(rawSongs: String) = parseActionScriptObjectList(rawSongs).map { rawSong ->
	val rawNestedNotes = parseActionScriptNestedList(rawSong["notes"]!!)
	if (rawNestedNotes !is ArrayList<*>) throw SkillParseException("Unexpected notes in $rawSong")
	val notes = rawNestedNotes.map { rawNote ->
		if (rawNote != "null") {
			if (rawNote is ArrayList<*>) {
				if (rawNote.size != 2) throw SkillParseException("Unexpected note $rawNote in $rawSong")
				SirenNote(parseInt(rawNote[0] as String), parseInt(rawNote[1] as String))
			} else {
				val noteValue = parseInt(rawNote as String)
				SirenNote(noteValue, noteValue)
			}
		} else null
	}

	SirenSong(
		name = parseFlashString(rawSong["tune"]!!, "song tune")!!,
		time = parseInt(rawSong["time"]!!),
		tempo = parseInt(rawSong["tempo"]!!),
		notes = ArrayList(notes)
	)
}
