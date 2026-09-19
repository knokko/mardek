package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.IntegerBitser;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;

import java.util.ArrayList;
import java.util.List;
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
		final BitOutputStream toServer;

		AbstractField(StructConnectionView view, int fieldID, BitOutputStream toServer) {
			this.view = view;
			this.fieldID = fieldID;
			this.toServer = toServer;
		}

		abstract void readFromServerInitial(BitInputStream input) throws Throwable;

		abstract void postReadFromServerInitial();

		abstract void setFromServer(Object newServerValue);
	}

	public static class SimpleFlatField<T> extends AbstractField {

		private T serverValue;
		private T localValue;
		private ChangeState changeState;
		private final List<Consumer<T>> serverListeners = new ArrayList<>();
		private final List<Consumer<T>> localListeners = new ArrayList<>();
		private final List<Consumer<ChangeState>> changeStateListeners = new ArrayList<>();

		public SimpleFlatField(StructConnectionView view, int fieldID, BitOutputStream toServer) {
			super(view, fieldID, toServer);
			this.changeState = ChangeState.UNINITIALIZED;
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

		public synchronized void subscribe(boolean considerLocalValue, Consumer<T> updateValue) {
			if (considerLocalValue) localListeners.add(updateValue);
			else serverListeners.add(updateValue);

			if (changeState != ChangeState.UNINITIALIZED) {
				if (considerLocalValue && changeState != ChangeState.UP_TO_DATE) {
					updateValue.accept(localValue);
				} else {
					updateValue.accept(serverValue);
				}
			}
		}

		public synchronized void subscribeChangeState(Consumer<ChangeState> updateChangeState) {
			changeStateListeners.add(updateChangeState);
			if (changeState != ChangeState.UNINITIALIZED) updateChangeState.accept(changeState);
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

		public synchronized void save() {
			if (changeState != ChangeState.MODIFIED && changeState != ChangeState.SAVING) return;

			int uploadableFieldID = view.mapToUploadableFieldID(fieldID);
			int maxUploadableFieldID = view.getNumUploadableFields() - 1;

			synchronized (toServer) {
				try {
					IntegerBitser.encodeUniformInteger(uploadableFieldID, 0L, maxUploadableFieldID, toServer);
					view.protocol.serializeFlatFieldValue(fieldID, toServer, localValue);
					toServer.flush();
				} catch (Throwable failed) {
					throw new RuntimeException(failed);
				}
			}

			changeState = ChangeState.SAVING;

			for (var listener : changeStateListeners) listener.accept(ChangeState.SAVING);
		}
	}

	public static class ReadWriteStruct {

		private final StructConnectionView view;
		private final BitOutputStream toServer;
		private final Runnable closeStream;
		private final long reference;
		private final AbstractField[] fields;

		public ReadWriteStruct(StructConnectionView view, BitOutputStream toServer, Runnable closeStream, long reference) {
			this.view = view;
			this.toServer = toServer;
			this.closeStream = closeStream;
			this.reference = reference;
			this.fields = new AbstractField[view.protocol.getNumFields()];
			for (int fieldID = 0; fieldID < fields.length; fieldID++) {
				if (view.protocol.getFieldType(fieldID) == BitStructProtocol.FieldType.SIMPLE) {
					this.fields[fieldID] = new SimpleFlatField<>(view, fieldID, toServer);
				}
			}
		}

		public void readFromServer(BitInputStream input) throws Throwable {
			try {
				for (var field : fields) field.readFromServerInitial(input);
				input.discardCurrentByte();

				for (var field : fields) field.postReadFromServerInitial();

				long maxDownloadableFieldID = view.getNumLateDownloadableFields() - 1L;

				//noinspection InfiniteLoopStatement
				while (true) {
					int downloadableFieldID = (int) IntegerBitser.decodeUniformInteger(
							0L, maxDownloadableFieldID, input
					);
					int fieldID = view.mapDownloadableFieldID(downloadableFieldID);
					Object newValue = view.protocol.deserializeFlatFieldValue(fieldID, input);
					System.out.println("Client: receive " + newValue + " for field " + fieldID);
					input.discardCurrentByte();
					fields[fieldID].setFromServer(newValue);
				}
			} finally {
				closeStream.run();
			}
		}

		public <T> SimpleFlatField<T> getSimpleField(Class<?> declaringClass, String fieldName) {
			int fieldID = view.protocol.getFieldId(declaringClass, fieldName);
			//noinspection unchecked
			return (SimpleFlatField<T>) fields[fieldID];
		}

		public void close() {
			closeStream.run();
		}
	}
}
