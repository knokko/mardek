package com.github.knokko.vk2d.pipeline;

import com.github.knokko.vk2d.Vk2dConfig;
import com.github.knokko.vk2d.Vk2dInstance;

public class Vk2dPipelines {

	public final Vk2dInstance instance;
	public final Vk2dColorPipeline color;
	public final Vk2dOvalPipeline oval;
	public final Vk2dImagePipeline image;
	public final Vk2dKimPipeline kim1, kim3;
	public final Vk2dGlyphPipeline text;
	public final Vk2dBlurPipeline blur;

	public Vk2dPipelines(Vk2dInstance instance, Vk2dPipelineContext context, Vk2dConfig config) {
		this.instance = instance;
		this.color = config.color ? new Vk2dColorPipeline(context) : null;
		this.oval = config.oval ? new Vk2dOvalPipeline(context, instance) : null;
		this.image = config.image ? new Vk2dImagePipeline(context, instance) : null;
		this.kim1 = config.kim1 ? new Vk2dKimPipeline(context, instance, 1) : null;
		this.kim3 = config.kim3 ? new Vk2dKimPipeline(context, instance, 3) : null;
		this.text = config.text ? new Vk2dGlyphPipeline(context, instance) : null;
		this.blur = config.blur ? new Vk2dBlurPipeline(context, instance) : null;
	}

	public void destroy() {
		Vk2dPipeline[] all = { color, oval, image, kim1, kim3, text, blur };
		for (Vk2dPipeline pipeline : all) {
			if (pipeline != null) pipeline.destroy(instance.boiler);
		}
	}
}
