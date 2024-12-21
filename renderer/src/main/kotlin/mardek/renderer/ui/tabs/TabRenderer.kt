package mardek.renderer.ui.tabs

abstract class TabRenderer {

	abstract fun beforeRendering()

	abstract fun render()

	open fun postUiRendering() {}
}
