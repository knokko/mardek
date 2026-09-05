package com.github.knokko.bitser;

record DeepCompareLazyJob(
		ReferenceLazyBits<?> lazyA, ReferenceLazyBits<?> lazyB,
		BitStructWrapper<?> wrapper, RecursionNode node
) {

	boolean areEqual(DeepComparator comparator) {
		DeepComparator innerComparator = new DeepComparator(lazyA.get(), lazyB.get(), wrapper, comparator.bitser);
		innerComparator.referenceTargetMapping.putAll(comparator.referenceTargetMapping);
		return innerComparator.equals();
	}
}
