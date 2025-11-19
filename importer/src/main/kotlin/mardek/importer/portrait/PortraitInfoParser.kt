package mardek.importer.portrait

import mardek.content.Content
import mardek.content.portrait.PortraitInfo
import mardek.importer.area.parseFlashString
import mardek.importer.util.parseActionScriptCode
import mardek.importer.util.parseActionScriptObject
import java.lang.Integer.parseInt
import java.util.Locale

internal fun parsePortraitScript(content: Content, script: String) {
	val code = parseActionScriptCode(script.split("\n"))
	val mapping = parseActionScriptObject(code.variableAssignments["with_avatar"]!!)
	for ((key, properties) in mapping) {
		content.portraits.info.add(parsePortraitInfo(content, key, properties))
	}
}

private fun parsePortraitInfo(content: Content, key: String, rawValue: String): PortraitInfo {
	if (!key.startsWith("s_")) throw IllegalArgumentException("Unexpected key $key")

	val properties = parseActionScriptObject(rawValue)

	fun parseSkin(skinKey: String): String? {
		val skinValue = properties[skinKey] ?: return null
		return try {
			parseInt(skinValue).toString()
		} catch (_: NumberFormatException) {
			val skinName = parseFlashString(skinValue, skinKey) ?: return null
			skinName.lowercase(Locale.ROOT)
		}
	}

	val elementalBackgroundName = properties["elembg"]
	val element = if (elementalBackgroundName != null) content.stats.elements.find {
		it.rawName == parseFlashString(elementalBackgroundName, "elembg")!!
	} else null

	val flashName = key.substring(2).lowercase(Locale.ROOT)
	return PortraitInfo(
		flashName = flashName,
		rootSkin = parseFlashString(properties["t"]!!, "root skin")!!.lowercase(Locale.ROOT),
		faceSkin = parseSkin("face") ?: flashName,
		hairSkin = parseSkin("hair") ?: flashName,
		eyeSkin = parseSkin("eyes") ?: flashName,
		eyeBrowSkin = parseSkin("eyebrows") ?: flashName,
		mouthSkin = parseSkin("mouth") ?: "",
		armorSkin = parseSkin("armour") ?: flashName,
		robeSkin = parseSkin("robe"),
		faceMask = parseSkin("facemask"),
		ethnicitySkin = parseSkin("ethnicity") ?: "1",
		voiceStyle = parseSkin("voice"),
		elementalBackground = element,
	)
}
