package mardek.editor

import java.util.Locale

enum class MardekOS {

	/**
	 * Windows X64
	 */
	Windows,

	/**
	 * Linux X64
	 */
	Linux;

	override fun toString() = name.lowercase(Locale.ROOT)

	companion object {
		fun getByName(name: String) = entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
	}
	// TODO BITSER Jar
}
