package com.github.knokko.vk2d.text;

import com.github.knokko.boiler.builders.BoilerBuilder;
import com.github.knokko.boiler.commands.SingleTimeCommands;
import com.github.knokko.boiler.memory.MemoryCombiner;
import com.github.knokko.boiler.synchronization.ResourceUsage;
import com.github.knokko.vk2d.Vk2dConfig;
import com.github.knokko.vk2d.Vk2dInstance;
import com.github.knokko.vk2d.resource.Vk2dResourceWriter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK12.VK_API_VERSION_1_2;

public class SdfGeneratorPlayground {

	public static void main(String[] args) throws IOException {
		var boiler = new BoilerBuilder(
				VK_API_VERSION_1_2, "TestGlyphs", 1
		).validation().build();

		var config = new Vk2dConfig();
		config.simpleText = true;
		var instance = new Vk2dInstance(boiler, config);

		var writer = new Vk2dResourceWriter();
		int fontDataMyriad = writer.addFontBlob(TestFontCollection.myriadFont());
		int fontMyriad = writer.addFont(fontDataMyriad, 0);
		writer.addFallbackAtlas(fontMyriad, 16, 40f, 0.5f);
		var bundle = writer.directlyCreateBundle(instance, null);

		var font = bundle.getFont(fontMyriad);
		var atlas = font.chooseAtlas(10f, 10f, 0);

		var readbackCombiner = new MemoryCombiner(boiler, "ReadbackMemory");
		var readBackBuffer = readbackCombiner.addMappedBuffer(
				2L * atlas.image.width * atlas.image.height, 4L, VK_BUFFER_USAGE_TRANSFER_DST_BIT
		);
		var readbackMemory = readbackCombiner.build(true);

		SingleTimeCommands.submit(boiler, "CopyAtlas", recorder -> {
			recorder.transitionLayout(
					atlas.image, ResourceUsage.shaderRead(VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT),
					ResourceUsage.TRANSFER_SOURCE
			);
			recorder.copyImageToBuffer(atlas.image, readBackBuffer);
			recorder.bufferBarrier(readBackBuffer, ResourceUsage.TRANSFER_DEST, ResourceUsage.HOST_READ);
		}).destroy();

		var image = new BufferedImage(atlas.image.width, atlas.image.height, BufferedImage.TYPE_INT_ARGB);

		if (atlas.bitsPerPixel == 8) {
			var readBackData = readBackBuffer.byteBuffer();
			for (int y = 0; y < image.getHeight(); y++) {
				for (int x = 0; x < image.getWidth(); x++) {
					var rawValue = readBackData.get();
					var color = new Color(rawValue + 128, rawValue + 128, rawValue + 128);
					image.setRGB(x, y, color.getRGB());
				}
			}
		} else {
			var readBackData = readBackBuffer.shortBuffer();
			for (int y = 0; y < image.getHeight(); y++) {
				for (int x = 0; x < image.getWidth(); x++) {
					int rawValue = readBackData.get();
					float floatValue = (rawValue - Short.MIN_VALUE) / (float) Character.MAX_VALUE;
					var color = new Color(floatValue, floatValue, floatValue);
					image.setRGB(x, y, color.getRGB());
				}
			}
		}

		ImageIO.write(image, "PNG", new File("readback.png"));

		readbackMemory.destroy(boiler);
		bundle.destroy(boiler);
		instance.destroy();
		boiler.destroyInitialObjects();
	}
}
