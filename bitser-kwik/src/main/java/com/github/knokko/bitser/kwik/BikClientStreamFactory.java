package com.github.knokko.bitser.kwik;

import com.github.knokko.bitser.connection.BitClientStream;
import com.github.knokko.bitser.connection.ConnectionHelper;
import com.github.knokko.bitser.io.BitInputStream;
import tech.kwik.core.QuicStream;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

class BikClientStreamFactory implements BitClientStream.Factory {

	private final DataOutputStream mainOutput;
	private final Map<Integer, CompletableFuture<QuicStream>> nextStreamMapping = new HashMap<>();

	private final AtomicInteger nextStreamID = new AtomicInteger();

	public BikClientStreamFactory(DataOutputStream mainOutput) {
		this.mainOutput = mainOutput;
	}

	@Override
	public BitClientStream createStream(long controllerID, int viewID) {
		return new BikClientStream(() -> requestActualStream(controllerID, viewID));
	}

	public int addResponseCallback(ConnectionHelper.BitReader readResponse) {
		var future = new CompletableFuture<QuicStream>();
		int responseID = nextStreamID.incrementAndGet();
		nextStreamMapping.put(responseID, future.whenComplete((stream, failure) -> {
			if (failure != null) return;

			new Thread(() -> {
				System.out.println("Processing response...");
				var input = new BitInputStream(stream.getInputStream());
				try {
					readResponse.read(input);
				} catch (Throwable failed) {
					throw new RuntimeException(failed);
				}
				stream.resetStream(0L);
				System.out.println("Processed response");
			}).start();
		}));
		return responseID;
	}

	private CompletableFuture<QuicStream> requestActualStream(long controllerID, int viewID) {
		var future = new CompletableFuture<QuicStream>();
		synchronized (mainOutput) {
			int streamID = nextStreamID.incrementAndGet();
			try {
				mainOutput.writeInt(streamID);
				mainOutput.writeLong(controllerID);
				mainOutput.writeInt(viewID);
				mainOutput.flush();
			} catch (IOException failed) {
				throw new RuntimeException(failed);
			}
			nextStreamMapping.put(streamID, future);
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
