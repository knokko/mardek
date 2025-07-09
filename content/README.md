# Content
The content module contains the classes that define the content of the
game. For instance, it defines which properties items and monsters can
have. The root class of this module is called `Content`, and instances
of this class essentially define all the content of the game:
- it defines all playable characters
- it defines all areas
- it defines all items
- it defines all monsters
- etc...

## Exporting and loading
When the game is exported, the *content* is stored in a file called
`content.bits` in `game/src/main/resources/mardek/game`. Every modded 
variant of MARDEK (that uses this engine) will have a different
`content.bits`. The engine will load the `content.bits` file upon
start-up.
