package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.IntegerBitser;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class BitClientChildStructField extends BitClientField {

	private record Listener(Consumer<BitClientStruct> callback, int viewIndex) {}

	private boolean finishedInitialization = false;
	private long serverControllerID = -1L;
	private final BitClientStream.Factory streamFactory;
	private final List<Listener> listeners = new ArrayList<>();

	BitClientChildStructField(BitClientStream.Factory streamFactory, BitStructConnectionView view, int fieldID) {
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
	synchronized void postReadFromServerInitial() {
		finishedInitialization = true;
		if (view.shouldDownloadDuringInitialization(fieldID)) {
			var childViews = Objects.requireNonNull(view.getChildStructViewsOrNull(fieldID));
			for (var listener : listeners) {
				var childStream = streamFactory.createStream(serverControllerID, listener.viewIndex);
				listener.callback.accept(new BitClientStruct(
						childViews.get(listener.viewIndex),
						childStream,
						streamFactory,
						serverControllerID
				));
			}
		}
	}

	@Override
	synchronized void setFromServer(Object newServerValue) {
		long oldControllerID = serverControllerID;
		this.serverControllerID = (Long) newServerValue;

		if (oldControllerID != serverControllerID) {
			var childViews = Objects.requireNonNull(view.getChildStructViewsOrNull(fieldID));
			for (var listener : listeners) {
				var childStream = streamFactory.createStream(serverControllerID, listener.viewIndex);
				listener.callback.accept(new BitClientStruct(
						childViews.get(listener.viewIndex), childStream, streamFactory, serverControllerID
				));
			}
		}
	}

	@Override
	void save(BitOutputStream output) {
		// ClientStructField's cannot be saved; save their BitClient.Struct instead
	}

	public synchronized Object subscribe(Consumer<BitClientStruct> updateChildStruct, int viewIndex) {
		var listener = new Listener(updateChildStruct, viewIndex);
		listeners.add(listener);

		if (finishedInitialization) {
			var childViews = Objects.requireNonNull(view.getChildStructViewsOrNull(fieldID));
			var childStream = streamFactory.createStream(serverControllerID, viewIndex);
			updateChildStruct.accept(new BitClientStruct(
					childViews.get(viewIndex), childStream, streamFactory, serverControllerID
			));
		}

		return listener;
	}

	public synchronized void cancelSubscription(Object subscription) {
		//noinspection SuspiciousMethodCalls
		if (!listeners.remove(subscription)) {
			System.err.println("Warning: attempted to cancel missing subscription for field " + fieldID + " of " + view);
		}
	}
}
