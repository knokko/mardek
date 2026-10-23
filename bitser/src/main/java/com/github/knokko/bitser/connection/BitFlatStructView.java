package com.github.knokko.bitser.connection;

public class BitFlatStructView {

	public final BitStructProtocol protocol;
	private final boolean[] simpleFields;
	private final BitFlatStructView[] structFields;

	private boolean finishedRegistration;

	public BitFlatStructView(BitStructProtocol protocol) {
		this.protocol = protocol;
		this.simpleFields = new boolean[protocol.getNumFields()];
		this.structFields = new BitFlatStructView[protocol.getNumFields()];
	}

	@Override
	public String toString() {
		return "BitFlatStructView(" + protocol + ")";
	}

	private void assertRegistrationIsOpen() {
		if (finishedRegistration) throw new IllegalStateException("Registration is already closed");
	}

	public void markChildStructField(Class<?> declaringClass, String fieldName, BitFlatStructView childView) {
		assertRegistrationIsOpen();
		int fieldID = protocol.getFieldId(declaringClass, fieldName);
		var fieldType = protocol.getFieldType(fieldID);
		if (fieldType != BitStructProtocol.FieldType.STRUCT) {
			throw new IllegalArgumentException(fieldName + " of " + declaringClass + " is not a struct field");
		}
		if (structFields[fieldID] != null) {
			throw new IllegalStateException("Field " + fieldName + " is already a child");
		}
		structFields[fieldID] = childView;
	}

	public void markAllSimpleFields() {
		assertRegistrationIsOpen();

		for (int fieldID = 0; fieldID < protocol.getNumFields(); fieldID++) {
			if (protocol.getFieldType(fieldID) == BitStructProtocol.FieldType.SIMPLE) {
				simpleFields[fieldID] = true;
			}
		}
	}

	public void markSimpleField(Class<?> declaringClass, String fieldName) {
		int fieldID = protocol.getFieldId(declaringClass, fieldName);
		var fieldType = protocol.getFieldType(fieldID);
		if (fieldType != BitStructProtocol.FieldType.SIMPLE) {
			throw new IllegalArgumentException(fieldName + " of " + declaringClass + " is not simple");
		}

		simpleFields[fieldID] = true;
	}

	public void finishRegistration() {
		assertRegistrationIsOpen();
		finishedRegistration = true;
	}

	private void assertRegistrationIsClosed() {
		if (!finishedRegistration) throw new IllegalStateException("Registration is still open");
	}

	public boolean shouldDownloadSimpleField(int fieldID) {
		assertRegistrationIsClosed();
		return simpleFields[fieldID];
	}

	public BitFlatStructView getChildStructOrNull(int fieldID) {
		assertRegistrationIsClosed();
		return structFields[fieldID];
	}
}
