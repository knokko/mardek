# MARDEK
## Rewrite of the MARDEK flash game series in Kotlin

### Development environment
#### OpenJDK 21 to 24
First of all, install OpenJDK **21**.
Anything below JDK 21 will almost certainly fail.
JDK 24 seems to work as well, but JDK 25 does not.
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

### Project structure
This project consists of several modules:
- [audio](audio/README.md) contains the audio player code (OpenAL)
  and the music.
- [content](content/README.md) contains the classes to model all
  the game content (e.g. items, areas, monsters).
- [game](game/README.md) contains the code that launches the game
  and glues all other modules.
- [importer](importer/README.md) contains the code to parse the Flash
  MARDEK and convert it to a 'nicer' format used by the rest of this
  engine/project.
- [input](input/README.md) models the keyboard/mouse events, which will
  be read by `state` and supplied by `game`.
- [renderer](renderer/README.md) ensures that the state of the game is 
  shown on your screen. It uses the Vulkan graphics API for rendering.
- [state](state/README.md) models and tracks the state of the game (both in
  the title screen and when actually in-game)
- [vk2d](vk2d/README.md) is a 2d rendering library that I wrote for
  MARDEK, and is used by `renderer`. I intend to eventually move it to
  its own GitHub repository once it matures.

### Creating releases
To create releases that can be given to players/testers
(without them needing to install OpenJDK or the Vulkan SDK),
you should simply push your code to GitHub. This repository has
a GitHub Actions workflow that will generate:
- an exe file for Windows x64
- an executable for Linux x64
- a (totally untested) executable for Linux arm64
- a (totally untested) executable for macOS x64
- a (totally untested) executable for macOS arm64
- a jar file for any OS/architecture supported by LWJGL,
  but requires OpenJDK 21+ to run

### License notes
Since this repository contains many assets made by Tobias Cornwall
(the creator of MARDEK), I am reluctant to provide a license for
this repository, since I don't own these assets. He has given
permission to finish/rewrite the game, but I don't know the details
about this.
