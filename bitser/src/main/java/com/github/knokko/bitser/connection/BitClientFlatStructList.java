package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.IntegerBitser;
import com.github.knokko.bitser.io.BitInputStream;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BiConsumer;

public class BitClientFlatStructList<T> {

	private final BitFlatStructListConnectionView<?> view;
	private final BitClientStream stream;

	private T[] elements = null;
	private long[] controllerIDs = null;
	private boolean finishedInitialization;
	private final List<BiConsumer<T[], long[]>> listeners = new ArrayList<>();

	public BitClientFlatStructList(BitFlatStructListConnectionView<?> view, BitClientStream stream) {
		this.view = view;
		this.stream = stream;
	}

	public void start() {
		stream.start(this::processInput);
	}

	private void refreshListFromServer(BitInputStream fromServer) throws Throwable {
		var protocol = view.elementsView.protocol;

		int length = IntegerBitser.decodeUnknownLength(null, "BitClient.StructList", fromServer);
		Object[] newElements = new Object[length];
		long[] newControllerIDs = new long[length];

		for (int index = 0; index < length; index++) {
			newControllerIDs[index] = IntegerBitser.decodeVariableIntegerUsingTerminatorBits(
					0L, Long.MAX_VALUE, fromServer
			);
			var fieldValues = new HashMap<Field, Object>();
			for (int fieldID = 0; fieldID < protocol.getNumFields(); fieldID++) {
				if (view.elementsView.shouldDownloadSimpleField(fieldID)) {
					var fieldValue = protocol.deserializeFlatFieldValue(fieldID, fromServer);
					var field = protocol.getFieldByID(fieldID);
					fieldValues.put(field, fieldValue);
				}

				var childStructView = view.elementsView.getChildStructOrNull(fieldID);
				if (childStructView != null) {
					throw new UnsupportedOperationException("TODO BITSER");
				}
			}

			var structInstance = protocol.createPartialInstance.createPartialInstance(fieldValues);
			newElements[index] = structInstance;
		}

		fromServer.discardCurrentByte();

		synchronized (listeners) {
			finishedInitialization = true;
			//noinspection unchecked
			this.elements = (T[]) newElements;
			this.controllerIDs = newControllerIDs;

			for (var listener : listeners) {
				listener.accept(Arrays.copyOf(elements, elements.length), Arrays.copyOf(controllerIDs, controllerIDs.length));
			}
		}
	}

	private void processInput(BitInputStream fromServer) throws Throwable {
		//noinspection InfiniteLoopStatement
		while (true) {
			refreshListFromServer(fromServer);
		}
	}

	public Object subscribe(BiConsumer<T[], long[]> callback) {
		synchronized (listeners) {
			if (finishedInitialization) {
				callback.accept(
						Arrays.copyOf(elements, elements.length),
						Arrays.copyOf(controllerIDs, controllerIDs.length)
				);
			}
			listeners.add(callback);
		}

		return callback;
	}

	public void cancelSubscription(Object subscription) {
		synchronized (listeners) {
			//noinspection SuspiciousMethodCalls
			if (!listeners.remove(subscription)) {
				System.err.println("Attempted to remove missing subscription in BitClient.StructList");
			}
		}
	}

	public void close() {
		stream.close();
	}
}
