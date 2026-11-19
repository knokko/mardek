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
	// TODO BITSER Jar
}
