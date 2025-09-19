# Audio

## Code
The audio module contains the code that handles the interaction with
OpenAL, which is used to play the sounds & music. The module should be
used by creating an instance of `AudioUpdater`, and invoking its `run()`
method on a background thread. At the time of writing this, it is simply
used by this code:
```kotlin
Thread {
    val audioUpdater = AudioUpdater(gameState)
    UpdateLoop({ loop ->
        if (mainThread.isAlive) audioUpdater.update()
        else loop.stop()
    }, 10_000_000L).run()
    audioUpdater.destroy()
}.start()
```
This code will check the game state every 10 milliseconds to decide which
sounds & music should be played. To do so, it will briefly acquire the
global game state lock. After releasing the lock, it will play the
actual audio.

## Resources
Currently, the `resources` directory of this module contains several
sound effects, and contains all the music of the game. I ripped them from
the original game, and converted them to `.ogg` files, which are
convenient to use from OpenAL.

### Sound effects
I would like to stop putting the sound effects in the `audio` module:
I would like to put them in the `importer` instead, and make them part
of the exported content, since this will become more flexible.

### Music
At some point, I would also like to move the music to the importer, but
this is currently a bad idea since they would take up too much space in
the exported binaries. I should someday build a better system for this,
but not anytime soon.
