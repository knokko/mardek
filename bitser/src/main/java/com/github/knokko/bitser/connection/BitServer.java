package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.io.BitInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

		public synchronized void addClient(OutputStream toClient, InputStream fromClient, Runnable closeStream) {
			var connection = new StructConnection(toClient, new BitInputStream(fromClient), closeStream);
			connections.add(connection);

			if (view.getNumUploadableFields() > 0) {
				var fromClientThread = new Thread(connection::readFromClient);
				fromClientThread.setDaemon(true);
				fromClientThread.start();
			}

			connection.writeToClient();
		}

		private class StructConnection {

			private static final byte[] CANCEL_TOKEN = new byte[0];

			final OutputStream toClient;
			final BitInputStream fromClient;
			final Runnable closeStream;
			final BlockingQueue<byte[]> toClientQueue = new LinkedBlockingQueue<>();

			StructConnection(OutputStream toClient, BitInputStream fromClient, Runnable closeStream) {
				this.toClient = toClient;
				this.fromClient = fromClient;
				this.closeStream = closeStream;
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
							if (nextPacket == CANCEL_TOKEN) return;
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
					int numFields = view.protocol.getNumFields();
					Object[] newValues = new Object[numFields];
					boolean[] hasChanges = new boolean[numFields];

					Arrays.fill(newValues, null);
					Arrays.fill(hasChanges, false);

					//noinspection InfiniteLoopStatement
					while (true) {
						for (int fieldID = 0; fieldID < numFields; fieldID++) {
							if (!view.canUploadFlat(fieldID)) continue;
							if (!fromClient.read()) continue;

							newValues[fieldID] = view.protocol.deserializeFlatFieldValue(fieldID, fromClient);
							hasChanges[fieldID] = true;
						}
						fromClient.discardCurrentByte();

						byte[] packet = ConnectionHelper.capture(toClients -> {
							for (int fieldID = 0; fieldID < numFields; fieldID++) {
								if (!view.canDownloadFlat(fieldID)) continue;
								toClients.write(hasChanges[fieldID]);
								if (hasChanges[fieldID]) {
									view.protocol.serializeFlatFieldValue(fieldID, toClients, newValues[fieldID]);
								}
							}
						});

						synchronized (StructController.this) {
							for (int fieldID = 0; fieldID < numFields; fieldID++) {
								if (hasChanges[fieldID]) {
									view.protocol.setFieldValue(structInstance, fieldID, newValues[fieldID]);
								}
							}

							if (packet.length > 0) {
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
					toClientQueue.add(CANCEL_TOKEN);
					closeStream.run();
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
