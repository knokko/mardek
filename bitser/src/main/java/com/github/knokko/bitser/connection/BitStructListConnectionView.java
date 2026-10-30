package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class BitStructListConnectionView<T> extends BitConnectionView {

	public final BitStructProtocol elementsProtocol;
	private final List<BitStructConnectionView> elementsViews = new ArrayList<>();
	private final List<Supplier<Operation<T>>> operations = new ArrayList<>();

	private boolean closedRegistration;

	public BitStructListConnectionView(int viewID, BitStructProtocol elementsProtocol) {
		super(viewID);
		this.elementsProtocol = elementsProtocol;
	}

	private void assertRegistrationIsOpen() {
		if (closedRegistration) throw new IllegalStateException("Registration is already closed");
	}

	public void addElementsView(BitStructConnectionView elementsView) {
		assertRegistrationIsOpen();
		if (elementsViews.stream().anyMatch(otherView -> otherView.localId == elementsView.localId)) {
			throw new IllegalArgumentException("Duplicate local ID " + elementsView.localId);
		}
		this.elementsViews.add(elementsView);
	}

	public int addOperation(Supplier<Operation<T>> operation) {
		assertRegistrationIsOpen();

		int index = operations.size();
		operations.add(operation);
		return index;
	}

	public int addSimpleOperation(Function<List<T>, Boolean> operation) {
		return addOperation(() -> new SimpleOperation<>(operation));
	}

	public <R> int addSimpleGlobalOperation(Consumer<GlobalContext<T, R>> operation) {
		return addOperation(() -> new SimpleGlobalOperation<>(operation));
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

	public List<BitStructConnectionView> getElementsViews() {
		assertRegistrationIsClosed();
		return new ArrayList<>(elementsViews);
	}

	public Operation<T> getOperation(int index) {
		assertRegistrationIsClosed();
		return operations.get(index).get();
	}

	@Override
	public BitServerController initializeController(BitServerContext context, Object targetObject) {
		return new BitServerStructListController<>(context, (List<?>) targetObject, elementsProtocol);
	}

	public static abstract class Operation<T> {

		public abstract void readFromClient(BitInputStream fromClient) throws Throwable;

		public abstract boolean execute(List<T> structList);

		public abstract void respondToClient(BitOutputStream toClient) throws Throwable;
	}

	public interface GlobalOperation<T, R> {

		void considerGlobalState(Supplier<GlobalContext<T, R>> getRootStructAndAcquireGlobalLock);
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

	public record GlobalContext<T, R>(R rootStruct, List<T> thisList, BitServerControllerMapping controllerMapping) {}

	private static class SimpleGlobalOperation<T, R> extends Operation<T> implements GlobalOperation<T, R> {

		private final Consumer<GlobalContext<T, R>> operation;
		private GlobalContext<T, R> context;

		SimpleGlobalOperation(Consumer<GlobalContext<T, R>> operation) {
			this.operation = operation;
		}

		@Override
		public void readFromClient(BitInputStream fromClient) {}

		@Override
		public boolean execute(List<T> structList) {
			operation.accept(context);
			return false;
		}

		@Override
		public void respondToClient(BitOutputStream toClient) {}

		@Override
		public void considerGlobalState(Supplier<GlobalContext<T, R>> getRootStructAndAcquireGlobalLock) {
			this.context = getRootStructAndAcquireGlobalLock.get();
		}
	}
}
