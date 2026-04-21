# The color pipeline
The color pipeline can be used to fill quads or rectangles with a 'flat' color, or to draw gradient rectangles/quads.
This is the simplest pipeline of `vk2d`.

## Setting up
To enable the color pipelines, the `config.color` field must be set to `true`:
```java
@Override
protected void setupConfig(Vk2dConfig config) {
	config.color = true;
}
```

## Creating a batch
Use `pipelines.color.createBatch(stage, batchCapacity)` to create an instance of `Vk2dColorBatch`.
- For single-stage rendering, the `stage` parameter should be `frame.swapchainStage`.
- See the [README](../../README.md) for instructions on choosing the right batch size/capacity.
All color drawing operations will occupy 2 triangles.

## Drawing operations
TODO CHAP2 Refer to doc comments of Vk2dColorBatch
The color pipeline supports 4 drawing methods: `fill`, `gradient`, `fillUnaligned`, and `gradientUnaligned`.
They are demonstrated below:
![Showcase of the color pipeline](./color.png)

### Filling a rectangle with a flat/plain color
Use `colorBatch.fill(minX, minY, maxX, maxY, color)` to fill the rectangle `(minX, minY, maxX, maxY)`
(all coordinates are **inclusive**) with `color`.
For instance, use `colorBatch.fill(20, 30, 80, 45, srgbToLinear(rgb(80, 23, 71)))` to fill the rectangle between
`(20, 30)` and `(80, 45)` with dark purple.

![example drawing result](../../test-cases/expected/color-pipeline-fill.png)

### Filling a rectangle with a gradient
Use `colorBatch.gradient(minX, minY, maxX, maxY, bottomLeftColor, bottomRightColor, topLeftColor)` to fill the
rectangle `(minX, minY, maxX, maxY)` with a gradient.
- If `bottomLeftColor == bottomRightColor && topLeftColor != bottomLeftColor`, it will be a vertical gradient.
- If `bottomLeftColor != bottomRightColor && topLeftColor == bottomLeftColor`, it will be a horizontal gradient.
