package com.github.knokko.bitser;

record DeepCompareReferenceJob(Object referenceA, Object referenceB) {

	boolean notEqual(DeepComparator comparator) {
		var mappedA = comparator.referenceTargetMapping.get(referenceA);
		if (mappedA == null) return referenceA != referenceB;
		else return mappedA != referenceB;
	}
}
