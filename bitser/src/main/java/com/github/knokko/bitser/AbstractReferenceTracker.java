package com.github.knokko.bitser;

import java.util.ArrayList;

abstract class AbstractReferenceTracker {

	final BitserCache cache;

	AbstractReferenceTracker(BitserCache cache) {
		this.cache = cache;
	}

	final ArrayList<WithStructJob> structJobs = new ArrayList<>();
	final ArrayList<WithArrayJob> arrayJobs = new ArrayList<>();

	abstract void registerTarget(String label, Object target);

	abstract LazyReferenceTargets getLazyTargets(String label);
}
