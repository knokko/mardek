package com.github.knokko.vk2d.batch;

import com.github.knokko.boiler.buffers.MappedVkbBuffer;
import com.github.knokko.vk2d.pipeline.Vk2dPipeline;

import java.nio.ByteBuffer;

/**
 * <p>
 *     Represents contiguous batch of vertex data that can normally be rendered using a single
 *     draw call.
 * </p>
 *
 * <p>
 *     {@code vertexBuffers} and {@code vertexData} are arrays that should have the same length <b>L</b>, where <b>L</b>
 *     is the number of vertex dimensions of the pipeline to which the batch belongs:
 *     {@link Vk2dPipeline#getVertexDimensions()}.
 * </p>
 * @param vertexBuffers For each vertex dimension, this array has the {@link MappedVkbBuffer} where the vertex data
 *                      for that dimension is stored.
 * @param vertexData For each vertex dimension, this array has a {@link ByteBuffer} to which the vertex data of that
 *                   dimension should be written. It is originally obtained using the corresponding
 *                   {@link MappedVkbBuffer#byteBuffer()} (from {@code vertexBuffers}).
 *                   The {@link ByteBuffer#position()} is crucial since we use it to track how many vertices are put
 *                   in the mini batch.
 */
public record MiniBatch(
		MappedVkbBuffer[] vertexBuffers,
		ByteBuffer[] vertexData
) {
}
