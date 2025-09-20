# Input
The `input` module is a small module that defines the keyboard/mouse
for the game-state updater. The game-state updater depends on this
module rather than on windowing managers like `GLFW` or `SDL`,
which makes unit testing and porting easier.
