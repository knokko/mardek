package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.IntegerBitser;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class BitClientStructListField extends BitClientField {

	private record NestedListener(Consumer<BitClientStructList> callback, int viewIndex) {}
	private record FlatListener<T>(Consumer<BitClientFlatStructList<T>> callback, int viewIndex) {}

	private boolean finishedInitialization = false;
	private long serverControllerID = -1L;
	private final BitClientStream.Factory streamFactory;
	private final List<NestedListener> nestedListeners = new ArrayList<>();
	private final List<FlatListener<?>> flatListeners = new ArrayList<>();

	private int numNestedViews;

	BitClientStructListField(BitClientStream.Factory streamFactory, BitStructConnectionView view, int fieldID) {
		super(view, fieldID);
		this.streamFactory = streamFactory;
		this.numNestedViews = 0;

		var nestedChildViews = view.getStructListViewsOrNull(fieldID);
		if (nestedChildViews != null) numNestedViews += nestedChildViews.size(); // TODO BITSER Hm... the viewIndex is not always right... maybe every view should get a unique ID, instead of this mess?
	}

	@Override
	void readFromServerInitial(BitInputStream input) throws Throwable {
		if (view.shouldDownloadDuringInitialization(fieldID)) {
			serverControllerID = IntegerBitser.decodeVariableIntegerUsingTerminatorBits(
					0L, Long.MAX_VALUE, input
			);
		}
	}

	private void notifyListeners() {
		var nestedChildViews = view.getStructListViewsOrNull(fieldID);
		for (var listener : nestedListeners) {
			var childStream = streamFactory.createStream(serverControllerID, listener.viewIndex);
			assert nestedChildViews != null;
			listener.callback.accept(new BitClientStructList(
					nestedChildViews.get(listener.viewIndex), childStream, streamFactory
			));
		}

		var flatChildViews = view.getFlatStructListViewsOrNull(fieldID);
		for (var listener : flatListeners) {
			var childStream = streamFactory.createStream(serverControllerID, listener.viewIndex + numNestedViews);
			listener.callback.accept(new BitClientFlatStructList<>(
					flatChildViews.get(listener.viewIndex), childStream
			));
		}
	}

	@Override
	synchronized void postReadFromServerInitial() {
		if (view.shouldDownloadDuringInitialization(fieldID)) notifyListeners();
		finishedInitialization = true;
	}

	@Override
	synchronized void setFromServer(Object newServerValue) {
		long oldControllerID = serverControllerID;
		this.serverControllerID = (Long) newServerValue;
		if (oldControllerID != serverControllerID) notifyListeners();
	}

	@Override
	void save(BitOutputStream output) {
		// Struct list fields cannot be reassigned; only modified
	}

	public synchronized Object subscribeNested(int viewIndex, Consumer<BitClientStructList> updateStructList) {
		var listener = new NestedListener(updateStructList, viewIndex);
		nestedListeners.add(listener);

		if (finishedInitialization) {
			var childViews = Objects.requireNonNull(view.getStructListViewsOrNull(fieldID));
			var childStream = streamFactory.createStream(serverControllerID, viewIndex);
			updateStructList.accept(new BitClientStructList(childViews.get(viewIndex), childStream, streamFactory));
		}

		return listener;
	}

	public synchronized <T> Object subscribeFlat(int viewIndex, Consumer<BitClientFlatStructList<T>> updateStructList) {
		var listener = new FlatListener<>(updateStructList, viewIndex);
		flatListeners.add(listener);

		if (finishedInitialization) {
			var childViews = Objects.requireNonNull(view.getFlatStructListViewsOrNull(fieldID));
			var childStream = streamFactory.createStream(serverControllerID, viewIndex + numNestedViews);
			updateStructList.accept(new BitClientFlatStructList<>(childViews.get(viewIndex), childStream));
		}

		return listener;
	}

	public synchronized void cancelSubscription(Object subscription) {
		//noinspection SuspiciousMethodCalls
		if (!nestedListeners.remove(subscription) && !flatListeners.remove(subscription)) {
			System.err.println("Warning: attempted to cancel missing StructListField subscription " +
					"for field " + fieldID + " of " + view);
		}
	}
}
