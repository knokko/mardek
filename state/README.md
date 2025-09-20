# State
The `state` module defines and manages the game state.

The `GameStateManager` class is the core of this module. The `game`
module creates an instance of `GameStateManager`. It calls the 
`update(...)` method of the `GameStateManager` 100 times per second.

Furthermore, it assigns an `InputManager` to the `GameStateManager`.
The `game` module will send *events* (e.g. the mouse moved or a key
on the keyboard was pressed). These events are defined by the `input` 
module. These events are stored in the `InputManager`.

The `GameStateManager` will check the events in the `InputManager`,
and use those to determine whether it should change the state
(e.g. move the player characters when the arrow keys are pressed).
