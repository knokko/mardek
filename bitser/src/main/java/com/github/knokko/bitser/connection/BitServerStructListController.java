package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.IntegerBitser;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import static java.lang.Math.max;

public class BitServerStructListController<T> extends BitServerController {

	private static final byte[] CANCEL_TOKEN = new byte[0];

	final List<T> structList;
	final BitStructProtocol elementsProtocol;
	final List<BitStructListConnectionView<T>> nestedViews = new ArrayList<>();
	final List<BitFlatStructListConnectionView<T>> flatViews = new ArrayList<>();
	final List<BitStructConnectionView> elementsViews = new ArrayList<>();
	final List<NestedConnection> nestedConnections = new ArrayList<>();
	final List<FlatConnection> flatConnections = new ArrayList<>();
	private final Semaphore hasFlatUpdatesSemaphore = new Semaphore(0);

	public BitServerStructListController(
			BitServerContext context, List<T> structList, BitStructProtocol elementsProtocol
	) {
		super(context);
		this.structList = structList;
		this.elementsProtocol = elementsProtocol;

		var flatUpdateThread = new Thread(this::runFlatUpdateThread);
		flatUpdateThread.setDaemon(true);
		flatUpdateThread.start(); // TODO BITSER Stop the thread after this controller is closed
	}

	@Override
	Object getTargetObject() {
		return structList;
	}

	private void runFlatUpdateThread() {
		while (true) {
			hasFlatUpdatesSemaphore.acquireUninterruptibly();
			hasFlatUpdatesSemaphore.drainPermits();

			context.globalLock().readLock().lock();
			try {
				synchronized (this) {
					for (var connection : flatConnections) {
						try {
							connection.toClientQueue.add(connection.generatePacketForClient());
						} catch (Throwable failed) {
							throw new RuntimeException(failed);
						}
					}
				}
			} finally {
				context.globalLock().readLock().unlock();
			}
		}
	}

	@Override
	public void addClient(
			OutputStream toClient, InputStream fromClient, Runnable closeStream,
			int viewID
	) {
		var nestedView = nestedViews.stream().filter(view -> view.localId == viewID).findAny();
		if (nestedView.isPresent()) {
			var connection = new NestedConnection(
					new BitOutputStream(toClient), new BitInputStream(fromClient), closeStream,
					nestedView.get()
			);

			context.globalLock().readLock().lock();
			try {
				synchronized (this) {
					nestedConnections.add(connection);

					if (connection.view.getNumOperations() > 0) {
						var fromClientThread = new Thread(connection::readFromClient);
						fromClientThread.setDaemon(true);
						fromClientThread.start();
					}

					connection.writeToClient();
				}
			} finally {
				context.globalLock().readLock().unlock();
			}
		} else {
			var flatView = flatViews.stream().filter(view -> view.localId == viewID).findAny();
			if (flatView.isEmpty()) throw new UnsupportedOperationException("No view has local ID " + viewID);
			var connection = new FlatConnection(
					new BitOutputStream(toClient), new BitInputStream(fromClient), closeStream,
					flatView.get()
			);

			context.globalLock().readLock().lock();
			try {
				synchronized (this) {
					flatConnections.add(connection);

					if (true) { // TODO BITSER Find a way to not need this
						var fromClientThread = new Thread(connection::readFromClient);
						fromClientThread.setDaemon(true);
						fromClientThread.start();
					}

					connection.writeToClient();
				}
			} finally {
				context.globalLock().readLock().unlock();
			}
		}
	}

	private void checkNoDuplicateLocalIDs(int newViewID) {
		if (nestedViews.stream().anyMatch(
				otherView -> otherView.localId == newViewID
		) || flatViews.stream().anyMatch(
				otherView -> otherView.localId == newViewID
		)) {
			throw new RuntimeException("Duplicate local view ID " + newViewID);
		}
	}

	@Override
	boolean addViewIfNotPresent(BitConnectionView view) {
		if (view instanceof BitStructListConnectionView) {
			@SuppressWarnings("unchecked")
			var listView = (BitStructListConnectionView<T>) view;
			if (listView.elementsProtocol != elementsProtocol) {
				throw new IllegalArgumentException("Protocol mismatch: " + elementsProtocol + " vs " + listView.elementsProtocol);
			}

			if (nestedViews.contains(listView)) return false;
			checkNoDuplicateLocalIDs(listView.localId);
			nestedViews.add(listView);

			for (var elementsView : listView.getElementsViews()) {
				if (!elementsViews.contains(elementsView)) elementsViews.add(elementsView);
			}
		} else {
			@SuppressWarnings("unchecked")
			var listView = (BitFlatStructListConnectionView<T>) view;
			if (listView.elementsView.protocol != elementsProtocol) {
				throw new IllegalArgumentException("Protocol mismatch: " + elementsProtocol + " vs " + listView.elementsView.protocol);
			}

			if (flatViews.contains(listView)) return false;
			checkNoDuplicateLocalIDs(listView.localId);
			flatViews.add(listView);
		}

		return true;
	}

	@Override
	void addOnChangeSemaphoreIfNotPresent(Semaphore semaphore) {
		throw new UnsupportedOperationException("Change semaphores are only supported by struct controllers");
	}

	@Override
	void getChildViews(
			Deque<BitServerControllerMapping.ViewPair> destination,
			Collection<BitServerControllerMapping.SemaphorePair> destinationSemaphores
	) {
		for (var element : structList) {
			for (var view : elementsViews) {
				destination.add(new BitServerControllerMapping.ViewPair(element, view));
			}
			destinationSemaphores.add(new BitServerControllerMapping.SemaphorePair(element, hasFlatUpdatesSemaphore));
		}
	}

	private byte[] generatePacketForNestedClients() throws Throwable {
		return ConnectionHelper.capture(toClients -> {
			IntegerBitser.encodeUnknownLength(structList.size(), toClients);
			for (var element : structList) {
				var childController = context.controllerMapping().getControllerByTarget(element);
				long controllerID = context.controllerMapping().getIdForController(childController);
				IntegerBitser.encodeVariableIntegerUsingTerminatorBits(
						controllerID, 0L, Long.MAX_VALUE, toClients
				);
			}
		});
	}

	public void refreshClientConnections() throws Throwable {
		context.globalLock().readLock().lock();
		try {
			context.controllerMapping().addIfNotPresent(
					context, structList, elementsViews, List.of(hasFlatUpdatesSemaphore)
			);

			var packet = generatePacketForNestedClients();
			for (var connection : nestedConnections) connection.toClientQueue.add(packet);
			hasFlatUpdatesSemaphore.release();
		} finally {
			context.globalLock().readLock().unlock();
		}
	}

	private class NestedConnection {

		final BitOutputStream toClient;
		final BitInputStream fromClient;
		final Runnable closeStream;
		final BitStructListConnectionView<T> view;
		final BlockingQueue<byte[]> toClientQueue = new LinkedBlockingQueue<>();

		NestedConnection(
				BitOutputStream toClient, BitInputStream fromClient, Runnable closeStream,
				BitStructListConnectionView<T> view
		) {
			this.toClient = toClient;
			this.fromClient = fromClient;
			this.closeStream = closeStream;
			this.view = view;
		}

		void writeToClient() {
			byte[] initialBytes;
			try {
				initialBytes = generatePacketForNestedClients();
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
				//noinspection InfiniteLoopStatement
				while (true) {
					int operationIndex = (int) IntegerBitser.decodeUniformInteger(
							0L, max(1L, view.getNumOperations() - 1L), fromClient
					);
					var operation = view.getOperation(operationIndex);

					operation.readFromClient(fromClient);
					fromClient.discardCurrentByte();

					boolean[] isGlobal = { false };
					if (operation instanceof BitStructListConnectionView.GlobalOperation<?,?>) {
						//noinspection unchecked
						((BitStructListConnectionView.GlobalOperation<T, Object>) operation).considerGlobalState(() -> {
							isGlobal[0] = true;
							return new BitStructListConnectionView.GlobalContext<>(
									context.rootStruct(), structList, context.controllerMapping()
							);
						});
					}

					if (isGlobal[0]) {
						boolean didQueue = context.globalQueue().offer(() -> {
							operation.execute(structList);
							operation.respondToClient(toClient);
							toClient.flush();
						});
						if (!didQueue) {
							System.err.println("Failed to put operation " + operationIndex + " in the global queue");
							// TODO BITSER Communicate this to the client somehow
						}
					} else {
						context.globalLock().readLock().lock();
						try {
							synchronized (BitServerStructListController.this) {
								if (operation.execute(structList)) {
									refreshClientConnections();
								}
							}
						} finally {
							context.globalLock().readLock().unlock();
						}
						operation.respondToClient(toClient);
						toClient.flush();
					}
				}
			} catch (Throwable failed) {
				System.err.println("StructListController.NestedConnection.readFromClient thread died: " + failed.getMessage());
			} finally {
				close();
			}
		}

		void close() {
			try {
				toClientQueue.add(CANCEL_TOKEN);
				closeStream.run();
				fromClient.close();
				toClient.finish();
			} catch (IOException alreadyClosed) {
				// Do nothing when the connections were already dead/closed
			}
		}
	}

	private class FlatConnection {

		final BitOutputStream toClient;
		final BitInputStream fromClient;
		final Runnable closeStream;
		final BitFlatStructListConnectionView<T> view;
		final BlockingQueue<byte[]> toClientQueue = new LinkedBlockingQueue<>();

		FlatConnection(
				BitOutputStream toClient, BitInputStream fromClient, Runnable closeStream,
				BitFlatStructListConnectionView<T> view
		) {
			this.toClient = toClient;
			this.fromClient = fromClient;
			this.closeStream = closeStream;
			this.view = view;
		}

		private byte[] generatePacketForClient() throws Throwable {
			return ConnectionHelper.capture(toClient -> {
				IntegerBitser.encodeUnknownLength(structList.size(), toClient);
				for (var element : structList) {
					var childController = (BitServerStructController<?>) context.controllerMapping().getControllerByTarget(element);
					long controllerID = context.controllerMapping().getIdForController(childController);
					IntegerBitser.encodeVariableIntegerUsingTerminatorBits(
							controllerID, 0L, Long.MAX_VALUE, toClient
					);

					for (int fieldID = 0; fieldID < elementsProtocol.getNumFields(); fieldID++) {
						if (view.elementsView.shouldDownloadSimpleField(fieldID)) {
							Object fieldValue;
							childController.lock.readLock().lock();
							try {
								fieldValue = elementsProtocol.getFieldValue(element, fieldID);
							} finally {
								childController.lock.readLock().unlock();
							}
							elementsProtocol.serializeFlatFieldValue(fieldID, toClient, fieldValue);
						}

						var childStruct = view.elementsView.getChildStructOrNull(fieldID);
						if (childStruct != null) {
							throw new UnsupportedOperationException("TODO BITSER: nested structs in flat lists");
						}
					}
				}
			});
		}

		void writeToClient() {
			byte[] initialBytes;
			try {
				initialBytes = generatePacketForClient();
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
				fromClient.read();
			} catch (Throwable failed) {
				System.err.println("StructListController.FlatConnection.readFromClient thread died: " + failed.getMessage());
			} finally {
				close();
			}
		}

		void close() {
			try {
				toClientQueue.add(CANCEL_TOKEN);
				closeStream.run();
				fromClient.close();
				toClient.finish();
			} catch (IOException alreadyClosed) {
				// Do nothing when the connections were already dead/closed
			}
		}
	}
}
