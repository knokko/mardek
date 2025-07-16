package com.github.knokko.vk2d;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.descriptors.DescriptorCombiner;
import com.github.knokko.boiler.descriptors.DescriptorUpdater;
import com.github.knokko.boiler.memory.MemoryBlock;
import com.github.knokko.boiler.memory.MemoryCombiner;
import com.github.knokko.boiler.window.AcquiredImage;
import com.github.knokko.boiler.window.VkbWindow;
import com.github.knokko.vk2d.batch.Vk2dGlyphBatch;
import com.github.knokko.vk2d.pipeline.Vk2dGlyphPipeline;
import org.lwjgl.system.MemoryStack;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;

import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.vkDestroyDescriptorPool;

public class GlyphBenchmark extends Vk2dWindow {

	private Vk2dGlyphPipeline textPipeline;
	private long descriptorPool, descriptorSet;

	private long referenceTime = System.nanoTime();
	private int fps = 0;

	private MemoryBlock testMemory;

	public GlyphBenchmark(VkbWindow window) {
		super(window, false);
	}

	@Override
	protected void setup(BoilerInstance boiler, MemoryStack stack) {
		super.setup(boiler, stack);
		this.textPipeline = new Vk2dGlyphPipeline(pipelineContext);

		long alignment = boiler.deviceProperties.limits().minStorageBufferOffsetAlignment();
		var combiner = new MemoryCombiner(boiler, "TestGlyph");
		var infoBuffer = combiner.addMappedBuffer(100_000L, alignment, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT);
		var intersectionBuffer = combiner.addMappedBuffer(100_000L, alignment, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT);
		this.testMemory = combiner.build(false);

		try {
			FloatBuffer intersectionData = intersectionBuffer.floatBuffer();
			File intersectionsFile = new File("intersections.bin");
			int numIntersections = Math.toIntExact(intersectionsFile.length() / 4);
			DataInputStream intersectionsInput = new DataInputStream(Files.newInputStream(intersectionsFile.toPath()));
			for (int counter = 0; counter < numIntersections; counter++) intersectionData.put(intersectionsInput.readFloat());
			intersectionsInput.close();

			IntBuffer infoData = infoBuffer.intBuffer();
			File infoFile = new File("info.bin");
			int numInfo = Math.toIntExact(infoFile.length() / 4);
			DataInputStream infoInput = new DataInputStream(Files.newInputStream(infoFile.toPath()));
			for (int counter = 0; counter < numInfo; counter++) infoData.put(infoInput.readInt());
			infoInput.close();
		} catch (IOException io) {
			throw new RuntimeException(io);
		}

		FloatBuffer intersectionData = intersectionBuffer.floatBuffer();
		for (int counter = 0; counter < 4; counter++) System.out.println(intersectionData.get());
		var descriptors = new DescriptorCombiner(boiler);
		descriptors.addSingle(textPipeline.descriptorSetLayout, descriptorSet -> this.descriptorSet = descriptorSet);
		descriptorPool = descriptors.build("Glyph");

		var updater = new DescriptorUpdater(stack, 2);
		updater.writeStorageBuffer(0, descriptorSet, 0, intersectionBuffer);
		updater.writeStorageBuffer(1, descriptorSet, 1, infoBuffer);
		updater.update(boiler);
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

		Vk2dGlyphBatch batch1 = textPipeline.addBatch(frame, 10_000, descriptorSet);
		int cellSize = 100;
		for (int y = 0; y < swapchainImage.height(); y += cellSize) {
			for (int x = 0; x < swapchainImage.width(); x += cellSize) {
				batch1.simple(x, y, x + cellSize, y + cellSize - 1, 0);
			}
		}
	}

	@Override
	protected void cleanUp(BoilerInstance boiler) {
		super.cleanUp(boiler);
		textPipeline.destroy(boiler);
		testMemory.destroy(boiler);
		vkDestroyDescriptorPool(boiler.vkDevice(), descriptorPool, null);
	}

	public static void main(String[] args) {
		bootstrap("GlyphBenchmark", 1, Vk2dValidationMode.NONE, GlyphBenchmark::new);
	}
}
