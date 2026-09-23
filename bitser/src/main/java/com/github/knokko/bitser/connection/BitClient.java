package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.IntegerBitser;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class BitClient {

	public enum ChangeState {

		UNINITIALIZED,
		UP_TO_DATE,
		MODIFIED,
		SAVING
	}

	private static abstract class AbstractField {

		final StructConnectionView view;
		final int fieldID;

		AbstractField(StructConnectionView view, int fieldID) {
			this.view = view;
			this.fieldID = fieldID;
		}

		abstract void readFromServerInitial(BitInputStream input) throws Throwable;

		abstract void postReadFromServerInitial();

		abstract void setFromServer(Object newServerValue);

		abstract void save(BitOutputStream output) throws Throwable;
	}

	public static class SimpleFlatField<T> extends AbstractField {

		private T serverValue;
		private T localValue;
		private ChangeState changeState;
		private final List<Consumer<T>> serverListeners = new ArrayList<>();
		private final List<Consumer<T>> localListeners = new ArrayList<>();
		private final List<Consumer<ChangeState>> changeStateListeners = new ArrayList<>();

		SimpleFlatField(StructConnectionView view, int fieldID) {
			super(view, fieldID);
			this.changeState = ChangeState.UNINITIALIZED;
		}

		@Override
		void readFromServerInitial(BitInputStream input) throws Throwable {
			if (view.shouldDownloadDuringInitialization(fieldID)) {
				System.out.println("reading " + fieldID + "...");
				//noinspection unchecked
				serverValue = (T) view.protocol.deserializeFlatFieldValue(fieldID, input);
				System.out.println("finished reading " + fieldID);
			}
		}

		@Override
		synchronized void postReadFromServerInitial() {
			if (view.shouldDownloadDuringInitialization(fieldID)) {
				changeState = ChangeState.UP_TO_DATE;

				for (var listener : serverListeners) listener.accept(serverValue);

				for (var listener : localListeners) listener.accept(serverValue);

				for (var listener : changeStateListeners) listener.accept(ChangeState.UP_TO_DATE);
			}
		}

		@Override
		synchronized void setFromServer(Object newServerValue) {
			//noinspection unchecked
			serverValue = (T) newServerValue;
			if (changeState != ChangeState.UP_TO_DATE) {
				if (view.protocol.areFlatValuesEqual(fieldID, localValue, newServerValue)) {
					changeState = ChangeState.UP_TO_DATE;
					localValue = null;

					for (var listener : changeStateListeners) listener.accept(ChangeState.UP_TO_DATE);
				}
			}

			for (var listener : serverListeners) listener.accept(serverValue);

			if (changeState == ChangeState.UP_TO_DATE) {
				for (var listener : localListeners) listener.accept(serverValue);
			}
		}

		public synchronized Object subscribe(boolean considerLocalValue, Consumer<T> updateValue) {
			if (considerLocalValue) localListeners.add(updateValue);
			else serverListeners.add(updateValue);

			if (changeState != ChangeState.UNINITIALIZED) {
				if (considerLocalValue && changeState != ChangeState.UP_TO_DATE) {
					updateValue.accept(localValue);
				} else {
					updateValue.accept(serverValue);
				}
			}

			return updateValue;
		}

		public synchronized Object subscribeChangeState(Consumer<ChangeState> updateChangeState) {
			changeStateListeners.add(updateChangeState);
			if (changeState != ChangeState.UNINITIALIZED) updateChangeState.accept(changeState);
			return updateChangeState;
		}

		@SuppressWarnings("SuspiciousMethodCalls")
		public synchronized void cancelSubscription(Object subscription) {
			if (changeStateListeners.remove(subscription)) return;
			if (serverListeners.remove(subscription)) return;
			if (localListeners.remove(subscription)) return;
			System.err.println("Warning: attempted to cancel missing subscription for field " + fieldID + " of " + view);
		}

		public synchronized void set(T newValue) {
			var oldChangeState = changeState;
			if (view.protocol.areFlatValuesEqual(fieldID, serverValue, newValue)) {
				changeState = ChangeState.UP_TO_DATE;
				localValue = null;
			} else {
				changeState = ChangeState.MODIFIED;
				localValue = newValue;
			}

			for (var listener : localListeners) listener.accept(newValue);

			if (changeState != oldChangeState) {
				for (var listener : changeStateListeners) listener.accept(changeState);
			}
		}

		@Override
		synchronized void save(BitOutputStream output) throws Throwable {
			if (!view.canUploadFlat(fieldID)) return;
			if (changeState != ChangeState.MODIFIED) {
				output.write(false);
				return;
			}

			output.write(true);
			System.out.println("Saving " + localValue);
			view.protocol.serializeFlatFieldValue(fieldID, output, localValue);

			changeState = ChangeState.SAVING;

			for (var listener : changeStateListeners) listener.accept(ChangeState.SAVING);
		}
	}

	public static class ChildStructField extends AbstractField {

		private long serverControllerID = -1L;
		private final ClientStream.Factory streamFactory;
		private final List<Consumer<Struct>> listeners = new ArrayList<>();

		ChildStructField(ClientStream.Factory streamFactory, StructConnectionView view, int fieldID) {
			super(view, fieldID);
			this.streamFactory = streamFactory;
		}

		@Override
		void readFromServerInitial(BitInputStream input) throws Throwable {
			if (view.shouldDownloadDuringInitialization(fieldID)) {
				serverControllerID = IntegerBitser.decodeVariableIntegerUsingTerminatorBits(
						0L, Long.MAX_VALUE, input
				);
			}
		}

		@Override
		synchronized void postReadFromServerInitial() {
			if (view.shouldDownloadDuringInitialization(fieldID)) {
				var childView = Objects.requireNonNull(view.getChildStructViewOrNull(fieldID));
				for (var listener : listeners) {
					var childStream = streamFactory.createStream(serverControllerID);
					listener.accept(new Struct(childView, childStream, streamFactory));
				}
			}
		}

		@Override
		synchronized void setFromServer(Object newServerValue) {
			long oldControllerID = serverControllerID;
			this.serverControllerID = (Long) newServerValue;

			if (oldControllerID != serverControllerID) {
				var childView = Objects.requireNonNull(view.getChildStructViewOrNull(fieldID));
				for (var listener : listeners) {
					var childStream = streamFactory.createStream(serverControllerID);
					listener.accept(new Struct(childView, childStream, streamFactory));
				}
			}
		}

		@Override
		void save(BitOutputStream output) {
			// ClientStructField's cannot be saved; save their BitClient.Struct instead"
		}

		public synchronized Object subscribe(Consumer<Struct> updateChildStruct) {
			listeners.add(updateChildStruct);

			if (serverControllerID != -1L) {
				var childView = Objects.requireNonNull(view.getChildStructViewOrNull(fieldID));
				var childStream = streamFactory.createStream(serverControllerID);
				updateChildStruct.accept(new Struct(childView, childStream, streamFactory));
			}

			return updateChildStruct;
		}

		public synchronized void cancelSubscription(Object subscription) {
			//noinspection SuspiciousMethodCalls
			if (!listeners.remove(subscription)) {
				System.err.println("Warning: attempted to cancel missing subscription for field " + fieldID + " of " + view);
			}
		}
	}

	public static class Struct {

		private final StructConnectionView view;
		private final ClientStream stream;

		private final AbstractField[] fields;
		private final List<Consumer<Boolean>> canSaveListeners = new ArrayList<>();
		private final boolean[] canSaveArray;
		private boolean lastCanSave;

		public Struct(StructConnectionView view, ClientStream stream, ClientStream.Factory streamFactory) {
			this.view = view;
			this.stream = stream;

			this.fields = new AbstractField[view.protocol.getNumFields()];
			this.canSaveArray = new boolean[fields.length];
			for (int fieldID = 0; fieldID < fields.length; fieldID++) {
				var fieldType = view.protocol.getFieldType(fieldID);
				if (fieldType == BitStructProtocol.FieldType.SIMPLE) {
					var flatField = new SimpleFlatField<>(view, fieldID);
					this.fields[fieldID] = flatField;
					flatField.subscribeChangeState(newChangeState ->
						this.updateCanSave(flatField.fieldID, newChangeState == ChangeState.MODIFIED)
					);
				}

				if (fieldType == BitStructProtocol.FieldType.STRUCT) {
					this.fields[fieldID] = new ChildStructField(streamFactory, view, fieldID);
				}
			}
		}

		public void start() {
			stream.start(this::processInput);
		}

		private void processInput(BitInputStream fromServer) throws Throwable {
			for (var field : fields) {
				if (field != null) field.readFromServerInitial(fromServer);
			}
			fromServer.discardCurrentByte();

			for (var field : fields) {
				if (field != null) field.postReadFromServerInitial();
			}

			if (!view.hasAtLeastOneDownloadableField()) return;

			//noinspection InfiniteLoopStatement
			while (true) {
				for (int fieldID = 0; fieldID < view.protocol.getNumFields(); fieldID++) {
					if (!view.canDownloadFlat(fieldID)) continue;
					if (fromServer.read()) {
						Object newValue = view.protocol.deserializeFlatFieldValue(fieldID, fromServer);
						fields[fieldID].setFromServer(newValue);
					}
				}
				fromServer.discardCurrentByte();
			}
		}

		public <T> SimpleFlatField<T> getSimpleField(Class<?> declaringClass, String fieldName) {
			int fieldID = view.protocol.getFieldId(declaringClass, fieldName);
			//noinspection unchecked
			return (SimpleFlatField<T>) fields[fieldID];
		}

		private void updateCanSave(int fieldID, boolean canSaveField) {
			synchronized (canSaveListeners) {
				boolean previousCanSave = lastCanSave;
				canSaveArray[fieldID] = canSaveField;
				boolean newCanSave = false;
				for (boolean canSave : canSaveArray) {
					if (canSave) {
						newCanSave = true;
						break;
					}
				}

				if (previousCanSave != newCanSave) {
					lastCanSave = newCanSave;
					for (var listener : canSaveListeners) listener.accept(newCanSave);
				}
			}
		}

		public Object subscribeCanSave(Consumer<Boolean> updateCanSave) {
			synchronized (canSaveListeners) {
				if (lastCanSave) updateCanSave.accept(true);
				canSaveListeners.add(updateCanSave);
				return updateCanSave;
			}
		}

		public synchronized void cancelSubscription(Object subscription) {
			synchronized (canSaveListeners) {
				//noinspection SuspiciousMethodCalls
				if (!canSaveListeners.remove(subscription)) {
					System.err.println("Warning: cancelled missing subscription from " + view);
				}
			}
		}

		public void save() {
			stream.send(this::save);
		}

		private void save(BitOutputStream toServer) throws Throwable {
			for (var field : fields) {
				if (field != null) field.save(toServer);
			}
		}

		public void close() {
			stream.close();
		}
	}
}
