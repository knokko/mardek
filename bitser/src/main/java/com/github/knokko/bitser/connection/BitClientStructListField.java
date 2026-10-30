package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.IntegerBitser;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class BitClientStructListField extends BitClientField {

	private record NestedListener(Consumer<BitClientStructList> callback, int viewID) {}
	private record FlatListener<T>(Consumer<BitClientFlatStructList<T>> callback, int viewID) {}

	private boolean finishedInitialization = false;
	private long serverControllerID = -1L;
	private final BitClientStream.Factory streamFactory;
	private final List<NestedListener> nestedListeners = new ArrayList<>();
	private final List<FlatListener<?>> flatListeners = new ArrayList<>();

	BitClientStructListField(BitClientStream.Factory streamFactory, BitStructConnectionView view, int fieldID) {
		super(view, fieldID);
		this.streamFactory = streamFactory;
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
			var childStream = streamFactory.createStream(serverControllerID, listener.viewID);
			assert nestedChildViews != null;

			var nestedView = nestedChildViews.stream().filter(
					candidate -> candidate.localId == listener.viewID
			).findAny();
			if (nestedView.isEmpty()) throw new IllegalArgumentException("No nested view has ID " + listener.viewID);
			listener.callback.accept(new BitClientStructList(nestedView.get(), childStream, streamFactory));
		}

		var flatChildViews = view.getFlatStructListViewsOrNull(fieldID);
		for (var listener : flatListeners) {
			var flatView = flatChildViews.stream().filter(
					candidate -> candidate.localId == listener.viewID
			).findAny();
			if (flatView.isEmpty()) throw new IllegalArgumentException("No flat view has ID " + listener.viewID);
			var childStream = streamFactory.createStream(serverControllerID, listener.viewID);
			listener.callback.accept(new BitClientFlatStructList<>(flatView.get(), childStream));
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

	public synchronized Object subscribeNested(int viewID, Consumer<BitClientStructList> updateStructList) {
		var listener = new NestedListener(updateStructList, viewID);
		nestedListeners.add(listener);

		if (finishedInitialization) {
			var childViews = Objects.requireNonNull(view.getStructListViewsOrNull(fieldID));
			var childStream = streamFactory.createStream(serverControllerID, viewID);
			var childView = childViews.stream().filter(
					candidate -> candidate.localId == viewID
			).findAny();
			if (childView.isEmpty()) throw new IllegalArgumentException("No nested view has local ID " + viewID);
			updateStructList.accept(new BitClientStructList(childView.get(), childStream, streamFactory));
		}

		return listener;
	}

	public synchronized <T> Object subscribeFlat(int viewID, Consumer<BitClientFlatStructList<T>> updateStructList) {
		var listener = new FlatListener<>(updateStructList, viewID);
		flatListeners.add(listener);

		if (finishedInitialization) {
			var childViews = Objects.requireNonNull(view.getFlatStructListViewsOrNull(fieldID));
			var childView = childViews.stream().filter(
					candidate -> candidate.localId == viewID
			).findAny();
			if (childView.isEmpty()) throw new IllegalArgumentException("No flat child view has local ID " + viewID);
			var childStream = streamFactory.createStream(serverControllerID, viewID);
			updateStructList.accept(new BitClientFlatStructList<>(childView.get(), childStream));
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
