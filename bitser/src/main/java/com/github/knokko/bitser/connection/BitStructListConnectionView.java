package com.github.knokko.bitser.connection;

import java.util.ArrayList;
import java.util.List;

public class BitStructListConnectionView<T> extends BitConnectionView {

	public final BitStructProtocol elementsProtocol;
	private final List<BitStructConnectionView> elementsViews = new ArrayList<>();
	private int numOperations;

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

	public BitStructListOperation<T> addOperation() {
		assertRegistrationIsOpen();

		var operation = new BitStructListOperation<>(this, numOperations);
		numOperations += 1;
		return operation;
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
		return numOperations;
	}

	public List<BitStructConnectionView> getElementsViews() {
		assertRegistrationIsClosed();
		return new ArrayList<>(elementsViews);
	}

	@Override
	public BitServerController initializeController(BitServerContext context, Object targetObject) {
		return new BitServerStructListController<>(context, (List<?>) targetObject, elementsProtocol);
	}
}
