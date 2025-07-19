package com.github.knokko.vk2d.text;

import java.util.HashMap;
import java.util.Map;

public class GlyphCacheTracker {

	private final Map<Entry, Integer> stableMap = new HashMap<>();
	private final Map<Entry, Integer> scratchMap = new HashMap<>();

	private int nextStableIndex, nextScratchIndex;

	public void startFrame() {
		boolean clear = false; // TODO Figure out when to clear
		nextScratchIndex = 0;

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

	public int putScratch(int glyph, int height) {
		scratchMap.put(new Entry(glyph, height), nextScratchIndex);
		int result = nextScratchIndex;
		nextScratchIndex += height;
		return result;
	}

	public int getNextStableIndex() {
		return nextStableIndex;
	}

	public int getNextScratchIndex() {
		return nextScratchIndex;
	}

	private record Entry(int glyph, int height) {}
}
