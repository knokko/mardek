package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.IntegerBitser;
import com.github.knokko.bitser.io.BitInputStream;

import java.util.ArrayList;
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

			if (didInitialize) subscription.updateValue.accept(fieldValues[fieldId]);
		}
	}

	public static class ReadWriteStruct {

		private record Subscription(int fieldID, Consumer<Object> updateValue) {}

		private final StructConnectionView view;
		private final long reference;
		private final Object[] serverValues;
		private final Object[] localValues;
		private final List<ReadOnlyStruct.Subscription> subscriptions = new ArrayList<>();

		private boolean didInitialize;

		public ReadWriteStruct(StructConnectionView view, long reference) {
			this.view = view;
			this.reference = reference;
			this.serverValues = new Object[view.protocol.getNumFields()];
			this.localValues = new Object[view.protocol.getNumFields()];
		}

		public void readFromServer(BitInputStream input) throws Throwable {
			for (int fieldID = 0; fieldID < view.protocol.getNumFields(); fieldID++) {
				if (view.shouldDownloadDuringInitialization(fieldID)) {
					serverValues[fieldID] = view.protocol.deserializeFlatFieldValue(fieldID, input);
				}
			}
			input.discardCurrentByte();

			synchronized (this) {
				for (var subscription : subscriptions) {
					if (view.shouldDownloadDuringInitialization(subscription.fieldID)) {
						subscription.updateValue.accept(serverValues[subscription.fieldID]);
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
						if (subscription.fieldID == fieldID) subscription.updateValue.accept(newValue);
					}
				}
			}
		}
	}
}
