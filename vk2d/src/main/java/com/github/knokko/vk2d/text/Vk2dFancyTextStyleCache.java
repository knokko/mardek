package com.github.knokko.vk2d.text;

import com.github.knokko.boiler.buffers.PerFrameBuffer;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static com.github.knokko.vk2d.text.HarfbuzzChecks.assertHbSuccess;
import static org.lwjgl.util.harfbuzz.HarfBuzz.hb_buffer_create;
import static org.lwjgl.util.harfbuzz.HarfBuzz.hb_buffer_destroy;

public class Vk2dFancyTextStyleCache {

	private final PerFrameBuffer perFrameBuffer;
	public final long perFrameDescriptorSet;
	private final Map<Vk2dFancyTextStyle, Integer> cache = new HashMap<>();
	public final long hbBuffer = assertHbSuccess(hb_buffer_create(), "buffer_create");

	public Vk2dFancyTextStyleCache(PerFrameBuffer perFrameBuffer, long perFrameDescriptorSet) {
		this.perFrameBuffer = perFrameBuffer;
		this.perFrameDescriptorSet = perFrameDescriptorSet;
	}

	private void putGradient(ByteBuffer styleData, Vk2dFancyTextStyle.Gradient gradient) {
		styleData.putInt(gradient.color0());
		styleData.putInt(gradient.color1());
		styleData.putInt(gradient.color2());
		styleData.putInt(gradient.color3());
		styleData.putFloat(gradient.threshold0());
		styleData.putFloat(gradient.threshold1());
		styleData.putFloat(gradient.threshold2());
		styleData.putFloat(gradient.threshold3());
	}

	public int getStyleIndex(Vk2dFancyTextStyle style) {
		return cache.computeIfAbsent(style, key -> {
			var styleBuffer = perFrameBuffer.allocate(128, 128);
			var styleData = styleBuffer.byteBuffer();
			putGradient(styleData, style.fillColor());
			putGradient(styleData, style.innerStrokeColor());
			putGradient(styleData, style.outerStrokeColor());
			styleData.putInt(style.fillColor().baseColor());
			styleData.putInt(style.innerStrokeColor().baseColor());
			styleData.putInt(style.outerStrokeColor().baseColor());
			styleData.putInt(style.strokeBehindFill() ? 1 : 0);
			styleData.putFloat(style.fillDistanceFactor());
			styleData.putFloat(style.fillDistanceBias());
			styleData.putFloat(0f);
			styleData.putFloat(0f);
			return Math.toIntExact((styleBuffer.offset - perFrameBuffer.buffer.offset) / 128);
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
