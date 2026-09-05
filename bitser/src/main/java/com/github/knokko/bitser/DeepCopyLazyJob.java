package com.github.knokko.bitser;

record DeepCopyLazyJob(
		ReferenceLazyBits<?> original, ReferenceLazyBits<Object> copyDestination,
		BitStructWrapper<?> valuesWrapper, String[] labels, RecursionNode node
) {

	void copy(DeepCopyMachine machine) {
		var innerMachine = new DeepCopyMachine(original.get(), machine.bitser, machine.withParameters);
		machine.references.ownedReferences.forEach(innerMachine.references::register);
		innerMachine.run();
		copyDestination.value = innerMachine.destinationRoot;
	}
}
