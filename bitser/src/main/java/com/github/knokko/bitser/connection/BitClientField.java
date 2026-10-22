package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;

abstract class BitClientField {

	final BitStructConnectionView view;
	final int fieldID;

	BitClientField(BitStructConnectionView view, int fieldID) {
		this.view = view;
		this.fieldID = fieldID;
	}

	abstract void readFromServerInitial(BitInputStream input) throws Throwable;

	abstract void postReadFromServerInitial();

	abstract void setFromServer(Object newServerValue);

	abstract void save(BitOutputStream output) throws Throwable;
}
