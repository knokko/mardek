package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class StructListConnectionView<T> {

	public final StructConnectionView elementsView;
	private final List<Supplier<Operation<T>>> operations = new ArrayList<>();

	private boolean closedRegistration;

	public StructListConnectionView(StructConnectionView elementsView) {
		this.elementsView = elementsView;
	}

	private void assertRegistrationIsOpen() {
		if (closedRegistration) throw new IllegalStateException("Registration is already closed");
	}

	public int addOperation(Supplier<Operation<T>> operation) {
		assertRegistrationIsOpen();

		int index = operations.size();
		operations.add(operation);
		return index;
	}

	public int addSimpleOperation(Function<List<T>, Boolean> operation) {
		var simpleOperation = new SimpleOperation<>(operation);
		return addOperation(() -> simpleOperation);
	}

	public void finishRegistration() {
		assertRegistrationIsOpen();
		closedRegistration = true;
	}

	private void assertRegistrationIsClosed() {
		if (!closedRegistration) throw new IllegalStateException("Registration is still open");
	}

	public int getNumOperations() {
		assertRegistrationIsClosed();
		return operations.size();
	}

	public Operation<T> getOperation(int index) {
		assertRegistrationIsClosed();
		return operations.get(index).get();
	}

	public static abstract class Operation<T> {

		public abstract void readFromClient(BitInputStream fromClient) throws Throwable;

		public abstract boolean execute(List<T> structList);

		public abstract void respondToClient(BitOutputStream toClient) throws Throwable;
	}

	private static class SimpleOperation<T> extends Operation<T> {

		private final Function<List<T>, Boolean> operation;

		SimpleOperation(Function<List<T>, Boolean> operation) {
			this.operation = operation;
		}

		@Override
		public void readFromClient(BitInputStream fromClient) {}

		@Override
		public boolean execute(List<T> structList) {
			return operation.apply(structList);
		}

		@Override
		public void respondToClient(BitOutputStream toClient) {}
	}
}
