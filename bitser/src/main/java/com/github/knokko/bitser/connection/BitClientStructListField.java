package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.IntegerBitser;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class BitClientStructListField extends BitClientField {

	private record Listener(Consumer<BitClientStructList> callback, int viewIndex) {}

	private long serverControllerID = -1L;
	private final BitClientStream.Factory streamFactory;
	private final List<Listener> listeners = new ArrayList<>();

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

	@Override
	void postReadFromServerInitial() {
		if (view.shouldDownloadDuringInitialization(fieldID)) {
			var childViews = Objects.requireNonNull(view.getStructListViewsOrNull(fieldID));
			for (var listener : listeners) {
				var childStream = streamFactory.createStream(serverControllerID, listener.viewIndex);
				listener.callback.accept(new BitClientStructList(
						childViews.get(listener.viewIndex), childStream, streamFactory
				));
			}
		}
	}

	@Override
	void setFromServer(Object newServerValue) {
		long oldControllerID = serverControllerID;
		this.serverControllerID = (Long) newServerValue;

		if (oldControllerID != serverControllerID) {
			var childViews = Objects.requireNonNull(view.getStructListViewsOrNull(fieldID));
			for (var listener : listeners) {
				var childStream = streamFactory.createStream(serverControllerID, listener.viewIndex);
				listener.callback.accept(new BitClientStructList(
						childViews.get(listener.viewIndex), childStream, streamFactory
				));
			}
		}
	}

	@Override
	void save(BitOutputStream output) {
		// Struct list fields cannot be reassigned; only modified
	}

	public synchronized Object subscribe(Consumer<BitClientStructList> updateStructList, int viewIndex) {
		var listener = new Listener(updateStructList, viewIndex);
		listeners.add(listener);

		if (serverControllerID != -1L) {
			var childViews = Objects.requireNonNull(view.getStructListViewsOrNull(fieldID));
			var childStream = streamFactory.createStream(serverControllerID, viewIndex);
			updateStructList.accept(new BitClientStructList(childViews.get(viewIndex), childStream, streamFactory));
		}

		return listener;
	}

	public synchronized void cancelSubscription(Object subscription) {
		//noinspection SuspiciousMethodCalls
		if (!listeners.remove(subscription)) {
			System.err.println("Warning: attempted to cancel missing StructListField subscription " +
					"for field " + fieldID + " of " + view);
		}
	}
}
