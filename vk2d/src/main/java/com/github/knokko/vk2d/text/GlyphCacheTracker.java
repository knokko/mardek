package com.github.knokko.vk2d.text;

import java.util.HashMap;
import java.util.Map;

public class GlyphCacheTracker {

	private final Map<Entry, Integer> stableMap = new HashMap<>();
	private final Map<Entry, Integer> scratchMap = new HashMap<>();

	private final int scratchIntersectionBufferSize, scratchInfoBufferSize;
	private int nextStableIndex, nextScratchInfoIndex, nextScratchIntersectionIndex;

	public GlyphCacheTracker(long scratchIntersectionBufferSize, long scratchInfoBufferSize) {
		this.scratchIntersectionBufferSize = Math.toIntExact(scratchIntersectionBufferSize / 4L);
		this.scratchInfoBufferSize = Math.toIntExact(scratchInfoBufferSize / 4L);
	}

	public void startFrame() {
		boolean clear = false; // TODO Figure out when to clear
		nextScratchInfoIndex = 0;
		nextScratchIntersectionIndex = 0;

		int oldStableIndex = nextStableIndex;
		scratchMap.forEach((entry, index) -> {
			stableMap.put(entry, index + oldStableIndex);
			nextStableIndex += entry.height;
		});
		scratchMap.clear();

		if (clear) {
			nextStableIndex = 0;
			stableMap.clear();
		}
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
