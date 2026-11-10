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
		return new BikClientStream(() -> requestActualStream(controllerID, viewID), this);
	}

	@Override
	public int addResponseCallback(ConnectionHelper.BitReader readResponse) {
		var future = new CompletableFuture<QuicStream>();
		int responseID = nextStreamID.incrementAndGet();
		System.out.println("Chose response ID " + responseID);
		future.thenAccept(stream -> {
			System.out.println("Running future...");

			var processResponseThread = new Thread(() -> {
				System.out.println("Processing response...");
				var input = new BitInputStream(stream.getInputStream());
				try {
					readResponse.read(input);
				} catch (Throwable failed) {
					throw new RuntimeException(failed);
				}
				stream.resetStream(0L);
				System.out.println("Processed response");
			});
			processResponseThread.setName("BikClientStreamFactory.addResponseCallback.processResponse");
			processResponseThread.start();
			System.out.println("Started future thread");

		});
		nextStreamMapping.put(responseID, future);
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

		System.out.println("The stream ID is " + streamID);
		var future = nextStreamMapping.remove(streamID);
		System.out.println("The future is " + future);
		if (future == null) throw new IllegalStateException("Unexpected stream ID " + streamID);

		System.out.println("Completing the future...");
		future.complete(newStream);
		System.out.println("Completed the future " + future);
	}
}
