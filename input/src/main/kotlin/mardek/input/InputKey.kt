package mardek.input

enum class InputKey {
	MoveLeft,
	MoveUp,
	MoveRight,
	MoveDown,
	Click,

	/**
	 * The primary interact button that is used for talking to people, opening doors and chests, etc...
	 */
	Interact,

	/**
	 * The cancel button, which can be used to exit UI menu's, and can also be used to skep dialogue.
	 */
	Cancel,
	ToggleMenu,
	Escape,

	/**
	 * The key that is used to toggle the chat log during dialogue.
	 */
	ToggleChatLog,

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
	CheatMove,

	/**
	 * A cheat key to scroll to the 'next' area (in some arbitrary area ordering)
	 */
	CheatScrollUp,

	/**
	 * A cheat key to scroll to the 'previous' area (in some arbitrary area ordering)
	 */
	CheatScrollDown,

	/**
	 * A cheat key to save anywhere you want
	 */
	CheatSave,
}
