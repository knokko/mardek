package com.github.knokko.bitser;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

class LazyReferenceTargets {

	final String label;
	final ArrayList<Object> idsToUnstable;
	final Map<UUID, Object> stable;

	boolean performedLegacyConversion;

	LazyReferenceTargets(String label, ArrayList<Object> idsToUnstable, Map<UUID, Object> stable) {
		this.label = Objects.requireNonNull(label);
		this.idsToUnstable = Objects.requireNonNull(idsToUnstable);
		this.stable = Objects.requireNonNull(stable);
	}
}
