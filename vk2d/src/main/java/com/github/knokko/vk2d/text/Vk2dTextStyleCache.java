package com.github.knokko.vk2d.text;

import com.github.knokko.boiler.buffers.PerFrameBuffer;

import java.util.HashMap;
import java.util.Map;

import static com.github.knokko.vk2d.text.HarfbuzzChecks.assertHbSuccess;
import static org.lwjgl.util.harfbuzz.HarfBuzz.hb_buffer_create;
import static org.lwjgl.util.harfbuzz.HarfBuzz.hb_buffer_destroy;

public class Vk2dTextStyleCache {

	private final PerFrameBuffer perFrameBuffer;
	public final long perFrameDescriptorSet;
	private final Map<Vk2dTextStyle, Integer> cache = new HashMap<>();
	public final long hbBuffer = assertHbSuccess(hb_buffer_create(), "buffer_create");

	public Vk2dTextStyleCache(PerFrameBuffer perFrameBuffer, long perFrameDescriptorSet) {
		this.perFrameBuffer = perFrameBuffer;
		this.perFrameDescriptorSet = perFrameDescriptorSet;
	}

	public int getStyleIndex(Vk2dTextStyle style) {
		return cache.computeIfAbsent(style, key -> {
			var styleBuffer = perFrameBuffer.allocate(32, 32);
			var styleData = styleBuffer.byteBuffer();
			styleData.putInt(style.fill().color());
			styleData.putInt(style.stroke().color());
			styleData.putInt(style.stroke().behind() ? 1 : 0);
			styleData.putInt(0);
			styleData.putFloat(style.fill().distanceFactor());
			styleData.putFloat(style.fill().distanceBias());
			styleData.putFloat(style.stroke().width());
			styleData.putFloat(style.stroke().distancePower());
			return Math.toIntExact((styleBuffer.offset - perFrameBuffer.buffer.offset) / 32);
		});
	}

	public void reset() {
		cache.clear();
	}

	public void destroy() {
		cache.clear();
		hb_buffer_destroy(hbBuffer);
	}
}
