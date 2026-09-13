package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.IntegerBitser;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class BitServer<T> {

	public static class StructController<T> {

		final long reference;
		final T structInstance;
		final StructConnectionView view;
		final List<StructConnection> connections = new ArrayList<>();

		public StructController(long reference, T structInstance, StructConnectionView view) {
			this.reference = reference;
			this.structInstance = structInstance;
			this.view = view;
		}

		public synchronized void addClient(OutputStream toClient, InputStream fromClient) {
			var connection = new StructConnection(toClient, new BitInputStream(fromClient));
			connections.add(connection);

			if (view.getNumUploadableFields() > 0) {
				var fromClientThread = new Thread(connection::readFromClient);
				fromClientThread.setDaemon(true);
				fromClientThread.start();
			}

			connection.writeToClient();
		}

		private class StructConnection {

			final OutputStream toClient;
			final BitInputStream fromClient;
			final BlockingQueue<byte[]> toClientQueue = new LinkedBlockingQueue<>();

			StructConnection(OutputStream toClient, BitInputStream fromClient) {
				this.toClient = toClient;
				this.fromClient = fromClient;
			}

			void writeToClient() {
				byte[] initialBytes;
				try {
					initialBytes = ConnectionHelper.capture(initialOutput -> {
						for (int fieldID = 0; fieldID < view.protocol.getNumFields(); fieldID++) {
							if (view.shouldDownloadDuringInitialization(fieldID)) {
								var value = view.protocol.getFieldValue(structInstance, fieldID);
								view.protocol.serializeFlatFieldValue(fieldID, initialOutput, value);
							}
						}
					});
				} catch (Throwable failed) {
					throw new RuntimeException(failed);
				}

				var toClientThread = new Thread(() -> {
					try {
						toClient.write(initialBytes);
						toClient.flush();

						while (true) {
							var nextPacket = toClientQueue.take();
							toClient.write(nextPacket);
							toClient.flush();
						}
					} catch (Throwable failed) {
						throw new RuntimeException(failed);
					}
				});
				toClientThread.setDaemon(true);
				toClientThread.start();
			}

			void readFromClient() {
				try {
					long maxUploadableFieldID = view.getNumUploadableFields() - 1L;
					//noinspection InfiniteLoopStatement
					while (true) {
						int uploadableFieldID = (int) IntegerBitser.decodeUniformInteger(
								0L, maxUploadableFieldID, fromClient
						);
						int fieldID = view.mapUploadableFieldID(uploadableFieldID);
						Object newValue = view.protocol.deserializeFlatFieldValue(fieldID, fromClient);

						if (view.canDownloadFlat(fieldID)) {
							int downloadableFieldID = view.mapToDownloadableFieldID(fieldID);
							int maxDownloadableFieldID = view.getNumLateDownloadableFields() - 1;
							byte[] packet = ConnectionHelper.capture(toClients -> {
								IntegerBitser.encodeUniformInteger(
										downloadableFieldID, 0L, maxDownloadableFieldID, toClients
								);
								view.protocol.serializeFlatFieldValue(fieldID, toClients, newValue);
							});
							synchronized (StructController.this) {
								view.protocol.setFieldValue(structInstance, fieldID, newValue);
								for (var connection : connections) connection.toClientQueue.add(packet);
							}
						}
					}
				} catch (Throwable failed) {
					throw new RuntimeException(failed);
				} finally {
					close();
				}
			}

			void close() {
				try {
					fromClient.close();
					toClient.close();
				} catch (IOException alreadyClosed) {
					// Do nothing when the connections were already dead/closed
				}
			}
		}
	}

	private final StructController<T> rootController;

	public BitServer(StructController<T> rootController) {
		this.rootController = rootController;
	}
}
