package com.github.knokko.vk2d.resource;

import static org.lwjgl.vulkan.VK10.*;

public enum Vk2dImageCompression {
	NONE(VK_FORMAT_R8G8B8A8_SRGB, 4),
	BC1(VK_FORMAT_BC1_RGBA_SRGB_BLOCK, 8),
	BC4(VK_FORMAT_BC4_UNORM_BLOCK, 8),
	BC7(VK_FORMAT_BC7_SRGB_BLOCK, 16);

	public final int format;
	public final int alignment;

	Vk2dImageCompression(int format, int alignment) {
		this.format = format;
		this.alignment = alignment;
	}
}
