package mardek.editor.client.navbar

enum class NavbarTab(val category: NavbarCategory, val overrideDisplayName: String? = null) {

	Settings(NavbarCategory.File),

	Elements(NavbarCategory.Stats),
	StatusEffects(NavbarCategory.Stats, "Status effects"),
	CreatureTypes(NavbarCategory.Stats, "Creature types"),

	ItemTypes(NavbarCategory.Items, "Item types"),
	Items(NavbarCategory.Items);

	val displayName: String
		get() = overrideDisplayName ?: name
}
