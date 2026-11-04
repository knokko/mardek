package com.github.knokko.bitser.connection;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class BitServerOperationRegistry {

	private final Map<BitStructOperation, Supplier<BitServerStructOperation<?>>> structs = new HashMap<>();
	private final Map<BitStructListOperation<?>, Supplier<BitServerStructListOperation<?>>> nestedLists = new HashMap<>();
	private boolean finishedRegistration;

	private void assertRegistrationIsOpen() {
		if (finishedRegistration) throw new IllegalStateException("Registration is already closed");
	}

	public void implement(BitStructOperation operation, Supplier<BitServerStructOperation<?>> implementation) {
		assertRegistrationIsOpen();
		structs.put(operation, implementation);
	}

	public void implement(
			BitStructListOperation<?> operation, Supplier<BitServerStructListOperation<?>> implementation
	) {
		assertRegistrationIsOpen();
		nestedLists.put(operation, implementation);
	}

	public void finishRegistration() {
		assertRegistrationIsOpen();
		finishedRegistration = true;
	}

	private void assertRegistrationIsClosed() {
		if (!finishedRegistration) throw new IllegalStateException("Registration is still open");
	}

	BitServerStructOperation<?> getImplementation(BitStructConnectionView view, int operationID) {
		assertRegistrationIsClosed();
		var operation = new BitStructOperation(view, operationID);
		var operationFactory = structs.get(operation);
		if (operationFactory == null) throw new IllegalArgumentException(operation + " is not implemented");
		return operationFactory.get();
	}

	BitServerStructListOperation<?> getImplementation(BitStructListConnectionView<?> view, int operationID) {
		assertRegistrationIsClosed();
		var operation = new BitStructListOperation<>(view, operationID);
		var operationFactory = nestedLists.get(operation);
		if (operationFactory == null) throw new IllegalArgumentException(operation + " is not implemented");
		return operationFactory.get();
	}
}
