package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BitClientSimpleFlatField<T> extends BitClientField {

	private T serverValue;
	private T localValue;
	private BitClientChangeState changeState = BitClientChangeState.UNINITIALIZED;
	private final List<Consumer<T>> serverListeners = new ArrayList<>();
	private final List<Consumer<T>> localListeners = new ArrayList<>();
	private final List<Consumer<BitClientChangeState>> changeStateListeners = new ArrayList<>();

	BitClientSimpleFlatField(BitStructConnectionView view, int fieldID) {
		super(view, fieldID);
	}

	@Override
	void readFromServerInitial(BitInputStream input) throws Throwable {
		if (view.shouldDownloadDuringInitialization(fieldID)) {
			//noinspection unchecked
			serverValue = (T) view.protocol.deserializeFlatFieldValue(fieldID, input);
		}
	}

	@Override
	synchronized void postReadFromServerInitial() {
		if (view.shouldDownloadDuringInitialization(fieldID)) {
			changeState = BitClientChangeState.UP_TO_DATE;

			for (var listener : serverListeners) listener.accept(serverValue);

			for (var listener : localListeners) listener.accept(serverValue);

			for (var listener : changeStateListeners) listener.accept(BitClientChangeState.UP_TO_DATE);
		}
	}

	@Override
	synchronized void setFromServer(Object newServerValue) {
		//noinspection unchecked
		serverValue = (T) newServerValue;
		if (changeState != BitClientChangeState.UP_TO_DATE) {
			if (view.protocol.areFlatValuesEqual(fieldID, localValue, newServerValue)) {
				changeState = BitClientChangeState.UP_TO_DATE;
				localValue = null;

				for (var listener : changeStateListeners) listener.accept(BitClientChangeState.UP_TO_DATE);
			}
		}

		for (var listener : serverListeners) listener.accept(serverValue);

		if (changeState == BitClientChangeState.UP_TO_DATE) {
			for (var listener : localListeners) listener.accept(serverValue);
		}
	}

	public synchronized Object subscribe(boolean considerLocalValue, Consumer<T> updateValue) {
		if (considerLocalValue) localListeners.add(updateValue);
		else serverListeners.add(updateValue);

		if (changeState != BitClientChangeState.UNINITIALIZED) {
			if (considerLocalValue && changeState != BitClientChangeState.UP_TO_DATE) {
				updateValue.accept(localValue);
			} else {
				updateValue.accept(serverValue);
			}
		}

		return updateValue;
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
		System.err.println("Warning: attempted to cancel missing subscription for field " + fieldID + " of " + view);
	}

	public synchronized void set(T newValue) {
		var oldChangeState = changeState;
		if (view.protocol.areFlatValuesEqual(fieldID, serverValue, newValue)) {
			changeState = BitClientChangeState.UP_TO_DATE;
			localValue = null;
		} else {
			changeState = BitClientChangeState.MODIFIED;
			localValue = newValue;
		}

		for (var listener : localListeners) listener.accept(newValue);

		if (changeState != oldChangeState) {
			for (var listener : changeStateListeners) listener.accept(changeState);
		}
	}

	@Override
	synchronized void save(BitOutputStream output) throws Throwable {
		if (!view.canUploadFlat(fieldID)) return;
		if (changeState != BitClientChangeState.MODIFIED) {
			output.write(false);
			return;
		}

		output.write(true);
		view.protocol.serializeFlatFieldValue(fieldID, output, localValue);

		changeState = BitClientChangeState.SAVING;

		for (var listener : changeStateListeners) listener.accept(BitClientChangeState.SAVING);
	}
}
