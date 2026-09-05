package com.github.knokko.bitser;

import java.util.Map;

class PopulateMapJob extends PopulateJob {

	final Map<?, ?> map;
	Object[] keys;
	Object[] values;

	PopulateMapJob(Map<?, ?> map, Object[] keys, Object[] values, RecursionNode node) {
		super(node);
		this.map = map;
		this.keys = keys;
		this.values = values;
	}

	void populate() {
		for (int index = 0; index < keys.length; index++) {
			//noinspection unchecked
			((Map<Object, Object>) map).put(keys[index], values[index]);
		}
	}
}
