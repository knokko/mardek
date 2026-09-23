package com.github.knokko.bitser.kwik;

import com.github.knokko.bitser.connection.ClientStream;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import tech.kwik.core.QuicStream;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

class BikClientStream implements ClientStream {

	private final Supplier<CompletableFuture<QuicStream>> requestStream;
	private final Object sendLock = new Object();
	private CompletableFuture<QuicStream> stream;

	BikClientStream(Supplier<CompletableFuture<QuicStream>> requestStream) {
		this.requestStream = requestStream;
	}

	@Override
	public synchronized void start(InputReader processInput) {
		if (stream != null) throw new IllegalStateException("Already started");
		stream = requestStream.get();
		stream.whenComplete((quicStream, _) -> {
			if (quicStream == null) return ;
			var readThread = new Thread(() -> {
				try {
					processInput.read(new BitInputStream(quicStream.getInputStream()));
				} catch (Throwable e) {
					throw new RuntimeException(e);
				} finally {
					quicStream.resetStream(0L);
				}
			});
			readThread.setDaemon(true);
			readThread.start();
		});
	}

	@Override
	public void send(OutputWriter sendFrame) {
		if (stream == null) throw new IllegalStateException("Not yet started");

		synchronized(sendLock) {
			try {
				var output = new BitOutputStream(stream.get(5, TimeUnit.SECONDS).getOutputStream());
				sendFrame.write(output);
				output.flush();
			} catch (Throwable failed) {
				throw new RuntimeException(failed);
			}
		}
	}

	@Override
	public void close() {
		if (stream != null) {
			try {
				stream.get(5, TimeUnit.SECONDS).resetStream(0L);
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}
	}
}
