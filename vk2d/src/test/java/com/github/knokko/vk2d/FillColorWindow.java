package com.github.knokko.vk2d;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.memory.MemoryCombiner;
import com.github.knokko.boiler.window.AcquiredImage;
import com.github.knokko.boiler.window.VkbWindow;
import com.github.knokko.vk2d.pipeline.Vk2dColorPipeline;

import static com.github.knokko.boiler.utilities.ColorPacker.rgb;

public class FillColorWindow extends Vk2dWindow {

	private Vk2dColorPipeline colorPipeline;

	public FillColorWindow(VkbWindow window) {
		super(window, false);
	}

	@Override
	protected void createResources(MemoryCombiner combiner) {
		super.createResources(combiner);
		this.colorPipeline = new Vk2dColorPipeline(pipelineContext);
	}

	@Override
	protected void renderFrame(Vk2dFrame frame, CommandRecorder recorder, AcquiredImage swapchainImage, BoilerInstance boiler) {
		Vk2dBatch batch1 = frame.addBatch(colorPipeline, 6);
		colorPipeline.fill(batch1, 10, 50, 100, 300, rgb(0, 255, 255));
	}

	@Override
	protected void cleanUp(BoilerInstance boiler) {
		super.cleanUp(boiler);
		colorPipeline.destroy(boiler);
	}

	public static void main(String[] args) {
		bootstrap("FillColorBenchmark", 1, ValidationMode.STRONG, FillColorWindow::new);
	}
}
