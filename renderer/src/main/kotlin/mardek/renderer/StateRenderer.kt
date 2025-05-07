package mardek.renderer

abstract class StateRenderer {

	open fun beforeRendering(context: RenderContext) {}

	abstract fun render(context: RenderContext)
}
