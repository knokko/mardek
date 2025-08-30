package com.github.knokko.vk2d;

import com.github.knokko.vk2d.resource.Vk2dFakeImageCompression;
import com.github.knokko.vk2d.resource.Vk2dImageCompression;
import com.github.knokko.vk2d.resource.Vk2dResourceWriter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Objects;

public class ImageBenchmarkResourceWriter {

	static final File FILE = new File("image-benchmark-resources.bin");

	private static boolean hasContent(BufferedImage image) {
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				if (image.getRGB(x, y) != 0) return true;
			}
		}
		return false;
	}

	public static void main(String[] args) throws IOException {
		Vk2dResourceWriter writer = new Vk2dResourceWriter();

		try {
			BufferedImage weaponSheet = ImageIO.read(Objects.requireNonNull(
					ChaosImageBenchmark.class.getResource("images/weapons.png")
			));
			for (int y = 0; y < weaponSheet.getHeight(); y += 16) {
				for (int x = 0; x < weaponSheet.getWidth(); x += 16) {
					BufferedImage slice = weaponSheet.getSubimage(x, y, 16, 16);
					if (hasContent(slice)) {
						writer.addImage(slice, Vk2dImageCompression.NONE, true);
						writer.addFakeImage(slice, Vk2dFakeImageCompression.KIM1);
						writer.addFakeImage(slice, Vk2dFakeImageCompression.KIM3);
					}
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		OutputStream output = Files.newOutputStream(FILE.toPath());
		writer.write(output, null);
		output.close();
	}
}
