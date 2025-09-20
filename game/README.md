# Game
The `game` module is the glue that connects all other modules:
- it has the `main()` function
- it creates a `Vulkan` instance and `SDL` window
- it launches the audio thread
- it launches the game-state update thread
- it propagates keyboard/mouse events to the game-state updater
- it launches the rendering thread
