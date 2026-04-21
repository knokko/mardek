# vk2d
vk2d is a 2d rendering library written in Java that uses Vulkan.
It is designed to get good performance (high framerates),
at the expense of being more complicated than e.g. Swing with `Graphics2D`.
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
This is the only way that doesn't require any knowledge of Vulkan:
it simply sets up a window for you, and let's you render in it.

### Alternative approaches
If you know Vulkan and need a more flexible way,
you can create your own `BoilerInstance` and set up your own render loop.
(Check the source code of `Vk2dWindow.bootstrap`, it's not that much...)
It is also possible to use vk2d without any window at all, to simply render onto a `VkImage`.
In fact, many unit tests do just that. An example of 'headless' rendering is shown here:
```java
class HeadlessPlayground {

	public static void main(String[] args) {
		// Initialize vk-boiler
		var boiler = new BoilerBuilder(
				VK_API_VERSION_1_0, "HeadlessPlayground", 1
		).enableDynamicRendering().build();

		// Initialize vk2d
		var config = new Vk2dConfig();
		config.color = true;

		var vk2d = new Vk2dInstance(boiler, config);
		var pipelines = new Vk2dPipelines(vk2d, Vk2dPipelineContext.dynamicRendering(boiler, VK_FORMAT_R8G8B8A8_SRGB));

		// Allocate memory for the target image and the per-frame buffer
		var memoryCombiner = new MemoryCombiner(boiler, "HeadlessMemory");
		var perFrameBuffer = new PerFrameBuffer(memoryCombiner.addMappedBuffer(
				1000L, pipelines.perFrameBufferAlignment(), pipelines.perFrameBufferUsage()
		));
		var targetImage = memoryCombiner.addImage(new ImageBuilder(
				"HeadlessTargetImage", 100, 100
		).colorAttachment().addUsage(VK_IMAGE_USAGE_TRANSFER_SRC_BIT), 1f);
		var resultImageDataBuffer = memoryCombiner.addMappedBuffer(
				4L * targetImage.width * targetImage.height, 4L, VK_BUFFER_USAGE_TRANSFER_DST_BIT
		);
		var memory = memoryCombiner.build(false);

		// Start the frame
		perFrameBuffer.startFrame(0);
		var frame = new Vk2dFrame(perFrameBuffer, VK_NULL_HANDLE, null);
		var stage = new Vk2dRenderStage(targetImage, perFrameBuffer, null, ResourceUsage.TRANSFER_SOURCE);
		frame.stages.add(stage);

		// Add render commands
		var colorBatch = pipelines.color.addBatch(stage, 4);
		colorBatch.fill(0, 0, 100, 30, rgb(1f, 0f, 0f));
		colorBatch.gradient(
				10, 50, 80, 70,
				rgb(0f, 0f, 1f), rgb(0f, 1f, 1f), rgb(0f, 0f, 1f)
		);

		// Record & run render commands + copy image data to the host + await completion
		SingleTimeCommands.submit(boiler, "HeadlessCommands", recorder -> {
			frame.record(recorder);
			recorder.copyImageToBuffer(targetImage, resultImageDataBuffer);
			recorder.bufferBarrier(resultImageDataBuffer, ResourceUsage.TRANSFER_DEST, ResourceUsage.HOST_READ);
		}).destroy();

		// Save result image to disk
		var bufferedImage = ImageCoding.decodeBufferedImage(
				resultImageDataBuffer.byteBuffer(), targetImage.width, targetImage.height
		);
		try {
			ImageIO.write(bufferedImage, "PNG", new File("headless-playground.png"));
		} catch (IOException failed) {
			throw new RuntimeException(failed);
		}

		// Clean everything up
		pipelines.destroy();
		vk2d.destroy();
		memory.destroy(boiler);
		boiler.destroyInitialObjects();
	}
}
```

## Rendering flow
### The simple rendering flow
In a 'simple' rendering flow, you simply render stuff onto a `Vk2dFrame`,
which is typically the `Vk2dSwapchainFrame` that you get in `renderFrame(...)`.
In this simple flow, there are basically 2 things that you need to do:
1. Create *batches*, for instance by using `pipelines.color.addBatch(frame.swapchainStage, initialCapacity)`.
2. Call the methods of these batches to actually draw stuff, for instance `colorBatch.fill(minX, minY, maxX, maxY, color)`.

### Using multiple pipelines and batches
If you use `pipelines.color.addBatch(...)`, you will get a `Vk2dColorBatch`,
which can only render gradients and flat colors.
For more complicated drawing, you need to use different pipelines, e.g. `pipelines.image.addBatch(...)`.

When you create multiple batches, the *order in which you create the batches* is important,
because it defines the *order in which the batches are drawn*: newer batches will be drawn on top of older batches.
Draw operations *within the same batch* are simply drawn in order. For instance, if you do
```
var colorBatch = pipelines.color.addBatch(...);
var imageBatch = pipelines.image.addBatch(...);

colorBatch.drawColor1...
imageBatch.drawImage1
colorBatch.drawColor2...
```

Then:
- `drawColor1` happens first (first draw of the first batch)
- `drawColor2` happens second (second draw of the *first batch*)
- `drawImage1` happens last (first draw of the *last batch*)

## Pipelines
All drawing in vk2d is done using a *batch* of a *pipeline*.
Every pipeline can draw different things, and has their own type of batch
(e.g. `Vk2dColorPipeline` has `Vk2dColorBatch`es).

### Standard pipelines
The following standard pipelines are included with vk2d:
- [color](docs/pipeline/color.md) for drawing quads with a gradient or flat color
- [oval](docs/pipeline/oval.md) for drawing circles or ovals with a gradient or flat color
- [image](docs/pipeline/image.md) for drawing general-purpose possibly-compressed images
- [kim](docs/pipeline/kim.md) for drawing compressed pixelated 'fake' images
- [simple text](docs/pipeline/simple-text.md) for drawing simple text
- [fancy text](docs/pipeline/fancy-text.md) for drawing text with gradient colors
- [multiply](docs/pipeline/multiply.md) for multiplying the output colors from earlier rendering (niche)
- [blur](docs/pipeline/blur.md) for blurring the output of a stage, and drawing that onto another stage

### Custom pipelines
Furthermore, you can create your own custom pipelines by extending `Vk2dPipeline` and `Vk2dBatch`.
These are no docs for this, but you can take a look at the source code of the standard pipelines
(`Vk2dColorPipeline` is probably the easiest example).

To 'register' the pipelines, you should create a class that extends `Vk2dPipelines` (plural),
and override the `createPipelines` method of your class that extends `Vk2dWindow`:
```
class MyWindow extends Vk2dWindow {
...

	MyPipelines myPipelines;

	@Override
	protected Vk2dPipelines createPipelines(Vk2dInstance instance, Vk2dPipelineContext context) {
		myPipelines = new MyPipelines(instance, context);
		return myPipelines;
		// Now you can use myPipelines in the renderFrame method
	}
...
}
```

### Performance considerations
#### Minimize the number of stages (render passes)
By default, you only get 1 render stage (the swapchain render stage), but you can add more stages.
For instance, the blur pipeline requires another stage as input.

Using more stages is possible, but it is also rather expensive because each stage requires its own render pass.
Render passes are one of the most expensive operations that need to happen every frame.
They are normally more expensive than pipeline switches, and much more expensive than draw calls.
You can still have plenty of render passes per frame, especially on modern hardware,
but please don't use many more than you need.

#### Minimize the number of batches (pipelines)
You should aim to minimize the number of *batches* because every batch requires a
(somewhat expensive) graphics pipeline (shader) switch.
Graphics pipeline switches are normally cheaper than render passes,
but more expensive than draw calls.

So you should share each batch will all components that need such a batch.
For instance, if all your components need to render some gradients or flat color,
you should create 1 `Vk2dColorBatch`, and share it with all components.
In many cases, you need at most 1 batch per unique pipeline.

However, there are cases where you really need multiple batches of the same pipeline, for instance:
1. You need to draw some background color (using a color batch).
2. You need to draw some text *in front of* that background color (using a text batch).
3. You need to draw a translucent rectangle *in front of* that text (using a color batch).

Sharing the same color batch is *not* an option since later batches are drawn in front of older batches:
- If you create the color batch *before* the text batch,
then the text will be drawn *in front of* the translucent rectangle from step 3.
- If you create the color batch *after* the text batch,
then the background color from step 1 will be drawn *in front of* the text,
which would hide the text completely.

In cases like this, you really need to have multiple color batches.

#### Choosing the right batch sizes (draw calls)
Whenever you create a new batch, you need to specify the batch size/capacity, in *triangles*.
For instance, creating a color batch typically looks like this:
```
var colorBatch = pipelines.color.addBatch(stage, 4);
```
In this case, the initial capacity of the batch is 4 triangles (2 flat color rectangles, or 2 gradient rectangles).
- This would reserve enough space on the `PerFrameBuffer` for 4 color batch triangles.
- If you render at most 4 triangles with the color batch, they will be rendered using *only 1 draw call*.

If you render more than 4 triangles with that color batch,
additional space (typically twice the size of the previous/initial allocation)
will be reserved on the `PerFrameBuffer`,
but this additional space will probably *not* be adjacent to the original allocation.
- Each additional space reservation will need its *own draw call*.

Draw calls in Vulkan are not very expensive, but minimizing them is still a good practice.
In vk2d, you can minimize them by making sure the initial batch size/capacity is sufficiently large.
If you do that perfectly, you will need only 1 draw call per batch, which is the best you can hope for.
The exceptions to this rule are:
- `Vk2dImageBatch` needs a different draw call whenever it switches to another image (which happens a lot).
You can potentially optimize draw calls for this pipeline by grouping your image drawing operations by their image.
For instance, drawing image X first, image Y second, and image X third is potentially less efficient than drawing
image X first, image X second, and image Y third.
- `Vk2dSimpleTextBatch` and `Vk2dFancyText` batch need a different draw call whenever they switch to another texture
atlas. Since all fonts use different atlases, grouping your text drawing operations by font may help.

Vk2d could use the descriptor indexing feature to get rid of these exceptions,
but this feature is not supported on some crappy old iGPUs.
But... these crappy old iGPUs are also the devices for which you would want to optimize,
since the modern GPUs can very easily render 2d stuff anyway...)

Note that choosing a ridiculously large batch size will lead to only 1 draw call, which is nice,
but the drawback is that it wastes a lot of `PerFrameBuffer` space:
- If you claim too much space during 1 frame, the `PerFrameBuffer` will overflow (throw an exception).
You could create a bigger `PerFrameBuffer`, but that also costs more RAM,
and reduces the chance that the whole buffer fits on a cache line.
- You waste a lot of space between the draw calls,
which potentially increases the number of cache lines that you use each frame.

Usually, it is a good idea to claim a bit of extra capacity (e.g. 1.5x or 2x),
but not to an insane degree (e.g. 100x).
To find out how much you need, you can either count/calculate your method invocations,
or put the following code in your window class (that extends `Vk2dWindow`):
```
@Override
public boolean shouldPrintBatchSizes() {
	return true;
}
```
This should print how much capacity each draw call uses and wastes.
Since printing all this is probably much more expensive than drawing the frame itself,
you should only enable this to count your draw calls, and disable it once you're done.

