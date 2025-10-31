# vk2d
vk2d is a 2d rendering library written in Java that uses Vulkan.
It is designed to get good performance (high framerates), at the expense of being more complicated than e.g. Swing with `Graphics2D`.
This library can be used without a good understanding of Vulkan, but knowing Vulkan definitely helps.
I made this library for the MARDEK game that I am rewriting, but I intend to move it to its own repository someday.

## Getting started
### The easy approach
The easiest way to get started, is by using `Vk2dWindow.bootstrap`:
```java
public class YourApplication extends Vk2dWindow {

	private static final boolean CAP_FPS = false;

	private YourApplication(VkbWindow window) {
		super(window, CAP_FPS);
	}

	@Override
	protected void setupConfig(Vk2dConfig config) {
		// In this example, we only draw flat colors.
		// More complicated examples will enable more properties of `config`
		config.color = true;
	}

	@Override
	protected void renderFrame(
		Vk2dSwapchainFrame frame, int frameIndex,
		CommandRecorder recorder, AcquiredImage swapchainImage, BoilerInstance boiler
	) {
		printFps();

		int initialColorCapacity = 1000; // This is often a reasonable capacity
		var colorBatch = pipelines.color.addBatch(frame.swapchainStage, initialColorCapacity);
		colorBatch.fill(300, 100, 600, 600, srgbToLinear(rgb(255, 0, 0)));
	}

	public static void main(String[] args) {
		int applicationVersion = 1;
		Vk2dWindow.bootstrap("YourApplicationName", applicationVersion, Vk2dValidationMode.NONE, YourApplication::new);
	}
}
```
This is the only way that doesn't require any knowledge of Vulkan: it simply sets up a window for you, and let's you render in it.

### Alternative approaches
If you know Vulkan and need a more flexible way, you can create your own `BoilerInstance` and set up your own render loop.
(Check the source code of `Vk2dWindow.bootstrap`, it's not that much...)
It is also possible to use vk2d without any window at all, to simply render onto a `VkImage`. In fact, many unit tests do just that.
TODO create instructions/sample for this

## Rendering flow
### The simple rendering flow
In a 'simple' rendering flow, you simply render stuff onto a `Vk2dFrame`,
which is typically the `Vk2dSwapchainFrame` that you get in `renderFrame(...)`.
In this simple flow, there are basically 2 things that you need to do:
1. Create *batches*, for instance by using `pipelines.color.addBatch(frame.swapchainStage, initialCapacity)`.
2. Call the methods of these batches to actually draw stuff, for instance `colorBatch.fill(minX, minY, maxX, maxY, color)`.

TODO FINISH THIS

## Pipelines
- [color](docs/pipeline/color.md)
- [oval](docs/pipeline/oval.md)
- [image](docs/pipeline/image.md)
- [kim](docs/pipeline/kim.md)
- [glyph](docs/pipeline/glyph.md)
- [blur](docs/pipeline/blur.md)
