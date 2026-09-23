package com.github.knokko.bitser.connection;

public class StructConnectionView {

	final BitStructProtocol protocol;
	private final boolean[] canDownloadFlat;
	private final boolean[] canUploadFlat;
	private final boolean[] isConstant;
	private final boolean[] flatStructs;
	private final StructConnectionView[] childStructs;

	private boolean finishedRegistration;
	private boolean hasDownloadableFields;
	private boolean hasUploadableFields;

	public StructConnectionView(BitStructProtocol protocol) {
		this.protocol = protocol;
		this.canDownloadFlat = new boolean[protocol.getNumFields()];
		this.canUploadFlat = new boolean[protocol.getNumFields()];
		this.isConstant = new boolean[protocol.getNumFields()];
		this.flatStructs = new boolean[protocol.getNumFields()];
		this.childStructs = new StructConnectionView[protocol.getNumFields()];
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

	public void markChildStructField(
			Class<?> declaringClass, String fieldName,
			StructConnectionView childView, boolean constant
	) {
		int fieldID = getUnclaimedStructField(declaringClass, fieldName);
		childStructs[fieldID] = childView;
		canDownloadFlat[fieldID] = true;
		isConstant[fieldID] = constant;
	}

	public void markStructFieldAsFlat(Class<?> declaringClass, String fieldName, boolean canRead, boolean canWrite) {
		int fieldID = getUnclaimedStructField(declaringClass, fieldName);
		flatStructs[fieldID] = true;
		canDownloadFlat[fieldID] = canRead;
		canUploadFlat[fieldID] = canWrite;
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
		return canUploadFlat[fieldID];
	}

	StructConnectionView getChildStructViewOrNull(int fieldID) {
		assertRegistrationIsClosed();
		return childStructs[fieldID];
	}

	public StructConnectionView getChildStructView(Class<?> declaringClass, String fieldName) {
		assertRegistrationIsClosed();

		var childView = childStructs[protocol.getFieldId(declaringClass, fieldName)];
		if (childView == null) {
			throw new IllegalArgumentException("Field " + fieldName + " has no registered child view");
		}
		return childView;
	}

	boolean hasAtLeastOneDownloadableField() {
		return hasDownloadableFields;
	}

	boolean hasAtLeastOneUploadableField() {
		return hasUploadableFields;
	}
}
