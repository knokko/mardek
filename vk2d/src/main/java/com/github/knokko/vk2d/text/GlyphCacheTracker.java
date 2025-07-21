package com.github.knokko.vk2d.text;

import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class GlyphCacheTracker {

	private final Map<Entry, Integer> stableMap = new HashMap<>();
	private final Map<Entry, Integer> scratchMap = new HashMap<>();

	private int lastStableIntersectionIndex;
	private final int scratchIntersectionBufferSize, scratchInfoBufferSize;
	private final int stableIntersectionBufferSize, stableInfoBufferSize;
	private int nextStableInfoIndex, nextScratchInfoIndex, nextScratchIntersectionIndex;
	private int maxInFlightIntersections;
	private boolean shouldClearStable;

	private IntBuffer stableIntersectionIndices;
	private int[] previousScratchIntersectionCounts;

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
		this.previousScratchIntersectionCounts = new int[stableIntersectionIndices.capacity()];
	}

	public boolean startFrame() {
		previousScratchIntersectionCounts[getCurrentFrameInFlight()] = nextScratchIntersectionIndex;

		nextScratchInfoIndex = 0;
		nextScratchIntersectionIndex = 0;

		int frameInFlight = stableIntersectionIndices.position();
		maxInFlightIntersections = 0;
		for (int index = 0; index < previousScratchIntersectionCounts.length; index++) {
			if (index != frameInFlight) maxInFlightIntersections += previousScratchIntersectionCounts[index];
		}
		if (!stableIntersectionIndices.hasRemaining()) stableIntersectionIndices.position(0);
		lastStableIntersectionIndex = stableIntersectionIndices.get();

		int oldStableIndex = nextStableInfoIndex;
		scratchMap.forEach((entry, index) -> {
			stableMap.put(entry, index + oldStableIndex);
			nextStableInfoIndex += 2 * entry.size;
		});
		scratchMap.clear();

		if (shouldClearStable) {
			lastStableIntersectionIndex = 0;
			nextStableInfoIndex = 0;
			stableMap.clear();
			shouldClearStable = false;
			Arrays.fill(previousScratchIntersectionCounts, 0);
			maxInFlightIntersections = 0;
			return true;
		} else return false;
	}

	public Integer get(int fontIndex, int glyph, int size, boolean horizontal) {
		Integer index = stableMap.get(new Entry(fontIndex, glyph, size, horizontal));
		if (index != null) return index;
		index = scratchMap.get(new Entry(fontIndex, glyph, size, horizontal));
		if (index != null) return index + nextStableInfoIndex;
		return null;
	}

	public int putScratch(int fontIndex, int glyph, int size, int numCurves, boolean horizontal) {
		int newScratchInfoIndex = nextScratchInfoIndex + 2 * size;
		if (newScratchInfoIndex > scratchInfoBufferSize) return -1;

		int newScratchIntersectionIndex = nextScratchIntersectionIndex + 2 * size * numCurves;
		if (newScratchIntersectionIndex > scratchIntersectionBufferSize) return -1;

		int newStableInfoIndex = nextStableInfoIndex + newScratchInfoIndex;
		if (newStableInfoIndex > stableInfoBufferSize) {
			shouldClearStable = true;
			return -1;
		}

		int worstCaseStableIntersectionIndex = lastStableIntersectionIndex + maxInFlightIntersections + newScratchIntersectionIndex;
		if (worstCaseStableIntersectionIndex > stableIntersectionBufferSize) {
			if (lastStableIntersectionIndex + 2 * size * numCurves > stableIntersectionBufferSize) {
				shouldClearStable = true;
			}
			return -1;
		}

		scratchMap.put(new Entry(fontIndex, glyph, size, horizontal), nextScratchInfoIndex);
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

	private record Entry(int fontIndex, int glyph, int size, boolean horizontal) {}
}
