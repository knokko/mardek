package mardek.renderer.ui.tabs

abstract class TabRenderer {

	abstract fun beforeRendering()

	open fun renderBackgroundRectangles() {}

	abstract fun render()

	open fun postUiRendering() {}
}
