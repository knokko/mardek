package com.github.knokko.vk2d.pipeline;

import com.github.knokko.vk2d.Vk2dInstance;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static com.github.knokko.boiler.utilities.BoilerMath.leastCommonMultiple;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;

public class Vk2dPipelines {

	public final Vk2dInstance instance;
	public final Vk2dColorPipeline color;
	public final Vk2dMultiplyPipeline multiply;
	public final Vk2dOvalPipeline oval;
	public final Vk2dImagePipeline image;
	public final Vk2dKimPipeline kim1;
	public final Vk2dKim3Pipeline kim3;
	public final Vk2dSimpleTextPipeline simpleText;
	public final Vk2dFancyTextPipeline fancyText;
	public final Vk2dBlurPipeline blur;

	private final List<Field> pipelineFields = new ArrayList<>();
	private final List<Method> pipelineGetters = new ArrayList<>();

	public Vk2dPipelines(Vk2dInstance instance, Vk2dPipelineContext context) {
		this.instance = instance;
		var config = instance.config;
		this.color = config.color ? new Vk2dColorPipeline(context, instance) : null;
		this.multiply = config.multiply ? new Vk2dMultiplyPipeline(context, instance) : null;
		this.oval = config.oval ? new Vk2dOvalPipeline(context, instance) : null;
		this.image = config.image ? new Vk2dImagePipeline(context, instance) : null;
		this.kim1 = config.kim1 ? new Vk2dKimPipeline(context, instance, 1) : null;
		this.kim3 = config.kim3 ? new Vk2dKim3Pipeline(context, instance) : null;
		this.simpleText = config.simpleText ? new Vk2dSimpleTextPipeline(context, instance) : null;
		this.fancyText = config.fancyText ? new Vk2dFancyTextPipeline(context, instance) : null;
		this.blur = config.blur ? new Vk2dBlurPipeline(context, instance) : null;

		for (var field : this.getClass().getFields()) {
			if (!Modifier.isStatic(field.getModifiers()) && Vk2dPipeline.class.isAssignableFrom(field.getType())) {
				pipelineFields.add(field);
			}
		}

		for (var method : this.getClass().getMethods()) {
			if (!Modifier.isStatic(method.getModifiers()) && method.getParameterCount() == 0 &&
					Vk2dPipeline.class.isAssignableFrom(method.getReturnType())
			) {
				pipelineGetters.add(method);
			}
		}
	}

	protected List<Vk2dPipeline> all() {
		var pipelines = new ArrayList<Vk2dPipeline>(pipelineFields.size() + pipelineGetters.size());
		try {
			for (var field : pipelineFields) pipelines.add((Vk2dPipeline) field.get(this));
			for (var getter : pipelineGetters) pipelines.add((Vk2dPipeline) getter.invoke(this));
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
		return pipelines;
	}

	public long perFrameBufferAlignment() {
		long alignment = 1L;
		long storageAlignment = instance.boiler.deviceProperties.limits().minStorageBufferOffsetAlignment();

		for (var pipeline : all()) {
			if (pipeline == null) continue;
			if (pipeline.usesVertexBuffer) alignment = leastCommonMultiple(alignment, 4L);
			if (pipeline.usesStorageBuffer) alignment = leastCommonMultiple(alignment, storageAlignment);
		}

		return alignment;
	}

	public int perFrameBufferUsage() {
		int usage = 0;

		for (var pipeline : all()) {
			if (pipeline == null) continue;
			if (pipeline.usesVertexBuffer) usage |= VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;
			if (pipeline.usesStorageBuffer) usage |= VK_BUFFER_USAGE_STORAGE_BUFFER_BIT;
		}

		return usage;
	}

	public void destroy() {
		for (Vk2dPipeline pipeline : all()) {
			if (pipeline != null) pipeline.destroy(instance.boiler);
		}
	}
}

