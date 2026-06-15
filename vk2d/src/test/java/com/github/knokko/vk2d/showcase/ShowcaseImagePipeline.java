package com.github.knokko.vk2d.showcase;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.window.AcquiredImage;
import com.github.knokko.boiler.window.VkbWindow;
import com.github.knokko.vk2d.Vk2dConfig;
import com.github.knokko.vk2d.Vk2dValidationMode;
import com.github.knokko.vk2d.Vk2dWindow;
import com.github.knokko.vk2d.frame.Vk2dSwapchainFrame;
import com.github.knokko.vk2d.resource.Vk2dGreyscaleChannel;
import com.github.knokko.vk2d.resource.Vk2dImageCompression;
import com.github.knokko.vk2d.resource.Vk2dResourceWriter;
import com.github.knokko.vk2d.text.TestFontCollection;
import com.github.knokko.vk2d.text.TextAlignment;
import com.github.knokko.vk2d.text.Vk2dTextStyle;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import static com.github.knokko.boiler.utilities.ColorPacker.rgb;

public class ShowcaseImagePipeline extends Vk2dWindow {

	private int pixelatedImage;
	private int smoothImage;
	private int clampedImage;

	private int pixelatedBc1Image;
	private int smoothBc1Image;
	private int clampedBc1Image;

	private int pixelatedBc7Image;
	private int smoothBc7Image;
	private int clampedBc7Image;

	private int pixelatedGreyImage;
	private int smoothGreyImage;
	private int clampedGreyImage;

	private int pixelatedBc4Image;
	private int smoothBc4Image;
	private int clampedBc4Image;

	public ShowcaseImagePipeline(VkbWindow window) {
		super(window, true);
	}

	@Override
	protected InputStream initialResourceBundle() throws IOException {
		var writer = new Vk2dResourceWriter();
		var originalImage = ImageIO.read(Objects.requireNonNull(
				getClass().getClassLoader().getResourceAsStream(
						"com/github/knokko/vk2d/images/showcase/original.png"
				)
		));

		int fontDataThaana = writer.addFontBlob(TestFontCollection.thaanaFont());
		int fontThaana = writer.addFont(fontDataThaana, 0);
		writer.addFallbackAtlas(fontThaana, 8, 20f, 0.1f);

		this.pixelatedImage = writer.addImage(originalImage, Vk2dImageCompression.NONE, true, false);
		this.smoothImage = writer.addImage(originalImage, Vk2dImageCompression.NONE, false, false);
		this.clampedImage = writer.addImage(originalImage, Vk2dImageCompression.NONE, false, true);

		this.pixelatedBc1Image = writer.addImage(originalImage, Vk2dImageCompression.BC1, true, false);
		this.smoothBc1Image = writer.addImage(originalImage, Vk2dImageCompression.BC1, false, false);
		this.clampedBc1Image = writer.addImage(originalImage, Vk2dImageCompression.BC1, false, true);

		this.pixelatedBc7Image = writer.addImage(originalImage, Vk2dImageCompression.BC7, true, false);
		this.smoothBc7Image = writer.addImage(originalImage, Vk2dImageCompression.BC7, false, false);
		this.clampedBc7Image = writer.addImage(originalImage, Vk2dImageCompression.BC7, false, true);

		this.pixelatedGreyImage = writer.addGreyscaleImage(
				originalImage, Vk2dImageCompression.NONE,
				Vk2dGreyscaleChannel.ALPHA, true, false
		);
		this.smoothGreyImage = writer.addGreyscaleImage(
				originalImage, Vk2dImageCompression.NONE,
				Vk2dGreyscaleChannel.ALPHA, false, false
		);
		this.clampedGreyImage = writer.addGreyscaleImage(
				originalImage, Vk2dImageCompression.NONE,
				Vk2dGreyscaleChannel.ALPHA, false, true
		);

		this.pixelatedBc4Image = writer.addGreyscaleImage(
				originalImage, Vk2dImageCompression.BC4,
				Vk2dGreyscaleChannel.ALPHA, true, false
		);
		this.smoothBc4Image = writer.addGreyscaleImage(
				originalImage, Vk2dImageCompression.BC4,
				Vk2dGreyscaleChannel.ALPHA, false, false
		);
		this.clampedBc4Image = writer.addGreyscaleImage(
				originalImage, Vk2dImageCompression.BC4,
				Vk2dGreyscaleChannel.ALPHA, false, true
		);

		// Replay the output
		var output = new ByteArrayOutputStream();
		writer.write(output, null);
		return new ByteArrayInputStream(output.toByteArray());
	}

	@Override
	protected void setupConfig(Vk2dConfig config) {
		config.image = true;
		config.simpleText = true;
		config.color = true;
	}

	@Override
	protected void renderFrame(
			Vk2dSwapchainFrame frame, int frameIndex,
			CommandRecorder recorder, AcquiredImage swapchainImage, BoilerInstance boiler
	) {
		pipelines.color.addBatch(frame.swapchainStage, 2).fill(
				0, 0, swapchainImage.getWidth(), swapchainImage.getHeight(),
				rgb(150, 150, 250)
		);

		var textBatch = pipelines.simpleText.addBatch(frame.swapchainStage, 1000, simpleTextStyleCache);
		var textStyle = new Vk2dTextStyle.FillStyle(rgb(0, 0, 0)).only();
		var font = resources.getFont(0);
		float textHeight = 20f;
		textBatch.drawString(
				"pixelated", 300f, 40f, textHeight, font, textStyle, TextAlignment.LEFT
		);
		textBatch.drawString(
				"smooth", 500f, 40f, textHeight, font, textStyle, TextAlignment.LEFT
		);
		textBatch.drawString(
				"clamped", 700f, 40f, textHeight, font, textStyle, TextAlignment.LEFT
		);

		textBatch.drawString(
				"uncompressed", 20f, 140f, textHeight, font, textStyle, TextAlignment.LEFT
		);
		textBatch.drawString(
				"BC7 compression", 20f, 300f, textHeight, font, textStyle, TextAlignment.LEFT
		);
		textBatch.drawString(
				"BC1 compression", 20f, 460f, textHeight, font, textStyle, TextAlignment.LEFT
		);

		var imageBatch = pipelines.image.addBatch(frame.swapchainStage, 100, resources);
		imageBatch.simpleScale(290f, 70f, 8f, pixelatedImage);
		imageBatch.simpleScale(490f, 70f, 8f, smoothImage);
		imageBatch.simpleScale(690f, 70f, 8f, clampedImage);

		imageBatch.simpleScale(290f, 230f, 8f, pixelatedBc7Image);
		imageBatch.simpleScale(490f, 230f, 8f, smoothBc7Image);
		imageBatch.simpleScale(690f, 230f, 8f, clampedBc7Image);

		// TODO CHAP2 Improve BC1 compression
		imageBatch.simpleScale(290f, 390f, 8f, pixelatedBc1Image);
		imageBatch.simpleScale(490f, 390f, 8f, smoothBc1Image);
		imageBatch.simpleScale(690f, 390f, 8f, clampedBc1Image);

		textBatch.drawString(
				"Greyscale", 300f, 600f, 1.5f * textHeight, font, textStyle, TextAlignment.LEFT
		);
		textBatch.drawString(
				"pixelated", 300f, 660f, textHeight, font, textStyle, TextAlignment.LEFT
		);
		textBatch.drawString(
				"smooth", 500f, 660f, textHeight, font, textStyle, TextAlignment.LEFT
		);
		textBatch.drawString(
				"clamped", 700f, 660f, textHeight, font, textStyle, TextAlignment.LEFT
		);

		imageBatch.simpleScale(290f, 690f, 8f, pixelatedGreyImage);
		imageBatch.simpleScale(490f, 690f, 8f, smoothGreyImage);
		imageBatch.simpleScale(690f, 690f, 8f, clampedGreyImage);

		imageBatch.simpleScale(290f, 850f, 8f, pixelatedBc4Image);
		imageBatch.simpleScale(490f, 850f, 8f, smoothBc4Image);
		imageBatch.simpleScale(690f, 850f, 8f, clampedBc4Image);
	}

	public static void main(String[] args) {
		Vk2dWindow.bootstrap("ShowcaseImagePipeline", 1, Vk2dValidationMode.WEAK, ShowcaseImagePipeline::new);
	}
}
