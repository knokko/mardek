# Renderer
The `renderer` module visualizes the game state. The `game` module
will give a window to the `renderer`, on which the `renderer` will
keep displaying (rendering) the game state.

This module is separated from the `state` module, which is convenient
for unit testing. Furthermore, it opens up the future possibility to
reload the renderer mid-game since the `state` doesn't depend on the
`renderer`.

The `RenderManager` class is the core of this module. The `game`
module creates an instance of this class, and invokes its
`renderFrame(...)` method every frame.
