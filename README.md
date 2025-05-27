# MARDEK
## Rewrite of the MARDEK flash game series in Kotlin

### Development environment
#### OpenJDK
First of all, install OpenJDK 21.
Later versions will probably work as well, earlier versions may or may not.
After installing OpenJDK, you **should** be able to run the game by running
```
./gradlew run
```
Unfortunately, this has only been tested on a limited set of computers,
so you never know whether it will work on another... if not, contact the
developers.

#### IntelliJ (optional)
Installing an IDE is the next step (but optional).
I would strongly recommend to choose IntelliJ as IDE,
both community edition and ultimate edition should work fine.

When you open this folder for the first time in IntelliJ,
it will take some time to import all kinds of Gradle stuff and pull
dependencies, which probably takes less than a minute.

You should now be able to run the game by going to `MardekGame.kt`
and clicking the green triangle on the left of the `main` method
(hint: press shift twice to search for files).

#### Vulkan SDK
Some development functionality requires you to install the
[Vulkan SDK](https://vulkan.lunarg.com/sdk/home).
After installing the Vulkan SDK, you should see the
Vulkan Validation Layer when you run `vulkaninfo --summary` in a terminal:
```
VK_LAYER_KHRONOS_validation       Khronos Validation Layer
```
(Although a computer restart may be required on Windows.)
When this layer is present, you should be able to run all the unit tests:
```
./gradlew test
```
Furthermore, you should now be able to run the game with the
validation layer enabled:
```
./gradlew runValidation
```
Alternative, you can run `MardekGame.kt` with a `validation`
program argument.

#### Flash importing
Some content is automatically imported from the Flash game.
However, since this rewrite is far from finished, we often need to
change the import code to import more content, after which we need
to re-import the flash content.

Before the import script will work, you need to copy `MARDEK.swf`
from Steam to `flash/MARDEK.swf`. You should be able to find it
at `steamapps/common/MARDEK/MARDEK.swf`
(wherever your `steamapps` directory may be).
After this is done, you should be able to import the content:
```
./gradlew exportContent
```
If the above command failed with on Linux:
```
Bc7 compression failed: ./bc7enc-linux: error while loading shared libraries: libomp.so.5: cannot open shared object file: No such file or directory
```
You should install `libomp-dev`, for instance by running
```
sudo apt install libomp-dev
```

### Video settings
Currently, there is no nice video settings UI, but there are
some things you can tweak.

#### Graphics card
By default, the game will prefer a discrete graphics card over
an integrated graphics card, and it will prefer an integrated
graphics card over a CPU implementation.

You can use the `integrated` program argument to let the game
prefer an integrated graphics card (this argument is only
useful if you have both a discrete and integrated graphics card),
and you can use the `cpu` program argument to let the game prefer
a CPU implementation (this only works when you actually install
such an implementation like llvmpipe or swiftshader).

#### FPS cap
By default, the FPS is capped to the refresh rate of your monitor
(because more FPS is useless in a game like MARDEK, and just
wastes power). You can uncap the FPS by opening `GameWindow.kt`
and replacing `VK_PRESENT_MODE_FIFO_KHR` with
`VK_PRESENT_MODE_MAILBOX_KHR`.

### Project structure
This project consists of several modules:
- `audio` contains the audio player code (OpenAL) and the music.
- `content` contains classes to model all the game content
  (e.g. items, areas, monsters).
- `game` contains the code that launches the game and is
  basically the glue that connects all the other modules.
- `importer` contains the code to import the Flash content.
- `input` models the game controls, which will be written by
  `game` and read by `renderer` and `state`.
- `renderer` ensures that the state of the game is shown on
  your screen. It uses the Vulkan graphics API for rendering.
- `state` models and tracks the state of the game (both in
  the title screen and when actually in-game)
- `ui-renderer` is a mini library for rendering UI with
  Vulkan, and is used by `renderer`

### Creating releases
To create releases that can be given to players/testers
(without them needing to install OpenJDK or the Vulkan SDK),
you should simply push your code to GitHub. This repository has
a GitHub Actions workflow that will generate:
- an exe file for Windows x64
- an executable for Linux x64
- a (totally untested) executable for Linux arm64
- a (totally untested) executable for MacOS x64
- a (totally untested) executable for MacOS arm64
- a jar file for any OS/architecture supported by LWJGL,
  but requires OpenJDK 21+ to run

### License notes
Since this repository contains many assets made by Tobias Cornwall
(the creator of MARDEK), I am reluctant to provide a license for
this repository, since I don't own these assets. He has given
permission to finish/rewrite the game, but I don't know the details
about this.
