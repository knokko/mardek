package com.github.knokko.bitser.connection;

public abstract class BitConnectionView {

	public final int localId;

	public BitConnectionView(int localId) {
		this.localId = localId;
	}

	public abstract BitServerController initializeController(BitServerContext context, Object targetObject);
}
