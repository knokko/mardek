package mardek.editor.client.navbar

enum class NavbarCategory(private val overrideDisplayName: String? = null) {
	File,
	Stats,
	Items;

	val displayName: String
		get() = overrideDisplayName ?: name
}
