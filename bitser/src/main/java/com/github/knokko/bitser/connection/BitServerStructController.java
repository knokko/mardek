package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.IntegerBitser;
import com.github.knokko.bitser.io.BitInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BitServerStructController<T> extends BitServerController {

	final T structInstance;
	final BitStructProtocol protocol;
	final List<BitStructConnectionView> views = new ArrayList<>();
	final List<Connection> connections = new ArrayList<>();
	final ReadWriteLock lock = new ReentrantReadWriteLock();
	final List<Semaphore> onChangeSemaphores = new ArrayList<>();

	public BitServerStructController(BitServerContext context, T structInstance, BitStructProtocol protocol) {
		super(context);
		this.structInstance = structInstance;
		this.protocol = protocol;
	}

	@Override
	Object getTargetObject() {
		return structInstance;
	}

	@Override
	public void addClient(
			OutputStream toClient, InputStream fromClient, Runnable closeStream,
			int viewID
	) {
		var view = views.stream().filter(candidate -> candidate.localId == viewID).findAny();
		if (view.isEmpty()) throw new IllegalArgumentException("No view has local ID " + viewID);
		var connection = new Connection(view.get(), toClient, new BitInputStream(fromClient), closeStream);

		context.globalLock().readLock().lock();
		try {
			lock.writeLock().lock();
			try {
				connections.add(connection);

				if (connection.view.hasAtLeastOneUploadableField()) {
					var fromClientThread = new Thread(connection::readFromClient);
					fromClientThread.setDaemon(true);
					fromClientThread.start();
				}

				connection.writeToClient();
			} finally {
				lock.writeLock().unlock();
			}
		} finally {
			context.globalLock().readLock().unlock();
		}
	}

	@Override
	boolean addViewIfNotPresent(BitConnectionView view) {
		var structView = (BitStructConnectionView) view;
		if (structView.protocol != protocol) {
			throw new IllegalArgumentException("Protocol mismatch: " + protocol + " vs " + structView.protocol);
		}

		if (views.contains(structView)) return false;
		if (views.stream().anyMatch(otherView -> otherView.localId == view.localId)) {
			throw new IllegalArgumentException("Multiple local views have local ID " + view.localId);
		}
		views.add(structView);
		return true;
	}

	@Override
	void addOnChangeSemaphoreIfNotPresent(Semaphore semaphore) {
		if (!onChangeSemaphores.contains(semaphore)) onChangeSemaphores.add(semaphore);
	}

	@Override
	void getChildViews(
			Deque<BitServerControllerMapping.ViewPair> destination,
			Collection<BitServerControllerMapping.SemaphorePair> destinationSemaphores
	) {
		for (int fieldID = 0; fieldID < protocol.getNumFields(); fieldID++) {
			for (var view : views) {

				var childStructViews = view.getChildStructViewsOrNull(fieldID);
				if (childStructViews != null) {
					var childStruct = protocol.getFieldValue(structInstance, fieldID);
					for (var childView : childStructViews) {
						destination.add(new BitServerControllerMapping.ViewPair(childStruct, childView));
					}
				}

				var structListViews = view.getStructListViewsOrNull(fieldID);
				if (structListViews != null) {
					@SuppressWarnings("unchecked")
					var structList = (List<T>) protocol.getFieldValue(structInstance, fieldID);
					for (var structListView : structListViews) {
						destination.add(new BitServerControllerMapping.ViewPair(structList, structListView));
					}
				}

				var flatStructListViews = view.getFlatStructListViewsOrNull(fieldID);
				if (flatStructListViews != null) {
					@SuppressWarnings("unchecked")
					var structList = (List<T>) protocol.getFieldValue(structInstance, fieldID);
					for (var structListView : flatStructListViews) {
						destination.add(new BitServerControllerMapping.ViewPair(structList, structListView));
					}
				}
			}
		}
	}

	private class Connection {

		private static final byte[] CANCEL_TOKEN = new byte[0];

		final BitStructConnectionView view;
		final OutputStream toClient;
		final BitInputStream fromClient;
		final Runnable closeStream;
		final BlockingQueue<byte[]> toClientQueue = new LinkedBlockingQueue<>();

		Connection(BitStructConnectionView view, OutputStream toClient, BitInputStream fromClient, Runnable closeStream) {
			this.view = view;
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
							var childStructViews = view.getChildStructViewsOrNull(fieldID);
							var structListViews = view.getStructListViewsOrNull(fieldID);
							var structReferenceViews = view.getStructReferenceViewsOrNull(fieldID);

							if (childStructViews == null && structListViews == null && structReferenceViews == null) {
								view.protocol.serializeFlatFieldValue(fieldID, initialOutput, value);
							} else {
								var childController = context.controllerMapping().getControllerByTarget(value);
								long childID = context.controllerMapping().getIdForController(childController);
								IntegerBitser.encodeVariableIntegerUsingTerminatorBits(
										childID, 0L, Long.MAX_VALUE, initialOutput
								);
							}
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
						var referenceViews = view.getStructReferenceViewsOrNull(fieldID);
						if (referenceViews == null) {
							if (!view.canUploadFlat(fieldID)) continue;
							if (!fromClient.read()) continue;

							newValues[fieldID] = view.protocol.deserializeFlatFieldValue(fieldID, fromClient);
							hasChanges[fieldID] = true;
						} else {
							if (!view.canUploadReference(fieldID)) continue;
							if (!fromClient.read()) continue;

							long newControllerID = IntegerBitser.decodeVariableIntegerUsingTerminatorBits(
									0L, Long.MAX_VALUE, fromClient
							);
							newValues[fieldID] = newControllerID;
							hasChanges[fieldID] = true;
						}
					}
					fromClient.discardCurrentByte();

					byte[] packet = ConnectionHelper.capture(toClients -> {
						for (int fieldID = 0; fieldID < numFields; fieldID++) {
							var referenceViews = view.getStructReferenceViewsOrNull(fieldID);
							if (referenceViews == null) {
								if (!view.canDownloadFlat(fieldID)) continue;
								toClients.write(hasChanges[fieldID]);
								if (hasChanges[fieldID]) {
									view.protocol.serializeFlatFieldValue(fieldID, toClients, newValues[fieldID]);
								}
							} else {
								if (!view.canUploadReference(fieldID)) continue;
								toClients.write(hasChanges[fieldID]);
								if (hasChanges[fieldID]) {
									IntegerBitser.encodeVariableIntegerUsingTerminatorBits(
											(Long) newValues[fieldID], 0L, Long.MAX_VALUE, toClients
									);
								}
							}
						}
					});

					context.globalLock().readLock().lock();
					try {
						lock.writeLock().lock();
						try {
							for (int fieldID = 0; fieldID < numFields; fieldID++) {
								if (hasChanges[fieldID]) {
									var referenceViews = view.getStructReferenceViewsOrNull(fieldID);
									if (referenceViews == null) {
										view.protocol.setFieldValue(structInstance, fieldID, newValues[fieldID]);
									} else {
										long newControllerID = (Long) newValues[fieldID];
										var newController = context.controllerMapping().getControllerById(newControllerID);
										var newValue = ((BitServerStructController<?>) newController).structInstance;
										// TODO BITSER Validate this
										view.protocol.setFieldValue(structInstance, fieldID, newValue);
									}
								}
							}

							if (packet.length > 0) {
								for (var connection : connections) connection.toClientQueue.add(packet);
								System.out.println("Release " + onChangeSemaphores.size() + " change semaphores");
								for (var semaphore : onChangeSemaphores) semaphore.release();
							}
						} finally {
							lock.writeLock().unlock();
						}
					} finally {
						context.globalLock().readLock().unlock();
					}
				}
			} catch (Throwable failed) {
				System.err.println("StructController.readFromClient thread died: " + failed.getMessage());
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
