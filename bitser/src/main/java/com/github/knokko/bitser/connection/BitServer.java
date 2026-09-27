package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.IntegerBitser;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.lang.Math.max;

public class BitServer<T> {

	public static abstract class Controller {

		final ControllerMapping controllerMapping;

		Controller(ControllerMapping controllerMapping) {
			this.controllerMapping = controllerMapping;
		}

		abstract Object getTargetObject();

		abstract void getChildControllers(Collection<Controller> destination);

		public abstract void addClient(OutputStream toClient, InputStream fromClient, Runnable closeStream);
	}

	public static class StructController<T> extends Controller {

		final T structInstance;
		final StructConnectionView view;
		final List<StructConnection> connections = new ArrayList<>();

		public StructController(ControllerMapping controllerMapping, T structInstance, StructConnectionView view) {
			super(controllerMapping);
			this.structInstance = structInstance;
			this.view = view;
		}

		@Override
		Object getTargetObject() {
			return structInstance;
		}

		@Override
		void getChildControllers(Collection<Controller> destination) {
			for (int fieldID = 0; fieldID < view.protocol.getNumFields(); fieldID++) {
				var childStructView = view.getChildStructViewOrNull(fieldID);
				if (childStructView != null) {
					var childStruct = view.protocol.getFieldValue(structInstance, fieldID);
					destination.add(new StructController<>(controllerMapping, childStruct, childStructView));
				}

				@SuppressWarnings("unchecked")
				var structListView = (StructListConnectionView<T>) view.getStructListViewOrNull(fieldID);
				if (structListView != null) {
					@SuppressWarnings("unchecked")
					var structList = (List<T>) view.protocol.getFieldValue(structInstance, fieldID);
					destination.add(new StructListController<>(controllerMapping, structList, structListView));
				}
			}
		}

		@Override
		public synchronized void addClient(OutputStream toClient, InputStream fromClient, Runnable closeStream) {
			var connection = new StructConnection(toClient, new BitInputStream(fromClient), closeStream);
			connections.add(connection);

			if (view.hasAtLeastOneUploadableField()) {
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
								var childStructView = view.getChildStructViewOrNull(fieldID);
								var structListView = view.getStructListViewOrNull(fieldID);

								if (childStructView == null && structListView == null) {
									view.protocol.serializeFlatFieldValue(fieldID, initialOutput, value);
								} else {
									Controller childController = controllerMapping.getControllerByTarget(value);
									long childID = controllerMapping.getIdForController(childController);
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

	public static class StructListController<T> extends Controller {

		final List<T> structList;
		final StructListConnectionView<T> view;
		final List<StructListConnection> connections = new ArrayList<>();

		public StructListController(ControllerMapping controllerMapping, List<T> structList, StructListConnectionView<T> view) {
			super(controllerMapping);
			this.structList = structList;
			this.view = view;
		}

		@Override
		Object getTargetObject() {
			return structList;
		}

		@Override
		void getChildControllers(Collection<Controller> destination) {
			for (var element : structList) {
				destination.add(new StructController<>(controllerMapping, element, view.elementsView));
			}
		}

		@Override
		public synchronized void addClient(OutputStream toClient, InputStream fromClient, Runnable closeStream) {
			var connection = new StructListConnection(
					new BitOutputStream(toClient), new BitInputStream(fromClient), closeStream
			);
			connections.add(connection);

			if (view.getNumOperations() > 0) {
				var fromClientThread = new Thread(connection::readFromClient);
				fromClientThread.setDaemon(true);
				fromClientThread.start();
			}

			connection.writeToClient();
		}

		private class StructListConnection {

			private static final byte[] CANCEL_TOKEN = new byte[0];

			final BitOutputStream toClient;
			final BitInputStream fromClient;
			final Runnable closeStream;
			final BlockingQueue<byte[]> toClientQueue = new LinkedBlockingQueue<>();

			StructListConnection(BitOutputStream toClient, BitInputStream fromClient, Runnable closeStream) {
				this.toClient = toClient;
				this.fromClient = fromClient;
				this.closeStream = closeStream;
			}

			private byte[] generatePacket() throws Throwable {
				return ConnectionHelper.capture(toClients -> {
					IntegerBitser.encodeUnknownLength(structList.size(), toClients);
					for (var element : structList) {
						var childController = controllerMapping.getControllerByTarget(element);
						long controllerID = controllerMapping.getIdForController(childController);
						IntegerBitser.encodeVariableIntegerUsingTerminatorBits(
								controllerID, 0L, Long.MAX_VALUE, toClients
						);
					}
				});
			}

			void writeToClient() {
				byte[] initialBytes;
				try {
					initialBytes = generatePacket();
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

						synchronized (StructListController.this) {
							if (operation.execute(structList)) {
								controllerMapping.addAbsentStructs(structList, view.elementsView);

								var packet = generatePacket();
								for (var connection : connections) connection.toClientQueue.add(packet);
							}
						}

						operation.respondToClient(toClient);
						toClient.flush();
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
					toClient.finish();
				} catch (IOException alreadyClosed) {
					// Do nothing when the connections were already dead/closed
				}
			}
		}
	}

	public static class ControllerMapping {

		private final Map<Controller, Long> controllerToID = new IdentityHashMap<>();
		private final Map<Long, Controller> idToController = new HashMap<>();
		private final Map<Object, Controller> targetToController = new IdentityHashMap<>();
		private final ReadWriteLock lock = new ReentrantReadWriteLock();

		private long nextID = 0L;

		public ControllerMapping() {}

		private void addSingle(Controller controller) {
			controllerToID.put(controller, nextID);
			idToController.put(nextID, controller);
			targetToController.put(controller.getTargetObject(), controller);
			nextID += 1;
		}

		public void addWithChildren(Controller rootController) {
			lock.writeLock().lock();
			try {
				var remainingControllers = new ArrayList<Controller>();
				remainingControllers.add(rootController);

				while (!remainingControllers.isEmpty()) {
					var nextController = remainingControllers.remove(remainingControllers.size() - 1);
					addSingle(nextController);

					nextController.getChildControllers(remainingControllers);
				}
			} finally {
				lock.writeLock().unlock();
			}
		}

		public void add(Controller controller) {
			lock.writeLock().lock();
			try {
				addSingle(controller);
			} finally {
				lock.writeLock().unlock();
			}
		}

		public void addAbsentStructs(Collection<?> structs, StructConnectionView elementsView) {
			lock.writeLock().lock();
			try {
				for (var structElement : structs) {
					if (targetToController.containsKey(structElement)) continue;

					var controller = new StructController<>(this, structElement, elementsView);
					addSingle(controller);
				}
			} finally {
				lock.writeLock().unlock();
			}
		}

		public long getIdForController(Controller controller) {
			lock.readLock().lock();
			try {
				return controllerToID.get(controller);
			} finally {
				lock.readLock().unlock();
			}
		}

		public Controller getControllerById(long id) {
			lock.readLock().lock();
			try {
				return Objects.requireNonNull(idToController.get(id));
			} finally {
				lock.readLock().unlock();
			}
		}

		public Controller getControllerByTarget(Object target) {
			lock.readLock().lock();
			try {
				return Objects.requireNonNull(targetToController.get(target));
			} finally {
				lock.readLock().unlock();
			}
		}
	}

	private final StructController<T> rootController;

	public BitServer(StructController<T> rootController) {
		this.rootController = rootController;
	}
}
