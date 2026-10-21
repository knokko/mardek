package com.github.knokko.bitser.connection;

public abstract class BitConnectionView {

	public abstract BitServerController initializeController(BitServerContext context, Object targetObject);
}
