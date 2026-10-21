package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.IntegerBitser;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static java.lang.Math.max;

public class BitServerStructListController<T> extends BitServerController {

	final List<T> structList;
	final BitStructProtocol elementsProtocol;
	final List<BitStructListConnectionView<T>> views = new ArrayList<>();
	final List<BitStructConnectionView> elementsViews = new ArrayList<>(); // TODO BITSER Hm... how do I insert them?
	final List<StructListConnection> connections = new ArrayList<>();

	public BitServerStructListController(
			BitServerContext context, List<T> structList, BitStructProtocol elementsProtocol
	) {
		super(context);
		this.structList = structList;
		this.elementsProtocol = elementsProtocol;
	}

	@Override
	Object getTargetObject() {
		return structList;
	}

	@Override
	public void addClient(
			OutputStream toClient, InputStream fromClient, Runnable closeStream,
			int viewIndex
	) {
		var connection = new StructListConnection(
				new BitOutputStream(toClient), new BitInputStream(fromClient), closeStream,
				views.get(viewIndex)
		);

		context.globalLock().readLock().lock();
		try {
			connections.add(connection);

			if (connection.view.getNumOperations() > 0) {
				var fromClientThread = new Thread(connection::readFromClient);
				fromClientThread.setDaemon(true);
				fromClientThread.start();
			}

			connection.writeToClient();
		} finally {
			context.globalLock().readLock().unlock();
		}
	}

	@Override
	boolean addViewIfNotPresent(BitConnectionView view) {
		@SuppressWarnings("unchecked")
		var listView = (BitStructListConnectionView<T>) view;
		if (listView.elementsProtocol != elementsProtocol) {
			throw new IllegalArgumentException("Protocol mismatch: " + elementsProtocol + " vs " + listView.elementsProtocol);
		}

		if (views.contains(listView)) return false;
		views.add(listView);

		for (var elementsView : listView.getElementsViews()) {
			if (!elementsViews.contains(elementsView)) elementsViews.add(elementsView);
		}
		return true;
	}

	@Override
	void getChildViews(Deque<BitServerControllerMapping.ViewPair> destination) {
		for (var element : structList) {
			for (var view : elementsViews) {
				destination.add(new BitServerControllerMapping.ViewPair(element, view));
			}
		}
	}

	private byte[] generatePacketForClients() throws Throwable {
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
			context.controllerMapping().addIfNotPresent(context, structList, elementsViews);

			var packet = generatePacketForClients();
			for (var connection : connections) connection.toClientQueue.add(packet);
		} finally {
			context.globalLock().readLock().unlock();
		}
	}

	private class StructListConnection {

		private static final byte[] CANCEL_TOKEN = new byte[0];

		final BitOutputStream toClient;
		final BitInputStream fromClient;
		final Runnable closeStream;
		final BitStructListConnectionView<T> view;
		final BlockingQueue<byte[]> toClientQueue = new LinkedBlockingQueue<>();

		StructListConnection(
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
				initialBytes = generatePacketForClients();
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
				System.err.println("StructListConnection.readFromClient thread died: " + failed.getMessage());
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
