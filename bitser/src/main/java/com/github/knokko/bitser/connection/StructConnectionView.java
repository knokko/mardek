package com.github.knokko.bitser.connection;

import java.util.Arrays;

public class StructConnectionView {

	final BitStructProtocol protocol;
	private final boolean[] canDownloadFlat;
	private final boolean[] canUploadFlat;
	private final boolean[] isConstant;
	private final boolean[] flatStructs;
	private final StructConnectionView[] childStructs;

	private int[] downloadableFieldsMapping;
	private final int[] reverseDownloadableFieldMapping;
	private int[] uploadableFieldsMapping;

	public StructConnectionView(BitStructProtocol protocol) {
		this.protocol = protocol;
		this.canDownloadFlat = new boolean[protocol.getNumFields()];
		this.reverseDownloadableFieldMapping = new int[protocol.getNumFields()];
		Arrays.fill(reverseDownloadableFieldMapping, -1);
		this.canUploadFlat = new boolean[protocol.getNumFields()];
		this.isConstant = new boolean[protocol.getNumFields()];
		this.flatStructs = new boolean[protocol.getNumFields()];
		this.childStructs = new StructConnectionView[protocol.getNumFields()];
	}

	private void assertRegistrationIsOpen() {
		if (downloadableFieldsMapping != null) throw new IllegalStateException("Registration is already closed");
	}

	public void markStructFieldAsFlat(Class<?> declaringClass, String fieldName, boolean canRead, boolean canWrite) {
		assertRegistrationIsOpen();

		int fieldID = protocol.getFieldId(declaringClass, fieldName);
		if (flatStructs[fieldID]) throw new IllegalStateException("Field " + fieldName + " is already flat");
		if (childStructs[fieldID] != null) {
			throw new IllegalStateException("Field " + fieldName + " is already a child");
		}
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

		int numReadableFields = 0;
		int numWritableFields = 0;
		for (int fieldID = 0; fieldID < protocol.getNumFields(); fieldID++) {
			if (canDownloadFlat[fieldID] && !isConstant[fieldID]) numReadableFields += 1;
			if (canUploadFlat[fieldID]) {
				if (isConstant[fieldID]) throw new IllegalStateException("Writable fields must NOT be constant");
				numWritableFields += 1;
			}
		}

		downloadableFieldsMapping = new int[numReadableFields];
		uploadableFieldsMapping = new int[numWritableFields];

		int downloadableFieldID = 0;
		int writableFieldID = 0;
		for (int fieldID = 0; fieldID < protocol.getNumFields(); fieldID++) {
			if (canDownloadFlat[fieldID] && !isConstant[fieldID]) {
				reverseDownloadableFieldMapping[fieldID] = downloadableFieldID;
				downloadableFieldsMapping[downloadableFieldID] = fieldID;
				downloadableFieldID += 1;
			}
			if (canUploadFlat[fieldID]) {
				uploadableFieldsMapping[writableFieldID++] = fieldID;
			}
		}
	}

	private void assertRegistrationIsClosed() {
		if (downloadableFieldsMapping == null) throw new IllegalStateException("Registration is still open");
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

	int getNumLateDownloadableFields() {
		return downloadableFieldsMapping.length;
	}

	int mapDownloadableFieldID(int downloadableFieldID) {
		return downloadableFieldsMapping[downloadableFieldID];
	}

	int mapToDownloadableFieldID(int fieldID) {
		return reverseDownloadableFieldMapping[fieldID];
	}

	int getNumUploadableFields() {
		return uploadableFieldsMapping.length;
	}

	int mapUploadableFieldID(int uploadableFieldID) {
		return uploadableFieldsMapping[uploadableFieldID];
	}
}
