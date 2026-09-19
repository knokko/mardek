package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.IntegerBitser;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class BitClient {

	public static class ReadOnlyStruct {

		private record Subscription(int fieldID, Consumer<Object> updateValue) {}

		private final StructConnectionView view;
		private final long reference;
		private final Object[] fieldValues;
		private final List<Subscription> subscriptions = new ArrayList<>();

		private boolean didInitialize;

		public ReadOnlyStruct(StructConnectionView view, long reference) {
			this.view = view;
			this.reference = reference;
			this.fieldValues = new Object[view.protocol.getNumFields()];
		}

		public void readFromServer(BitInputStream input) throws Throwable {
			for (int fieldID = 0; fieldID < view.protocol.getNumFields(); fieldID++) {
				if (view.shouldDownloadDuringInitialization(fieldID)) {
					fieldValues[fieldID] = view.protocol.deserializeFlatFieldValue(fieldID, input);
				}
			}
			input.discardCurrentByte();

			synchronized (this) {
				for (var subscription : subscriptions) {
					if (view.shouldDownloadDuringInitialization(subscription.fieldID)) {
						System.out.println("Client: subscribe initial");
						subscription.updateValue.accept(fieldValues[subscription.fieldID]);
					}
				}
				didInitialize = true;
			}

			long maxDownloadableFieldID = view.getNumLateDownloadableFields() - 1L;

			//noinspection InfiniteLoopStatement
			while (true) {
				int downloadableFieldID = (int) IntegerBitser.decodeUniformInteger(
						0L, maxDownloadableFieldID, input
				);
				int fieldID = view.mapDownloadableFieldID(downloadableFieldID);
				Object newValue = view.protocol.deserializeFlatFieldValue(fieldID, input);
				input.discardCurrentByte();
				synchronized (this) {
					fieldValues[fieldID] = newValue;
					for (var subscription : subscriptions) {
						System.out.println("Client: subscribe late");
						if (subscription.fieldID == fieldID) subscription.updateValue.accept(newValue);
					}
				}
			}
		}

		public synchronized <T> void subscribeValue(Class<?> declaringClass, String fieldName, Consumer<T> updateValue) {
			int fieldId = view.protocol.getFieldId(declaringClass, fieldName);

			@SuppressWarnings("unchecked")
			var subscription = new Subscription(fieldId, (Consumer<Object>) updateValue);
			subscriptions.add(subscription);

			if (didInitialize) {
				System.out.println("Client: subscribe: already initialized");
				subscription.updateValue.accept(fieldValues[fieldId]);
			}
		}
	}

	public static class ReadWriteStruct {

		private record Subscription(int fieldID, Consumer<Object> updateValue, boolean considerLocalValue) {}

		private record ChangeStateSubscription(int fieldID, Consumer<ChangeState> updateChangeState) {}

		public enum ChangeState {

			UNINITIALIZED,
			UP_TO_DATE,
			MODIFIED,
			SAVING
		}

		private final StructConnectionView view;
		private final BitOutputStream toServer;
		private final long reference;
		private final Object[] serverValues;
		private final Object[] localValues;
		private final ChangeState[] changeStates;
		private final List<Subscription> subscriptions = new ArrayList<>();
		private final List<ChangeStateSubscription> changeStateSubscriptions = new ArrayList<>();

		public ReadWriteStruct(StructConnectionView view, BitOutputStream toServer, long reference) {
			this.view = view;
			this.toServer = toServer;
			this.reference = reference;
			this.serverValues = new Object[view.protocol.getNumFields()];
			this.localValues = new Object[view.protocol.getNumFields()];
			this.changeStates = new ChangeState[view.protocol.getNumFields()];
			Arrays.fill(changeStates, ChangeState.UNINITIALIZED);
		}

		public void readFromServer(BitInputStream input) throws Throwable {
			for (int fieldID = 0; fieldID < view.protocol.getNumFields(); fieldID++) {
				if (view.shouldDownloadDuringInitialization(fieldID)) {
					serverValues[fieldID] = view.protocol.deserializeFlatFieldValue(fieldID, input);
				}
			}
			input.discardCurrentByte();

			synchronized (this) {
				for (int fieldID = 0; fieldID < view.protocol.getNumFields(); fieldID++) {
					if (view.shouldDownloadDuringInitialization(fieldID)) {
						changeStates[fieldID] = ChangeState.UP_TO_DATE;
					}
				}

				for (var subscription : subscriptions) {
					if (view.shouldDownloadDuringInitialization(subscription.fieldID)) {
						System.out.println("Client: subscribe update during initialization");
						subscription.updateValue.accept(serverValues[subscription.fieldID]);
					}
				}
				for (var subscription: changeStateSubscriptions) {
					if (view.shouldDownloadDuringInitialization(subscription.fieldID)) {
						subscription.updateChangeState.accept(ChangeState.UP_TO_DATE);
					}
				}
			}

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
				synchronized (this) {
					Object oldValue = serverValues[fieldID];
					serverValues[fieldID] = newValue;
					if (changeStates[fieldID] != ChangeState.UP_TO_DATE) {
						if (view.protocol.areFlatValuesEqual(fieldID, oldValue, newValue)) {
							changeStates[fieldID] = ChangeState.UP_TO_DATE;
							localValues[fieldID] = null;

							for (var subscription : changeStateSubscriptions) {
								if (subscription.fieldID == fieldID) {
									subscription.updateChangeState.accept(ChangeState.UP_TO_DATE);
								}
							}
						}
					}

					for (var subscription : subscriptions) {
						if (subscription.fieldID == fieldID && !subscription.considerLocalValue) {
							System.out.println("Client: subscribe for initial");
							subscription.updateValue.accept(newValue);
						}
					}
				}
			}
		}

		public synchronized <T> void subscribeValue(
				Class<?> declaringClass, String fieldName,
				boolean considerLocalValue, Consumer<T> updateValue
		) {
			int fieldId = view.protocol.getFieldId(declaringClass, fieldName);

			@SuppressWarnings("unchecked")
			var subscription = new Subscription(fieldId, (Consumer<Object>) updateValue, considerLocalValue);
			subscriptions.add(subscription);

			System.out.println("Client: subscribe: change state is " + changeStates[fieldId]);
			if (changeStates[fieldId] != ChangeState.UNINITIALIZED) {
				if (considerLocalValue && changeStates[fieldId] != ChangeState.UP_TO_DATE) {
					subscription.updateValue.accept(localValues[fieldId]);
				} else {
					subscription.updateValue.accept(serverValues[fieldId]);
				}
			}
		}

		public synchronized void subscribeChangeState(
				Class<?> declaringClass, String fieldName, Consumer<ChangeState> updateChangeState
		) {
			int fieldId = view.protocol.getFieldId(declaringClass, fieldName);
			var subscription = new ChangeStateSubscription(fieldId, updateChangeState);
			changeStateSubscriptions.add(subscription);

			subscription.updateChangeState.accept(changeStates[fieldId]);
		}

		public synchronized void setValue(Class<?> declaringClass, String fieldName, Object newValue) {
			int fieldId = view.protocol.getFieldId(declaringClass, fieldName);
			Object serverValue = serverValues[fieldId];

			if (view.protocol.areFlatValuesEqual(fieldId, serverValue, newValue)) {
				changeStates[fieldId] = ChangeState.UP_TO_DATE;
				localValues[fieldId] = null;
				System.out.println("Client: Marked as up-to-date");
			} else {
				changeStates[fieldId] = ChangeState.MODIFIED;
				localValues[fieldId] = newValue;
				System.out.println("Client: Marked as modified");
			}

			for (var subscription : subscriptions) {
				if (subscription.fieldID == fieldId && subscription.considerLocalValue) {
					System.out.println("Client: setValue: update value");
					subscription.updateValue.accept(newValue);
				}
			}

			for (var subscription : changeStateSubscriptions) {
				if (subscription.fieldID == fieldId) {
					subscription.updateChangeState.accept(changeStates[fieldId]);
				}
			}
		}

		public synchronized void saveValue(Class<?> declaringClass, String fieldName) {
			int fieldID = view.protocol.getFieldId(declaringClass, fieldName);
			if (changeStates[fieldID] != ChangeState.MODIFIED && changeStates[fieldID] != ChangeState.SAVING) {
				return;
			}

			System.out.println("Client: save value " + localValues[fieldID] + " for field " + fieldID);
			System.out.println("server values are " + Arrays.toString(serverValues) + " and local are " + Arrays.toString(localValues) + " and changes are " + Arrays.toString(changeStates));
			int uploadableFieldID = view.mapToUploadableFieldID(fieldID);
			int maxUploadableFieldID = view.getNumUploadableFields() - 1;
			try {
				IntegerBitser.encodeUniformInteger(uploadableFieldID, 0L, maxUploadableFieldID, toServer);
				view.protocol.serializeFlatFieldValue(fieldID, toServer, localValues[fieldID]);
				toServer.flush();
			} catch (Throwable failed) {
				throw new RuntimeException(failed);
			}
			changeStates[fieldID] = ChangeState.SAVING;

			for (var subscription : changeStateSubscriptions) {
				if (subscription.fieldID == fieldID) {
					subscription.updateChangeState.accept(ChangeState.SAVING);
				}
			}
		}
	}
}
