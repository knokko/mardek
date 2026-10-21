package com.github.knokko.bitser.connection;

import java.util.List;

public class BitStructConnectionView extends BitConnectionView {

	public final BitStructProtocol protocol;
	private final boolean[] canDownloadFlat;
	private final boolean[] canUploadFlat;
	private final boolean[] isConstant;
	private final boolean[] flatStructs;
	private final BitStructConnectionView[][] childStructs;
	private final BitStructListConnectionView<?>[][] structLists;
	private final BitStructConnectionView[][] referenceStructs;

	private boolean finishedRegistration;
	private boolean hasDownloadableFields;
	private boolean hasUploadableFields;

	public BitStructConnectionView(BitStructProtocol protocol) {
		this.protocol = protocol;

		int numFields = protocol.getNumFields();
		this.canDownloadFlat = new boolean[numFields];
		this.canUploadFlat = new boolean[numFields];
		this.isConstant = new boolean[numFields];
		this.flatStructs = new boolean[numFields];
		this.childStructs = new BitStructConnectionView[numFields][];
		this.structLists = new BitStructListConnectionView[numFields][];
		this.referenceStructs = new BitStructConnectionView[numFields][];
	}

	@Override
	public String toString() {
		return "StructConnectionView(" + protocol + ")";
	}

	private void assertRegistrationIsOpen() {
		if (finishedRegistration) throw new IllegalStateException("Registration is already closed");
	}

	private int getUnclaimedStructField(Class<?> declaringClass, String fieldName) {
		assertRegistrationIsOpen();

		int fieldID = protocol.getFieldId(declaringClass, fieldName);
		var fieldType = protocol.getFieldType(fieldID);
		if (fieldType != BitStructProtocol.FieldType.STRUCT) {
			throw new IllegalArgumentException(fieldName + " of " + declaringClass + " is not a struct field");
		}
		if (flatStructs[fieldID]) throw new IllegalStateException("Field " + fieldName + " is already flat");
		if (childStructs[fieldID] != null) {
			throw new IllegalStateException("Field " + fieldName + " is already a child");
		}
		return fieldID;
	}

	private int getUnclaimedStructReferenceField(Class<?> declaringClass, String fieldName) {
		assertRegistrationIsOpen();

		int fieldID = protocol.getFieldId(declaringClass, fieldName);
		var fieldType = protocol.getFieldType(fieldID);
		if (fieldType != BitStructProtocol.FieldType.REFERENCE) {
			throw new IllegalArgumentException(fieldName + " of " + declaringClass + " is not a reference field");
		}
		if (referenceStructs[fieldID] != null) {
			throw new IllegalStateException("Field " + fieldName + " is already a reference");
		}
		return fieldID;
	}

	private int getUnclaimedStructListField(Class<?> declaringClass, String fieldName) {
		assertRegistrationIsOpen();

		int fieldID = protocol.getFieldId(declaringClass, fieldName);
		var fieldType = protocol.getFieldType(fieldID);
		if (fieldType != BitStructProtocol.FieldType.STRUCT_LIST) {
			throw new IllegalArgumentException(fieldName + " of " + declaringClass + " is not a struct list field");
		}
		if (structLists[fieldID] != null) {
			throw new IllegalStateException("Field " + fieldName + " is already a struct list");
		}
		return fieldID;
	}

	public void markChildStructField(
			Class<?> declaringClass, String fieldName,
			List<BitStructConnectionView> childViews, boolean constant
	) {
		int fieldID = getUnclaimedStructField(declaringClass, fieldName);
		childStructs[fieldID] = childViews.toArray(new BitStructConnectionView[0]);
		canDownloadFlat[fieldID] = true;
		isConstant[fieldID] = constant;
	}

	public void markStructReferenceField(
			Class<?> declaringClass, String fieldName,
			List<BitStructConnectionView> referencedObjectViews,
			boolean canRead, boolean canReassign
	) {
		int fieldID = getUnclaimedStructReferenceField(declaringClass, fieldName);
		referenceStructs[fieldID] = referencedObjectViews.toArray(new BitStructConnectionView[0]);
		canDownloadFlat[fieldID] = canRead;
		canUploadFlat[fieldID] = canReassign;
	}

	public void markStructFieldAsFlat(Class<?> declaringClass, String fieldName, boolean canRead, boolean canWrite) {
		int fieldID = getUnclaimedStructField(declaringClass, fieldName);
		flatStructs[fieldID] = true;
		canDownloadFlat[fieldID] = canRead;
		canUploadFlat[fieldID] = canWrite;
	}

	public void markStructListField(
			Class<?> declaringClass, String fieldName,
			List<BitStructListConnectionView<?>> listViews, boolean constant
	) {
		int fieldID = getUnclaimedStructListField(declaringClass, fieldName);
		structLists[fieldID] = listViews.toArray(new BitStructListConnectionView<?>[0]);
		canDownloadFlat[fieldID] = true;
		isConstant[fieldID] = constant;
	}

	public void markAllSimpleFields(boolean canRead, boolean canWrite) {
		assertRegistrationIsOpen();

		for (int fieldID = 0; fieldID < protocol.getNumFields(); fieldID++) {
			if (protocol.getFieldType(fieldID) == BitStructProtocol.FieldType.SIMPLE) {
				canDownloadFlat[fieldID] = canRead;
				canUploadFlat[fieldID] = canWrite;
			}
		}
	}

	public void markSimpleField(Class<?> declaringClass, String fieldName, boolean canRead, boolean canWrite) {
		int fieldID = protocol.getFieldId(declaringClass, fieldName);
		var fieldType = protocol.getFieldType(fieldID);
		if (fieldType != BitStructProtocol.FieldType.SIMPLE) {
			throw new IllegalArgumentException(fieldName + " of " + declaringClass + " is not simple");
		}

		canDownloadFlat[fieldID] = canRead;
		canUploadFlat[fieldID] = canWrite;
	}

	public void finishRegistration() {
		assertRegistrationIsOpen();

		finishedRegistration = true;

		for (int fieldID = 0; fieldID < protocol.getNumFields(); fieldID++) {
			if (canDownloadFlat[fieldID] && !isConstant[fieldID]) hasDownloadableFields = true;
			if (canUploadFlat[fieldID]) {
				if (isConstant[fieldID]) throw new IllegalStateException("Writable fields must NOT be constant");
				hasUploadableFields = true;
			}
		}
	}

	private void assertRegistrationIsClosed() {
		if (!finishedRegistration) throw new IllegalStateException("Registration is still open");
	}

	boolean isStructFlat(int fieldID) {
		assertRegistrationIsClosed();
		return flatStructs[fieldID];
	}

	boolean shouldDownloadDuringInitialization(int fieldID) {
		assertRegistrationIsClosed();
		return canDownloadFlat[fieldID];
	}

	boolean canDownloadFlat(int fieldID) {
		assertRegistrationIsClosed();
		return canDownloadFlat[fieldID] && !isConstant[fieldID];
	}

	boolean canUploadFlat(int fieldID) {
		assertRegistrationIsClosed();
		if (referenceStructs[fieldID] != null) {
			throw new UnsupportedOperationException("This is a reference field; use canUploadReference instead");
		}
		return canUploadFlat[fieldID];
	}

	boolean canUploadReference(int fieldID) {
		assertRegistrationIsClosed();
		if (referenceStructs[fieldID] == null) {
			throw new UnsupportedOperationException("This is not a reference field");
		}
		return canUploadFlat[fieldID];
	}

	List<BitStructConnectionView> getChildStructViewsOrNull(int fieldID) {
		assertRegistrationIsClosed();
		if (childStructs[fieldID] == null) return null;
		return List.of(childStructs[fieldID]);
	}

	List<BitStructListConnectionView<?>> getStructListViewsOrNull(int fieldID) {
		assertRegistrationIsClosed();
		if (structLists[fieldID] == null) return null;
		return List.of(structLists[fieldID]);
	}

	List<BitStructConnectionView> getStructReferenceViewsOrNull(int fieldID) {
		assertRegistrationIsClosed();
		if (referenceStructs[fieldID] == null) return null;
		return List.of(referenceStructs[fieldID]);
	}

	boolean hasAtLeastOneDownloadableField() {
		return hasDownloadableFields;
	}

	boolean hasAtLeastOneUploadableField() {
		return hasUploadableFields;
	}

	@Override
	public BitServerController initializeController(BitServerContext context, Object targetObject) {
		return new BitServerStructController<>(context, targetObject, protocol);
	}
}
