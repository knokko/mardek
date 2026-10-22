package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.IntegerBitser;
import com.github.knokko.bitser.io.BitInputStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static java.lang.Math.max;

public class BitClientStructList {

	private final BitStructListConnectionView<?> view;
	private final BitClientStream stream;
	private final BitClientStream.Factory streamFactory;

	private long[] controllerIDs = new long[0];
	private final List<Consumer<long[]>> listeners = new ArrayList<>();

	public BitClientStructList(
			BitStructListConnectionView<?> view,
			BitClientStream stream,
			BitClientStream.Factory streamFactory
	) {
		this.view = view;
		this.stream = stream;
		this.streamFactory = streamFactory;
	}

	public void start() {
		stream.start(this::processInput);
	}

	private void refreshListFromServer(BitInputStream fromServer) throws IOException {
		int length = IntegerBitser.decodeUnknownLength(null, "BitClient.StructList", fromServer);
		long[] newControllerIDs = new long[length];
		for (int index = 0; index < length; index++) {
			newControllerIDs[index] = IntegerBitser.decodeVariableIntegerUsingTerminatorBits(
					0L, Long.MAX_VALUE, fromServer
			);
		}

		fromServer.discardCurrentByte();

		synchronized (listeners) {
			this.controllerIDs = newControllerIDs;
			for (var listener : listeners) {
				listener.accept(Arrays.copyOf(controllerIDs, controllerIDs.length));
			}
		}
	}

	private void processInput(BitInputStream fromServer) throws Throwable {
		refreshListFromServer(fromServer);

		//noinspection InfiniteLoopStatement
		while (true) {
			// TODO BITSER Process operation feedback
			refreshListFromServer(fromServer);
		}
	}

	public BitClientStruct createConnectionToElement(long controllerID, int viewIndex) {
		return new BitClientStruct(
				view.getElementsViews().get(viewIndex),
				streamFactory.createStream(controllerID, viewIndex),
				streamFactory,
				controllerID
		);
	}

	public Object subscribe(Consumer<long[]> callback) {
		synchronized (listeners) {
			callback.accept(Arrays.copyOf(controllerIDs, controllerIDs.length));
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

	public void executeSimpleOperation(int operation) {
		stream.send(toServer ->
				IntegerBitser.encodeUniformInteger(
						operation, 0L, max(1L, view.getNumOperations() - 1), toServer
				)
		);
		// TODO BITSER Add support for operations with input & output
	}

	public void close() {
		stream.close();
	}
}
