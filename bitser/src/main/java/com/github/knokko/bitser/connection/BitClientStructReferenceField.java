package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.IntegerBitser;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

public class BitClientStructReferenceField extends BitClientField {

	private long serverControllerID = -1L;
	private long localControllerID = -1L;
	private BitClientChangeState changeState = BitClientChangeState.UNINITIALIZED;
	private final List<LongConsumer> serverListeners = new ArrayList<>();
	private final List<LongConsumer> localListeners = new ArrayList<>();
	private final List<Consumer<BitClientChangeState>> changeStateListeners = new ArrayList<>();
	private final BitClientStream.Factory streamFactory;

	BitClientStructReferenceField(BitClientStream.Factory streamFactory, BitStructConnectionView view, int fieldID) {
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
		if (view.shouldDownloadDuringInitialization(fieldID)) {
			changeState = BitClientChangeState.UP_TO_DATE;

			for (var listener : serverListeners) listener.accept(serverControllerID);

			for (var listener : localListeners) listener.accept(serverControllerID);

			for (var listener : changeStateListeners) listener.accept(BitClientChangeState.UP_TO_DATE);
		}
	}

	@Override
	synchronized void setFromServer(Object newServerValue) {
		serverControllerID = (Long) newServerValue;
		if (changeState != BitClientChangeState.UP_TO_DATE) {
			if (serverControllerID == localControllerID) {
				changeState = BitClientChangeState.UP_TO_DATE;
				localControllerID = -1L;

				for (var listener : changeStateListeners) listener.accept(BitClientChangeState.UP_TO_DATE);
			}
		}

		for (var listener : serverListeners) listener.accept(serverControllerID);

		if (changeState == BitClientChangeState.UP_TO_DATE) {
			for (var listener : localListeners) listener.accept(serverControllerID);
		}
	}

	@Override
	synchronized void save(BitOutputStream output) throws Throwable {
		if (view.getStructReferenceViewsOrNull(fieldID) == null) return;
		if (!view.canUploadReference(fieldID)) return;

		if (changeState != BitClientChangeState.MODIFIED) {
			output.write(false);
			return;
		}

		output.write(true);
		IntegerBitser.encodeVariableIntegerUsingTerminatorBits(
				localControllerID, 0L, Long.MAX_VALUE, output
		);

		changeState = BitClientChangeState.SAVING;

		for (var listener : changeStateListeners) listener.accept(BitClientChangeState.SAVING);
	}

	public synchronized Object subscribe(boolean considerLocalValue, LongConsumer updateReference) {
		if (considerLocalValue) localListeners.add(updateReference);
		else serverListeners.add(updateReference);

		if (changeState != BitClientChangeState.UNINITIALIZED) {
			if (considerLocalValue && changeState != BitClientChangeState.UP_TO_DATE) {
				updateReference.accept(localControllerID);
			} else {
				updateReference.accept(serverControllerID);
			}
		}

		return updateReference;
	}

	public BitClientStruct createConnectionToReference(long controllerID, int viewID) {
		var childView = view.getStructReferenceViewsOrNull(fieldID).stream().filter(
				candidate -> candidate.localId == viewID
		).findAny();
		if (childView.isEmpty()) throw new IllegalArgumentException("No local view has local ID " + viewID);
		return new BitClientStruct(
				childView.get(),
				streamFactory.createStream(controllerID, viewID),
				streamFactory,
				controllerID
		);
	}

	public synchronized Object subscribeChangeState(Consumer<BitClientChangeState> updateChangeState) {
		changeStateListeners.add(updateChangeState);
		if (changeState != BitClientChangeState.UNINITIALIZED) updateChangeState.accept(changeState);
		return updateChangeState;
	}

	@SuppressWarnings("SuspiciousMethodCalls")
	public synchronized void cancelSubscription(Object subscription) {
		if (changeStateListeners.remove(subscription)) return;
		if (serverListeners.remove(subscription)) return;
		if (localListeners.remove(subscription)) return;
		System.err.println("Warning: attempted to cancel missing subscription for reference field " + fieldID + " of " + view);
	}

	public synchronized void set(long newControllerID) {
		var oldChangeState = changeState;
		if (serverControllerID == newControllerID) {
			changeState = BitClientChangeState.UP_TO_DATE;
			localControllerID = -1L;
		} else {
			changeState = BitClientChangeState.MODIFIED;
			localControllerID = newControllerID;
		}

		for (var listener : localListeners) listener.accept(newControllerID);

		if (changeState != oldChangeState) {
			for (var listener : changeStateListeners) listener.accept(changeState);
		}
	}
}
