package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.IntegerBitser;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import static java.lang.Math.max;

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
		private ChangeState changeState = ChangeState.UNINITIALIZED;
		private final List<Consumer<T>> serverListeners = new ArrayList<>();
		private final List<Consumer<T>> localListeners = new ArrayList<>();
		private final List<Consumer<ChangeState>> changeStateListeners = new ArrayList<>();

		SimpleFlatField(StructConnectionView view, int fieldID) {
			super(view, fieldID);
		}

		@Override
		void readFromServerInitial(BitInputStream input) throws Throwable {
			if (view.shouldDownloadDuringInitialization(fieldID)) {
				//noinspection unchecked
				serverValue = (T) view.protocol.deserializeFlatFieldValue(fieldID, input);
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
			view.protocol.serializeFlatFieldValue(fieldID, output, localValue);

			changeState = ChangeState.SAVING;

			for (var listener : changeStateListeners) listener.accept(ChangeState.SAVING);
		}
	}

	private static class ChildStructField extends AbstractField {

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
					listener.accept(new Struct(childView, childStream, streamFactory, serverControllerID));
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
					listener.accept(new Struct(childView, childStream, streamFactory, serverControllerID));
				}
			}
		}

		@Override
		void save(BitOutputStream output) {
			// ClientStructField's cannot be saved; save their BitClient.Struct instead
		}

		public synchronized Object subscribe(Consumer<Struct> updateChildStruct) {
			listeners.add(updateChildStruct);

			if (serverControllerID != -1L) {
				var childView = Objects.requireNonNull(view.getChildStructViewOrNull(fieldID));
				var childStream = streamFactory.createStream(serverControllerID);
				updateChildStruct.accept(new Struct(childView, childStream, streamFactory, serverControllerID));
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

	public static class StructReferenceField extends AbstractField {

		private long serverControllerID = -1L;
		private long localControllerID = -1L;
		private ChangeState changeState = ChangeState.UNINITIALIZED;
		private final List<LongConsumer> serverListeners = new ArrayList<>();
		private final List<LongConsumer> localListeners = new ArrayList<>();
		private final List<Consumer<ChangeState>> changeStateListeners = new ArrayList<>();
		private final ClientStream.Factory streamFactory;

		StructReferenceField(ClientStream.Factory streamFactory, StructConnectionView view, int fieldID) {
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
				changeState = ChangeState.UP_TO_DATE;

				for (var listener : serverListeners) listener.accept(serverControllerID);

				for (var listener : localListeners) listener.accept(serverControllerID);

				for (var listener : changeStateListeners) listener.accept(ChangeState.UP_TO_DATE);
			}
		}

		@Override
		synchronized void setFromServer(Object newServerValue) {
			serverControllerID = (Long) newServerValue;
			if (changeState != ChangeState.UP_TO_DATE) {
				if (serverControllerID == localControllerID) {
					changeState = ChangeState.UP_TO_DATE;
					localControllerID = -1L;

					for (var listener : changeStateListeners) listener.accept(ChangeState.UP_TO_DATE);
				}
			}

			for (var listener : serverListeners) listener.accept(serverControllerID);

			if (changeState == ChangeState.UP_TO_DATE) {
				for (var listener : localListeners) listener.accept(serverControllerID);
			}
		}

		@Override
		synchronized void save(BitOutputStream output) throws Throwable {
			if (!view.canUploadReference(fieldID)) return;

			if (changeState != ChangeState.MODIFIED) {
				output.write(false);
				return;
			}

			output.write(true);
			IntegerBitser.encodeVariableIntegerUsingTerminatorBits(
					localControllerID, 0L, Long.MAX_VALUE, output
			);

			changeState = ChangeState.SAVING;

			for (var listener : changeStateListeners) listener.accept(ChangeState.SAVING);
		}

		public synchronized Object subscribe(boolean considerLocalValue, LongConsumer updateReference) {
			if (considerLocalValue) localListeners.add(updateReference);
			else serverListeners.add(updateReference);

			if (changeState != ChangeState.UNINITIALIZED) {
				if (considerLocalValue && changeState != ChangeState.UP_TO_DATE) {
					updateReference.accept(localControllerID);
				} else {
					updateReference.accept(serverControllerID);
				}
			}

			return updateReference;
		}

		public Struct createConnectionToReference(long controllerID) {
			return new Struct(
					view.getStructReferenceViewOrNull(fieldID),
					streamFactory.createStream(controllerID),
					streamFactory,
					controllerID
			);
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
			System.err.println("Warning: attempted to cancel missing subscription for reference field " + fieldID + " of " + view);
		}

		public synchronized void set(long newControllerID) {
			var oldChangeState = changeState;
			if (serverControllerID == newControllerID) {
				changeState = ChangeState.UP_TO_DATE;
				localControllerID = -1L;
			} else {
				changeState = ChangeState.MODIFIED;
				localControllerID = newControllerID;
			}

			for (var listener : localListeners) listener.accept(newControllerID);

			if (changeState != oldChangeState) {
				for (var listener : changeStateListeners) listener.accept(changeState);
			}
		}
	}

	public static class StructListField extends AbstractField {

		private long serverControllerID = -1L;
		private final ClientStream.Factory streamFactory;
		private final List<Consumer<StructList>> listeners = new ArrayList<>();

		StructListField(ClientStream.Factory streamFactory, StructConnectionView view, int fieldID) {
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
		void postReadFromServerInitial() {
			if (view.shouldDownloadDuringInitialization(fieldID)) {
				var childView = Objects.requireNonNull(view.getStructListViewOrNull(fieldID));
				for (var listener : listeners) {
					var childStream = streamFactory.createStream(serverControllerID);
					listener.accept(new StructList(childView, childStream, streamFactory));
				}
			}
		}

		@Override
		void setFromServer(Object newServerValue) {
			long oldControllerID = serverControllerID;
			this.serverControllerID = (Long) newServerValue;

			if (oldControllerID != serverControllerID) {
				var childView = Objects.requireNonNull(view.getStructListViewOrNull(fieldID));
				for (var listener : listeners) {
					var childStream = streamFactory.createStream(serverControllerID);
					listener.accept(new StructList(childView, childStream, streamFactory));
				}
			}
		}

		@Override
		void save(BitOutputStream output) {
			// Struct list fields cannot be reassigned; only modified
		}

		public synchronized Object subscribe(Consumer<StructList> updateStructList) {
			listeners.add(updateStructList);

			if (serverControllerID != -1L) {
				var childView = Objects.requireNonNull(view.getStructListViewOrNull(fieldID));
				var childStream = streamFactory.createStream(serverControllerID);
				updateStructList.accept(new StructList(childView, childStream, streamFactory));
			}

			return updateStructList;
		}

		public synchronized void cancelSubscription(Object subscription) {
			//noinspection SuspiciousMethodCalls
			if (!listeners.remove(subscription)) {
				System.err.println("Warning: attempted to cancel missing StructListField subscription " +
						"for field " + fieldID + " of " + view);
			}
		}
	}

	public static class Struct {

		private final StructConnectionView view;
		private final ClientStream stream;
		private final long controllerID;

		private final AbstractField[] fields;
		private final List<Consumer<Boolean>> canSaveListeners = new ArrayList<>();
		private final boolean[] canSaveArray;
		private boolean lastCanSave;

		public Struct(
				StructConnectionView view, ClientStream stream,
				ClientStream.Factory streamFactory, long controllerID
		) {
			this.view = view;
			this.stream = stream;
			this.controllerID = controllerID;

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

				if (fieldType == BitStructProtocol.FieldType.STRUCT_LIST) {
					this.fields[fieldID] = new StructListField(streamFactory, view, fieldID);
				}
			}
		}

		@Override
		public String toString() {
			return "BitClient.Struct(view=" + view + ", controller=" + controllerID + ")";
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

		public StructReferenceField getStructReferenceField(Class<?> declaringClass, String fieldName) {
			int fieldID = view.protocol.getFieldId(declaringClass, fieldName);
			return (StructReferenceField) fields[fieldID];
		}

		private ChildStructField getChildStructField(Class<?> declaringClass, String fieldName) {
			int fieldID = view.protocol.getFieldId(declaringClass, fieldName);
			return (ChildStructField) fields[fieldID];
		}

		private StructListField getStructListField(Class<?> declaringClass, String fieldName) {
			int fieldID = view.protocol.getFieldId(declaringClass, fieldName);
			return (StructListField) fields[fieldID];
		}

		public Object subscribeChildStruct(
				Class<?> declaringClass, String fieldName, Consumer<Struct> updateChildStruct
		) {
			return getChildStructField(declaringClass, fieldName).subscribe(updateChildStruct);
		}

		public Object subscribeStructList(
				Class<?> declaringClass, String fieldName, Consumer<StructList> updateStructList
		) {
			return getStructListField(declaringClass, fieldName).subscribe(updateStructList);
		}

		public void cancelChildStructSubscription(Class<?> declaringClass, String fieldName, Object subscription) {
			getChildStructField(declaringClass, fieldName).cancelSubscription(subscription);
		}

		public void cancelStructListSubscription(Class<?> declaringClass, String fieldName, Object subscription) {
			getStructListField(declaringClass, fieldName).cancelSubscription(subscription);
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

	public static class StructList {

		private final StructListConnectionView<?> view;
		private final ClientStream stream;
		private final ClientStream.Factory streamFactory;

		private long[] controllerIDs = new long[0];
		private final List<Consumer<long[]>> listeners = new ArrayList<>();

		public StructList(StructListConnectionView<?> view, ClientStream stream, ClientStream.Factory streamFactory) {
			this.view = view;
			this.stream = stream;
			this.streamFactory = streamFactory;
		}

		public void start() {
			stream.start(this::processInput);
		}

		private void refreshListFromServer(BitInputStream fromServer) throws IOException {
			int length = IntegerBitser.decodeUnknownLength(null, "BitClient.StructList", fromServer);
			long[] newControllerIDs = new long[length];
			for (int index = 0; index < length; index++) {
				newControllerIDs[index] = IntegerBitser.decodeVariableIntegerUsingTerminatorBits(
						0L, Long.MAX_VALUE, fromServer
				);
			}

			fromServer.discardCurrentByte();

			synchronized (listeners) {
				this.controllerIDs = newControllerIDs;
				for (var listener : listeners) {
					listener.accept(Arrays.copyOf(controllerIDs, controllerIDs.length));
				}
			}
		}

		private void processInput(BitInputStream fromServer) throws Throwable {
			refreshListFromServer(fromServer);

			//noinspection InfiniteLoopStatement
			while (true) {
				// TODO BITSER Process operation feedback
				refreshListFromServer(fromServer);
			}
		}

		public Struct createConnectionToElement(long controllerID) {
			return new Struct(view.elementsView, streamFactory.createStream(controllerID), streamFactory, controllerID);
		}

		public Object subscribe(Consumer<long[]> callback) {
			synchronized (listeners) {
				callback.accept(Arrays.copyOf(controllerIDs, controllerIDs.length));
				listeners.add(callback);
			}

			return callback;
		}

		public void cancelSubscription(Object subscription) {
			synchronized (listeners) {
				//noinspection SuspiciousMethodCalls
				if (!listeners.remove(subscription)) {
					System.err.println("Attempted to remove missing subscription in BitClient.StructList");
				}
			}
		}

		public void executeSimpleOperation(int operation) {
			stream.send(toServer ->
				IntegerBitser.encodeUniformInteger(
						operation, 0L, max(1L, view.getNumOperations() - 1), toServer
				)
			);
			// TODO BITSER Add support for operations with input & output
		}

		public void close() {
			stream.close();
		}
	}
}
