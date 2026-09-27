package com.github.knokko.bitser.connection;

public class StructListConnectionView {

	public enum Operation {

		Add,
		Swap,
		Replace,
		Remove
	}

	public final StructConnectionView elementsView;
	private final boolean[] allowedOperationsMask = new boolean[Operation.values().length];

	private Operation[] allowedOperations;

	public StructListConnectionView(StructConnectionView elementsView) {
		this.elementsView = elementsView;
	}

	private void assertRegistrationIsOpen() {
		if (allowedOperations != null) throw new IllegalStateException("Registration is already closed");
	}

	public void allowOperation(Operation operation) {
		allowedOperationsMask[operation.ordinal()] = true;
	}

	public void finishRegistration() {
		assertRegistrationIsOpen();

		int operationIndex = 0;
		for (var maybe : allowedOperationsMask) {
			if (maybe) operationIndex += 1;
		}

		allowedOperations = new Operation[operationIndex];
		operationIndex = 0;
		for (int enumIndex = 0; enumIndex < allowedOperationsMask.length; enumIndex++) {
			if (allowedOperationsMask[enumIndex]) {
				allowedOperations[operationIndex] = Operation.values()[enumIndex];
				operationIndex += 1;
			}
		}
	}

	private void assertRegistrationIsClosed() {
		if (allowedOperations == null) throw new IllegalStateException("Registration is still open");
	}

	public int getAllowedOperationIndex(Operation operation) {
		assertRegistrationIsClosed();

		for (int index = 0; index < allowedOperations.length; index++) {
			if (operation == allowedOperations[index]) return index;
		}

		throw new UnsupportedOperationException(operation + " is not allowed");
	}

	public Operation getAllowedOperation(int index) {
		return allowedOperations[index];
	}

	public int getNumAllowedOperations() {
		return allowedOperations.length;
	}
}
