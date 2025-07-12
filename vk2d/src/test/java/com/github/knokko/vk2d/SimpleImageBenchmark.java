package com.github.knokko.vk2d;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.buffers.MappedVkbBuffer;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.commands.SingleTimeCommands;
import com.github.knokko.boiler.images.ImageBuilder;
import com.github.knokko.boiler.images.VkbImage;
import com.github.knokko.boiler.memory.MemoryCombiner;
import com.github.knokko.boiler.synchronization.ResourceUsage;
import com.github.knokko.boiler.window.AcquiredImage;
import com.github.knokko.boiler.window.VkbWindow;
import com.github.knokko.vk2d.batch.Vk2dImageBatch;
import com.github.knokko.vk2d.pipeline.Vk2dImagePipeline;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static java.lang.Math.min;
import static org.lwjgl.vulkan.VK10.*;

public class SimpleImageBenchmark extends Vk2dWindow {

	private Vk2dImagePipeline imagePipeline;
	private final List<BufferedImage> bufferedImages = new ArrayList<>();
	private VkbImage[] images;
	private long[] imageDescriptors;
	private MappedVkbBuffer[] stagingBuffers;

	private long referenceTime = System.nanoTime();
	private int fps = 0;

	public SimpleImageBenchmark(VkbWindow window) {
		super(window, false);
	}

	private boolean hasContent(BufferedImage image) {
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				if (image.getRGB(x, y) != 0) return true;
			}
		}
		return false;
	}

	@Override
	protected void createResources(
			BoilerInstance boiler, Vk2dDescriptors descriptors,
			MemoryCombiner combiner, MemoryCombiner stagingCombiner
	) {
		super.createResources(boiler, descriptors, combiner, stagingCombiner);
		this.imagePipeline = new Vk2dImagePipeline(pipelineContext, descriptors);

		try {
			BufferedImage weaponSheet = ImageIO.read(Objects.requireNonNull(
					SimpleImageBenchmark.class.getResource("images/weapons.png")
			));
			for (int y = 0; y < weaponSheet.getHeight(); y += 16) {
				for (int x = 0; x < weaponSheet.getWidth(); x += 16) {
					BufferedImage slice = weaponSheet.getSubimage(x, y, 16, 16);
					if (hasContent(slice)) bufferedImages.add(slice);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		this.stagingBuffers = new MappedVkbBuffer[bufferedImages.size()];
		this.images = new VkbImage[bufferedImages.size()];
		for (int index = 0; index < stagingBuffers.length; index++) {
			BufferedImage image = bufferedImages.get(index);
			this.stagingBuffers[index] = stagingCombiner.addMappedBuffer(
					4L * image.getWidth() * image.getHeight(),
					4L, VK_BUFFER_USAGE_TRANSFER_SRC_BIT
			);
			this.images[index] = combiner.addImage(new ImageBuilder(
					"Image" + index, image.getWidth(), image.getHeight()
			).texture(), 0.5f);
		}
		this.imageDescriptors = descriptors.addImageArray(images, true);
	}

	@Override
	protected void performStagingCopies(BoilerInstance boiler) {
		for (int index = 0; index < stagingBuffers.length; index++) {
			stagingBuffers[index].encodeBufferedImage((BufferedImage) bufferedImages.get(index));
		}
		bufferedImages.clear();

		SingleTimeCommands.submit(boiler, "StagingCopies", recorder -> {
			recorder.bulkTransitionLayout(null, ResourceUsage.TRANSFER_DEST, images);
			recorder.bulkCopyBufferToImage(images, stagingBuffers);
			recorder.bulkTransitionLayout(
					ResourceUsage.TRANSFER_DEST,
					ResourceUsage.shaderRead(VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT),
					images
			);
		}).destroy();
		stagingBuffers = null;
		images = null;
	}

	@Override
	protected void renderFrame(Vk2dFrame frame, CommandRecorder recorder, AcquiredImage swapchainImage, BoilerInstance boiler) {
		long currentTime = System.nanoTime();
		if (currentTime - referenceTime > 1000_000_000L) {
			System.out.println("FPS is " + fps);
			fps = 0;
			referenceTime = currentTime;
		}
		fps += 1;

		Random rng = new Random();
		int numImages = 10_000;
		int minWidth = min(10, swapchainImage.width());
		int minHeight = min(10, swapchainImage.height());

		Vk2dImageBatch batch1 = frame.addBatch(imagePipeline, 6 * numImages);
		for (int counter = 0; counter < numImages; counter++) {
			int minX = rng.nextInt(1 + swapchainImage.width() - minWidth);
			int minY = rng.nextInt(1 + swapchainImage.height() - minHeight);
			int boundWidth = swapchainImage.width() - minX;
			int boundHeight = swapchainImage.height() - minY;
			batch1.simple(
					minX, minY, minX + rng.nextInt(boundWidth), minY + rng.nextInt(boundHeight),
					imageDescriptors[rng.nextInt(imageDescriptors.length)]
			);
		}
	}

	@Override
	protected void cleanUp(BoilerInstance boiler) {
		super.cleanUp(boiler);
		imagePipeline.destroy(boiler);
	}

	public static void main(String[] args) {
		bootstrap("SimpleImageBenchmark", 1, ValidationMode.NONE, SimpleImageBenchmark::new);
	}
}
