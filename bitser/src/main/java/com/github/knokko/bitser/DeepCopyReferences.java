package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.ReferenceBitserException;

import java.util.IdentityHashMap;
import java.util.Map;

class DeepCopyReferences {

	final Map<Object, Object> ownedReferences = new IdentityHashMap<>();

	void register(Object original, Object target) {
		if (ownedReferences.put(original, target) != null) {
			throw new ReferenceBitserException("Multiple reference targets have identity " + original);
		}
	}

	Object convert(Object reference) {
		var ownReference = ownedReferences.get(reference);
		if (ownReference != null) return ownReference;
		else return reference;
	}
}
