package mardek.input

enum class InputKey {
	MoveLeft,
	MoveUp,
	MoveRight,
	MoveDown,
	Click,
	Interact,
	Cancel,
	ToggleMenu,
	Escape,

	/**
	 * When this key is pressed while the player is typing in a text box, the last character should be removed
	 */
	BackspaceLast,

	/**
	 * When this key is pressed while the player is typing in a text box, the first character should be removed
	 */
	BackspaceFirst,

	/**
	 * The cheat key, currently used to bypass collision & avoid random battles
	 */
	Cheat,

	/**
	 * A cheat key to scroll to the 'next' area (in some arbitrary area ordering)
	 */
	ScrollUp,

	/**
	 * A cheat key to scroll to the 'previous' area (in some arbitrary area ordering)
	 */
	ScrollDown
}
