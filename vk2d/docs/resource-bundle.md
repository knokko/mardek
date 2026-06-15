# Resource bundles
Many pipelines (e.g. the image pipeline and the text pipelines) need assets/resources from a **resource bundle**:
- The image pipeline needs images from the resource bundle
- The text pipelines need fonts from the resource bundle

The resource bundle system of vk2d is needed to manage these assets (efficiently).
Every vk2d application can specify at most one *standard* resource bundle that is loaded upon start-up,
as well as any number of resource bundles that are loaded later.

## The standard resource bundle
There are basically two ways to create the standard resource bundle:
- The easy & 'slow' way: create the resource bundle upon application start-up
- The complex & 'quick' way: create the resource bundle beforehand, and load it upon application start-up

In both cases, you need to override `Vk2dWindow.initialResourceBundle()`.

### Create the resource bundle upon application start-up
This is the easiest way to get started with drawing text and images.
You need to create a new `Vk2dResourceWriter`, and replay its output using `ByteArrayOutputStream`
and `ByteArrayInputStream`:
```java
@Override
protected InputStream initialResourceBundle() throws IOException {
	var writer = new Vk2dResourceWriter();

	// Add images and fonts... e.g.
	this.image1 = writer.addImage(ImageIO.read("image1.png"), Vk2dImageCompression.BC7, false);
	int fontBlob = writer.addFontBlob(Files.newInputStream("font.ttf"));
	this.font = writer.addFont(fontBlob, 0);
	writer.addFallbackAtlas(font, 8, 20f, 0.1f);

	// Replay the output
	var output = new ByteArrayOutputStream();
	writer.write(output, null);
	return new ByteArrayInputStream(output.toByteArray());
}
```
You can use `image1` in e.g. `imageBatch.simple(...)` and you can use `font` in e.g. `simpleTextBatch.drawString...)`.
An example of this is shown in
[the TextPlayground example](../src/test/java/com/github/knokko/vk2d/text/TextPlayground.java).

### Create the resource bundle beforehand
You can create the resource bundle beforehand by e.g. creating an additional main class whose `main` method
simply writes a resource bundle to a file, which is used by the real main class, for instance:
```java
public class MyResourceBundleGenerator {
	public static void main(String[] args) throws IOException {
		var writer = new Vk2dResourceWriter();

		// Add images and fonts... e.g.
		int image1 = writer.addImage(ImageIO.read("image1.png"), Vk2dImageCompression.BC7, false);
		int fontBlob = writer.addFontBlob(Files.newInputStream("font.ttf"));
		int font = writer.addFont(fontBlob, 0);
		writer.addFallbackAtlas(font, 8, 20f, 0.1f);

		// Save the output
		var output = Files.newOutputStream(new File("bundle.vk2d").toPath());
		var cacheDirectory = new File("vk2d-cache");
		cacheDirectory.mkdirs();
		writer.write(output, cacheDirectory);
		output.close();
	}
}
```
In your `initialResourceBundle` method, you only need to load it:
```java
@Override
protected Path initialResourceFile() {
	return new File("bundle.vk2d").toPath();
}
```

### Considerations
If you create the resource bundle beforehand, you can considerably reduce the start-up/loading time/memory of your application.
Furthermore, you need to have `ImageIO` (in `java.awt`) on the classpath of `MyResourceBundleGenerator`,
but you do *not* need to distribute it with your real application.

The drawback is that it adds some complexity compared to the 'simple' approach.
First of all, you need to re-create the bundle whenever you change your assets.

Furthermore, you need to somehow track which image index corresponds to which image.
If you use the 'simple' approach, you can use e.g. `this.image1 = writer.addImage(...)` and `imageBatch.simple(coordinates, this.image1)`.
But, if you generate the resource bundle beforehand, you can't just store all the image indices in fields/variables,
and you will need to find another way.

## Additional resource bundles
It is also possible to load additional resource bundles later.
The easiest way is by using `Vk2dResourceLoader.loadSimple(instance, input)`, where `input` is either an `InputStream` or `Path`,
created using either of the methods to load the *standard* resource bundle (either beforehand or live).
If you create the resource bundle live, you could also use `writer.directlyCreateBundle()`, which saves you the
hassle of capturing the output stream.

Alternatively, if you want more control, you can copy the body of `Vk2dResourceLoader.loadSimple`,
and make whatever changes you need.
This method is less easy to use, but it gives you the control to potentially share descriptor sets and video memory allocations
with other Vulkan components in your application.
