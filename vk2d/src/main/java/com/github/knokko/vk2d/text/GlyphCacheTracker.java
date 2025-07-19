package com.github.knokko.vk2d.text;

import java.util.HashMap;
import java.util.Map;

public class GlyphCacheTracker {

	private final Map<Entry, Integer> stableMap = new HashMap<>();
	private final Map<Entry, Integer> scratchMap = new HashMap<>();

	private final int scratchIntersectionBufferSize, scratchInfoBufferSize;
	private final int stableIntersectionBufferSize, stableInfoBufferSize;
	private int nextStableIndex, nextScratchInfoIndex, nextScratchIntersectionIndex;
	private boolean shouldClearStable;

	public GlyphCacheTracker(
			long scratchIntersectionBufferSize, long scratchInfoBufferSize,
			long stableIntersectionBufferSize, long stableInfoBufferSize
	) {
		this.scratchIntersectionBufferSize = Math.toIntExact(scratchIntersectionBufferSize / 4L);
		this.scratchInfoBufferSize = Math.toIntExact(scratchInfoBufferSize / 4L);
		this.stableIntersectionBufferSize = Math.toIntExact(stableIntersectionBufferSize / 4L);
		this.stableInfoBufferSize = Math.toIntExact(stableInfoBufferSize / 4L);
	}

	public boolean startFrame() {
		nextScratchInfoIndex = 0;
		nextScratchIntersectionIndex = 0;

		int oldStableIndex = nextStableIndex;
		scratchMap.forEach((entry, index) -> {
			stableMap.put(entry, index + oldStableIndex);
			nextStableIndex += entry.height;
		});
		scratchMap.clear();

		if (shouldClearStable) {
			nextStableIndex = 0;
			stableMap.clear();
			shouldClearStable = false;
			return true;
		} else return false;
	}

	public Integer get(int glyph, int height) {
		Integer index = stableMap.get(new Entry(glyph, height));
		if (index != null) return index;
		index = scratchMap.get(new Entry(glyph, height));
		if (index != null) return index + nextStableIndex;
		return null;
	}

	public int putScratch(int glyph, int height, int numCurves) {
		int newScratchInfoIndex = nextScratchInfoIndex + height;
		if (newScratchInfoIndex > scratchInfoBufferSize) return -1;

		int newScratchIntersectionIndex = nextScratchIntersectionIndex + 2 * height * numCurves;
		if (newScratchIntersectionIndex > scratchIntersectionBufferSize) return -1;

		int newStableIndex = nextStableIndex + newScratchInfoIndex;
		if (2 * newStableIndex > stableInfoBufferSize) {
			shouldClearStable = true;
			return -1;
		}

		scratchMap.put(new Entry(glyph, height), nextScratchInfoIndex);
		int result = nextScratchInfoIndex;

		nextScratchInfoIndex = newScratchInfoIndex;
		nextScratchIntersectionIndex = newScratchIntersectionIndex;
		return result;
	}

	public int getNextStableIndex() {
		return nextStableIndex;
	}

	public int getNextScratchIntersectionIndex() {
		return nextScratchIntersectionIndex;
	}

	public int getNextScratchInfoIndex() {
		return nextScratchInfoIndex;
	}

	private record Entry(int glyph, int height) {}
}
