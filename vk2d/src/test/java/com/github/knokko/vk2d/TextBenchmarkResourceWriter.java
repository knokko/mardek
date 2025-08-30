package com.github.knokko.vk2d;

import com.github.knokko.vk2d.resource.Vk2dResourceWriter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Objects;

public class TextBenchmarkResourceWriter {

	private static final File TEXT_RESOURCE_FILE = new File(
			"vk2d/src/main/resources/com/github/knokko/vk2d/text-benchmark-resources.bin"
	);

	public static void main(String[] args) throws IOException {
		InputStream fontInput = Objects.requireNonNull(TextBenchmarkResourceWriter.class.getResourceAsStream(
				"fonts/thaana.ttf"
		));

		Vk2dResourceWriter writer = new Vk2dResourceWriter();
		writer.addFont(fontInput);

		File fontsFolder = new File("importer/src/main/resources/mardek/importer/fonts");
		for (File fontFile : Objects.requireNonNull(fontsFolder.listFiles())) {
			writer.addFont(Files.newInputStream(fontFile.toPath()));
		}

		OutputStream output = Files.newOutputStream(TEXT_RESOURCE_FILE.toPath());
		writer.write(output, null);
		output.close();
	}
}
