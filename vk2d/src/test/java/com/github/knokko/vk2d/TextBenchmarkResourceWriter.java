package com.github.knokko.vk2d;

import com.github.knokko.vk2d.resource.Vk2dResourceWriter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Objects;

public class TextBenchmarkResourceWriter {

	public static final File TEXT_RESOURCE_FILE = new File("text-benchmark-resources.bin");

	public static void main(String[] args) throws IOException {
		InputStream fontInput = Objects.requireNonNull(TextPlayground.class.getClassLoader().getResourceAsStream(
				"com/github/knokko/vk2d/fonts/thaana.ttf"
		));
		InputStream font2 = Files.newInputStream(new File("importer/src/main/resources/mardek/importer/fonts/274_Nyala.ttf").toPath());

		Vk2dResourceWriter writer = new Vk2dResourceWriter();
		writer.addFont(fontInput);
		writer.addFont(font2);

		OutputStream output = Files.newOutputStream(TEXT_RESOURCE_FILE.toPath());
		writer.write(output);
		output.close();
	}
}
