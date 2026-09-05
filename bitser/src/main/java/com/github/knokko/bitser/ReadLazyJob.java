package com.github.knokko.bitser;

import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.options.CollectionSizeLimit;

import java.util.ArrayList;

record ReadLazyJob(ReferenceLazyBits<?> lazy, String[] labels, RecursionNode node) {

	void read(BitInputStream input, CollectionSizeLimit sizeLimit, AbstractReferenceTracker references) throws Exception {
		lazy.bytes = SimpleLazyFieldWrapper.readLazyBytes(input, sizeLimit);
		lazy.potentialTargets = new LazyReferenceTargets[labels.length];
		for (int index = 0; index < labels.length; index++) {
			lazy.potentialTargets[index] = references.getLazyTargets(labels[index]);
		}
	}

	void convert(BackDeserializer deserializer) {
		for (var targets : lazy.potentialTargets) {
			if (targets == null || targets.performedLegacyConversion) continue;
			var newUnstable = new ArrayList<>(targets.idsToUnstable.size());
			for (var target : targets.idsToUnstable) {
				newUnstable.add(deserializer.references.getModernFromRaw(target));
			}
			targets.idsToUnstable.clear();
			targets.idsToUnstable.addAll(newUnstable);
			targets.performedLegacyConversion = true;
		}
	}
}
