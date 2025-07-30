package com.github.knokko.vk2d.batch;

import com.github.knokko.boiler.buffers.MappedVkbBuffer;

import java.nio.ByteBuffer;

public record BatchVertexData(
		MappedVkbBuffer[] vertexBuffers,
		ByteBuffer[] vertexData
) {
}
