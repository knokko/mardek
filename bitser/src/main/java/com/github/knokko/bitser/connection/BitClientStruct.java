package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BitClientStruct {

	private final BitStructConnectionView view;
	private final BitClientStream stream;
	public final long controllerID;

	private final BitClientField[] fields;
	private final List<Consumer<Boolean>> canSaveListeners = new ArrayList<>();
	private final boolean[] canSaveArray;
	private boolean lastCanSave;

	public BitClientStruct(
			BitStructConnectionView view, BitClientStream stream,
			BitClientStream.Factory streamFactory, long controllerID
	) {
		this.view = view;
		this.stream = stream;
		this.controllerID = controllerID;

		this.fields = new BitClientField[view.protocol.getNumFields()];
		this.canSaveArray = new boolean[fields.length];
		for (int fieldID = 0; fieldID < fields.length; fieldID++) {
			var fieldType = view.protocol.getFieldType(fieldID);
			if (fieldType == BitStructProtocol.FieldType.SIMPLE) {
				var flatField = new BitClientSimpleFlatField<>(view, fieldID);
				this.fields[fieldID] = flatField;
				flatField.subscribeChangeState(newChangeState -> this.updateCanSave(
						flatField.fieldID, newChangeState == BitClientChangeState.MODIFIED
				));
			}

			if (fieldType == BitStructProtocol.FieldType.STRUCT) {
				this.fields[fieldID] = new BitClientChildStructField(streamFactory, view, fieldID);
			}

			if (fieldType == BitStructProtocol.FieldType.STRUCT_LIST) {
				this.fields[fieldID] = new BitClientStructListField(streamFactory, view, fieldID);
			}

			if (fieldType == BitStructProtocol.FieldType.REFERENCE) {
				this.fields[fieldID] = new BitClientStructReferenceField(streamFactory, view, fieldID);
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

	public <T> BitClientSimpleFlatField<T> getSimpleField(Class<?> declaringClass, String fieldName) {
		int fieldID = view.protocol.getFieldId(declaringClass, fieldName);
		//noinspection unchecked
		return (BitClientSimpleFlatField<T>) fields[fieldID];
	}

	public BitClientStructReferenceField getStructReferenceField(Class<?> declaringClass, String fieldName) {
		int fieldID = view.protocol.getFieldId(declaringClass, fieldName);
		return (BitClientStructReferenceField) fields[fieldID];
	}

	private BitClientChildStructField getChildStructField(Class<?> declaringClass, String fieldName) {
		int fieldID = view.protocol.getFieldId(declaringClass, fieldName);
		return (BitClientChildStructField) fields[fieldID];
	}

	private BitClientStructListField getStructListField(Class<?> declaringClass, String fieldName) {
		int fieldID = view.protocol.getFieldId(declaringClass, fieldName);
		return (BitClientStructListField) fields[fieldID];
	}

	public Object subscribeChildStruct(
			Class<?> declaringClass, String fieldName, int viewIndex, Consumer<BitClientStruct> updateChildStruct
	) {
		return getChildStructField(declaringClass, fieldName).subscribe(updateChildStruct, viewIndex);
	}

	public Object subscribeStructList(
			Class<?> declaringClass, String fieldName, int viewIndex, Consumer<BitClientStructList> updateStructList
	) {
		return getStructListField(declaringClass, fieldName).subscribe(updateStructList, viewIndex);
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
