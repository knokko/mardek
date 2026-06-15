# The image pipeline
As the name suggests, the image pipeline can be used to draw images.
It supports both standard RGBA8 images, as well as block-compressed images (BC1 or BC7).

## Enable the image pipeline
To enable the image pipeline, the `config.image` field must be set to `true`:
```java
@Override
protected void setupConfig(Vk2dConfig config) {
	config.image = true;
}
```

## Add images to the resource bundle
This pipeline can only draw images that are part of the [resource bundle](../resource-bundle.md).
So, you need to register all images that you want to use.
(This example shows the *easy* approach to create resource bundles, but adding images using the *efficient* approach is very similar.)
```java
@Override
protected InputStream initialResourceBundle() throws IOException {
	var writer = new Vk2dResourceWriter();

	// You may or may not want the image to be pixelated.
	// To find out what looks best, you can just try both!
	this.pixelatedImage = writer.addImage(ImageIO.read("pixelated-image.png"), Vk2dImageCompression.NONE, true);
	this.smoothImage = writer.addImage(ImageIO.read("smooth-image.png"), Vk2dImageCompression.NONE, false);

	// compressedImage occupies ~25% of the video RAM of an uncompressed image, but has slightly lower quality
	this.compressedImage = writer.addImage(ImageIO.read("smooth-image.png"), Vk2dImageCompression.BC7, false);

	// veryCompressedImage occupies ~12.5% of the video RAM of an uncompressed image, but has lower quality
	this.veryCompressedImage = writer.addImage(ImageIO.read("smooth-image.png"), Vk2dImageCompression.BC1, false);

	// Replay the output
	var output = new ByteArrayOutputStream();
	writer.write(output, null);
	return new ByteArrayInputStream(output.toByteArray());
}
```

## Creating a batch
Use `pipelines.image.createBatch(stage, batchCapacity, resourceBundle)` to create an instance of `Vk2dImageBatch`.
- For single-stage rendering, the `stage` parameter should be `frame.swapchainStage`.
- See the [README](../../README.md) for instructions on choosing the right batch size/capacity.
Every drawn image will occupy 2 triangles.
- The `resourceBundle` parameter is the resource bundle that contains the image you want to draw.
If this is the *standard* resource bundle, use `yourWindowInstance.resources` (if you render inside the `renderFrame` method, it should be `this.resources`).

## Drawing operations
The image drawing operations are:
- `imageBatch.simple(minX, minY, maxX, maxY, imageIndex)`
- `imageBatch.colored(minX, minY, maxX, maxY, imageIndex, addColor, multiplyColor)`
- `imageBatch.simpleScale(minX, minY, scale, imageIndex)`
- `imageBatch.coloredScale(minX, minY, scale, imageIndex, addColor, multiplyColor)`
- `imageBatch.fillWithoutDistortion(minX, minY, maxX, maxY, imageIndex)`
- `imageBatch.rotated(midX, midY, angle, scale, imageIndex, addColor, multiplyColor)`
- `imageBatch.transformed(x1, y1, x2, y2, x3, y3, x4, y4, imageIndex, addColor, multiplyColor)`

- The `simple` and `colored` methods can be used to draw an image within a rectangle, possibly distorting it.
- The `colored` and `coloredScale` methods can be used to draw an image at a position *without* distorting it.
- The `colored` and `coloredScale` methods can be used to draw the image while transforming its colors.
- The `fillWithoutDistortion` method can be used to draw an image within a rectangle *without* distortion,
by cropping the image if needed
- The `rotated` method can be used to draw a rotated image around the given middle/center coordinates
- The `transformed` method can be used to draw an image with an arbitrary 2d transformation.
It is the most flexible method, but also the most complicated.

## Using colors
Some methods have an `addColor` and a `multiplyColor` parameter.
You can use e.g. `ColorPacker.rgb(red, green, blue)` to define these colors.
When the color of the rendered image pixel is `pixelColor`, the output color will be
`addColor + multiplyColor * pixelColor`, using component-wise arithmetic:
`red(outputColor) = red(addColor) + red(multiplyColor) * red(pixelColor)`
(The same holds for the `green`, `blue` and `alpha` components.)
Note that the 'identity' `addColor` is the integer 0 (increases all components by 0.0) and that the 'identity'
`multiplyColor` is -1 (multiplies all components by 1.0).

## Showcase
All the image drawing methods are demonstrated below:
TODO CHAP2 ![Showcase of the image pipeline](./image.png)

TODO CHAP2 Finish this
