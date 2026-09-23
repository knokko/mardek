package com.github.knokko.bitser.kwik;

import com.github.knokko.bitser.connection.ClientStream;
import tech.kwik.core.QuicStream;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

class BikClientStreamFactory implements ClientStream.Factory {

	private final DataOutputStream mainOutput;
	private final Map<Integer, CompletableFuture<QuicStream>> nextStreamMapping = new HashMap<>();

	private int nextStreamID;

	public BikClientStreamFactory(DataOutputStream mainOutput) {
		this.mainOutput = mainOutput;
	}

	@Override
	public ClientStream createStream(long controllerID) {
		return new BikClientStream(() -> requestActualStream(controllerID));
	}

	private CompletableFuture<QuicStream> requestActualStream(long controllerID) {
		var future = new CompletableFuture<QuicStream>();
		synchronized (mainOutput) {
			try {
				mainOutput.writeInt(nextStreamID);
				mainOutput.writeLong(controllerID);
				mainOutput.flush();
			} catch (IOException failed) {
				throw new RuntimeException(failed);
			}
			nextStreamMapping.put(nextStreamID, future);
			nextStreamID += 1;
		}
		return future;
	}

	public void addStream(QuicStream newStream) {
		var dataInput = new DataInputStream(newStream.getInputStream());
		int streamID;
		try {
			streamID = dataInput.readInt();
		} catch (IOException failed) {
			throw new RuntimeException(failed);
		}

		var future = nextStreamMapping.remove(streamID);
		if (future == null) throw new IllegalStateException("Unexpected stream ID " + streamID);

		future.complete(newStream);
	}
}
