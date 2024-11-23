package mardek.renderer.ui

/**
 * The types of layouts supported by menu components. Components are always placed at menu coordinates, but the layout
 * determines how these coordinates will be transformed to window coordinates.
 *
 * All layouts will transform higher menu X-coordinates to the right of lower menu X-coordinates and higher menu
 * Y-coordinates above lower menu Y-coordinates. There are 3 typically desired properties of layouts:
 * - 1 unit on the X-axis is equally long as 1 unit on the Y-axis
 * - The bottom-left corner of the window is at menu coordinates (0, 0)
 * - The top-right corner of the window is at menu coordinates (1, 1)
 *
 * Each layout guarantees exactly 2 of these properties. (It is even impossible to support all 3 when the width of the
 * window is not equal to the height of the window.) This means that picking the right layout is always a matter of
 * compromises (unless the window width is equal to the window height, in which case all layouts are identical). Thus,
 * picking the right layout is a matter of compromises.
 */
enum class SpaceLayout {
	/**
	 * The bottom-left point is (0, 0) and the top-right point is (1, 1). If the width is not equal to the height,
	 * 1 unit on the X-axis will not have the same length as 1 unit on the Y-axis.
	 *
	 * ## Purpose
	 * This layout is useful when you really want the bottom-left corner to be at (0, 0) and the top-right corner to be
	 * at (1, 1). When you use this, the 'distortion problem' will be propagated to the child components.
	 *
	 * For instance, imagine the following scenario:
	 * - A component is placed between (0, 0) and (0.25, 0.25) in menu coordinates
	 * - The width of the window is 800 pixels
	 * - The height of the window is 400 pixels
	 *
	 * This will cause the window coordinates of the component to be between (0, 0) and (200, 100), so the user will
	 * perceive the width of the component to be twice as big as the height of the component. This is probably not what
	 * the programmer intended since the width in *menu coordinates* is equal to the height in menu coordinates.
	 *
	 * So, when you choose this option, you should ensure that your components can handle different window sizes.
	 */
	Simple,

	/**
	 * The bottom-left point is (0, 0) and the top-right point is (1, *d*) where *d* is some *positive* number. The
	 * value of *d* will be chosen such that 1 unit on the X-axis will have the same length as 1 unit on the Y-axis.
	 */
	GrowUp,

	/**
	 * The bottom-left point is (0, *d*) and the top-right point is (1, 1) where *d* is some real number that is
	 * smaller than 1 (it may or may not be positive). The value of *d* will be chosen such that 1 unit on the X-axis is
	 * equally long as 1 unit on the Y-axis.
	 */
	GrowDown,

	/**
	 * The bottom-left point is (0, 0) and the top-right point is (*d*, 1) where *d* is some *positive* number. The
	 * value of *d* will be chosen such that 1 unit on the X-axis will have the same length as 1 unit on the Y-axis.
	 */
	GrowRight
}
