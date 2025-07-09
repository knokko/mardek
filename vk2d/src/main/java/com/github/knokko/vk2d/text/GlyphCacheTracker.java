package com.github.knokko.vk2d.text;

import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

public class GlyphCacheTracker {

	private final Map<Entry, Integer> stableMap = new HashMap<>();
	private final Map<Entry, Integer> scratchMap = new HashMap<>();

	private final int scratchIntersectionBufferSize, scratchInfoBufferSize;
	private final int stableIntersectionBufferSize, stableInfoBufferSize;
	private int nextStableInfoIndex, nextScratchInfoIndex, nextScratchIntersectionIndex;
	private boolean shouldClearStable;

	private IntBuffer stableIntersectionIndices;

	public GlyphCacheTracker(
			long scratchIntersectionBufferSize, long scratchInfoBufferSize,
			long stableIntersectionBufferSize, long stableInfoBufferSize
	) {
		this.scratchIntersectionBufferSize = Math.toIntExact(scratchIntersectionBufferSize / 4L);
		this.scratchInfoBufferSize = Math.toIntExact(scratchInfoBufferSize / 4L);
		this.stableIntersectionBufferSize = Math.toIntExact(stableIntersectionBufferSize / 4L);
		this.stableInfoBufferSize = Math.toIntExact(stableInfoBufferSize / 4L);
	}

	public void setIntersectionIndexBuffer(IntBuffer stableIntersectionIndices) {
		if (this.stableIntersectionIndices != null) throw new IllegalStateException();
		this.stableIntersectionIndices = stableIntersectionIndices;
	}

	public boolean startFrame() {
		nextScratchInfoIndex = 0;
		nextScratchIntersectionIndex = 0;

		if (!stableIntersectionIndices.hasRemaining()) stableIntersectionIndices.position(0);
		int lastStableIntersectionIndex = stableIntersectionIndices.get();
		if (lastStableIntersectionIndex > stableIntersectionBufferSize) throw new IllegalStateException(
				"last stable index is " + lastStableIntersectionIndex + ", but size is only " + stableIntersectionBufferSize
		);
		if (lastStableIntersectionIndex < 0) {
			throw new IllegalStateException("last stable index is " + lastStableIntersectionIndex);
		}
		if (lastStableIntersectionIndex == stableIntersectionBufferSize) shouldClearStable = true;

		int oldStableIndex = nextStableInfoIndex;
		scratchMap.forEach((entry, scratchIndex) -> {
			stableMap.put(entry, 3 * scratchIndex / 2 + oldStableIndex);
			nextStableInfoIndex += 3 * entry.intSize;
		});
		scratchMap.clear();

		if (shouldClearStable) {
			nextStableInfoIndex = 0;
			stableMap.clear();
			shouldClearStable = false;
			System.out.println("Flush intersection cache: intersection index is " +
					lastStableIntersectionIndex + " / " + stableIntersectionBufferSize);
			return true;
		} else return false;
	}

	public Integer get(
			int fontIndex, int glyph, float offset,
			float size, int intSize, boolean horizontal, float maxOrthogonalDistance
	) {
		Entry key = new Entry(fontIndex, glyph, offset, size, intSize, horizontal, maxOrthogonalDistance);
		Integer index = stableMap.get(key);
		if (index != null) return index;
		index = scratchMap.get(key);
		if (index != null) return 3 * index / 2 + nextStableInfoIndex;
		return null;
	}

	public int putScratch(
			int fontIndex, int glyph, float offset, float size, int intSize,
			int numCurves, boolean horizontal, float maxOrthogonalDistance
	) {
		int newScratchInfoIndex = nextScratchInfoIndex + 2 * intSize;
		if (newScratchInfoIndex > scratchInfoBufferSize) return -1;

		int newScratchIntersectionIndex = nextScratchIntersectionIndex + 4 * intSize * numCurves;
		if (newScratchIntersectionIndex > scratchIntersectionBufferSize) return -1;

		int newStableInfoIndex = nextStableInfoIndex + 3 * newScratchInfoIndex / 2;
		if (newStableInfoIndex > stableInfoBufferSize) {
			shouldClearStable = true;
			return -1;
		}

		scratchMap.put(new Entry(fontIndex, glyph, offset, size, intSize, horizontal, maxOrthogonalDistance), nextScratchInfoIndex);
		int result = nextScratchInfoIndex;

		nextScratchInfoIndex = newScratchInfoIndex;
		nextScratchIntersectionIndex = newScratchIntersectionIndex;
		return result;
	}

	public int getNextStableInfoIndex() {
		return nextStableInfoIndex;
	}

	public int getNextScratchIntersectionIndex() {
		return nextScratchIntersectionIndex;
	}

	public int getNextScratchInfoIndex() {
		return nextScratchInfoIndex;
	}

	public int getCurrentFrameInFlight() {
		if (stableIntersectionIndices.position() == 0) return stableIntersectionIndices.capacity() - 1;
		else return stableIntersectionIndices.position() - 1;
	}

	// TODO Use fixed-point offset & size?
	private record Entry(
			int fontIndex, int glyph, float offset,
			float size, int intSize, boolean horizontal, float maxOrthogonalDistance
	) {}
}
